package com.apzda.cloud.uc.security.authentication;

import cn.hutool.crypto.digest.DigestUtil;
import com.apzda.cloud.audit.aop.AuditContextHolder;
import com.apzda.cloud.audit.aop.AuditLog;
import com.apzda.cloud.captcha.helper.CaptchaHelper;
import com.apzda.cloud.config.exception.SettingUnavailableException;
import com.apzda.cloud.config.service.SettingService;
import com.apzda.cloud.gsvc.error.ServiceError;
import com.apzda.cloud.gsvc.infra.TempStorage;
import com.apzda.cloud.gsvc.security.exception.AuthenticationError;
import com.apzda.cloud.gsvc.security.token.JwtAuthenticationToken;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMeta;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMetaRepository;
import com.apzda.cloud.uc.UserMetas;
import com.apzda.cloud.uc.config.UCenterConfigProperties;
import com.apzda.cloud.uc.domain.entity.Oauth;
import com.apzda.cloud.uc.domain.repository.TenantRepository;
import com.apzda.cloud.uc.domain.service.UserManager;
import com.apzda.cloud.uc.realm.AuthenticatingRealm;
import com.apzda.cloud.uc.security.AuthTempData;
import com.apzda.cloud.uc.security.error.NeedCaptchaError;
import com.apzda.cloud.uc.setting.UcSetting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

/**
 * @author fengz
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultAuthenticationProvider implements AuthenticationProvider, ApplicationContextAware {

    private final UserManager userManager;

    private final UserDetailsMetaRepository userDetailsMetaRepository;

    private final CaptchaHelper captchaHelper;

    private final SettingService settingService;

    private final TempStorage tempStorage;

    private TransactionTemplate transactionTemplate;

    private ApplicationContext applicationContext;

    private UCenterConfigProperties properties;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.properties = this.applicationContext.getBean(UCenterConfigProperties.class);
        this.transactionTemplate = this.applicationContext.getBean(TransactionTemplate.class);
    }

    @Override
    @AuditLog(activity = "login", template = "{} authenticated successfully", errorTpl = "{} authenticated failure: {}",
            args = { "#authentication.principal", "#throwExp?.message" })
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        log.trace("Start Authenticate Authentication: {}", authentication);

        val principal = authentication.getPrincipal();
        if (Objects.isNull(principal) || StringUtils.isBlank((String) principal)) {
            throw new UsernameNotFoundException("username is blank");
        }
        val username = (String) principal;
        val context = AuditContextHolder.getContext();
        context.setUsername(username);

        UcSetting ucSetting;
        try {
            ucSetting = settingService.load(UcSetting.class);
        }
        catch (SettingUnavailableException e) {
            log.error("无法加载用户中心配置: {}", e.getMessage());
            throw new AuthenticationError(ServiceError.SERVICE_UNAVAILABLE);
        }

        // 加载登录临时数据
        val data = loadLoginTmpData(username);
        // 验证码
        validateCaptcha(data, ucSetting);
        // 获取用户认证领域
        val provider = StringUtils.defaultIfBlank(ucSetting.getProvider(), "db");
        val realm = applicationContext.getBean(provider + "AuthenticatingRealm", AuthenticatingRealm.class);
        log.debug("开始用户/密码认证，认证域({}): {}", provider, username);
        Exception exception;
        try {
            val realmUser = realm.authenticate(authentication);
            if (realmUser != null) {
                val userDetailsMeta = userDetailsMetaRepository.create(realmUser);
                val authed = JwtAuthenticationToken.authenticated(userDetailsMeta, realmUser.getPassword());
                userDetailsMeta.remove(UserDetailsMeta.AUTHORITY_META_KEY);
                userDetailsMeta.set(UserDetailsMeta.LOGIN_TIME_META_KEY, authed, 0L);
                userDetailsMeta.remove(UserMetas.RUNNING_AS, authed);
                userDetailsMeta.setOpenId(realmUser.getOpenId());
                userDetailsMeta.setUnionId(realmUser.getUnionId());
                userDetailsMeta.setProvider(realm.getName());

                val domain = realmUser.getDomain();
                if (StringUtils.isNotBlank(domain)) {
                    val tenantRepository = applicationContext.getBean(TenantRepository.class);
                    val tenant = tenantRepository.getByDomain(domain);
                    tenant.ifPresent(value -> userDetailsMeta.set(UserMetas.CURRENT_TENANT_ID, authed, value.getId()));
                }
                else {
                    transactionTemplate.execute((status) -> {
                        val user = userManager.getUserByUsernameAndProvider(username, provider);
                        val meta = user.getMeta(UserMetas.CURRENT_TENANT_ID);
                        meta.ifPresent(
                                value -> userDetailsMeta.set(UserMetas.CURRENT_TENANT_ID, authed, value.getValue()));
                        return true;
                    });
                }

                data.setErrorCnt(0);
                data.setNeedCaptcha(false);
                saveLoginTmpData(username, data);

                // this is a transit Oauth Object!!!
                val oauth = new Oauth();
                oauth.setOpenId(realmUser.getOpenId());
                oauth.setUnionId(realmUser.getUnionId());
                oauth.setProvider(realm.getName());
                userManager.onAuthenticated(authed, oauth);

                return authed;
            }
            else {
                throw new AuthenticationError(ServiceError.USER_PWD_INCORRECT);
            }
        }
        catch (AuthenticationException e) {
            exception = e;
        }

        if (!properties.isCaptchaDisabled() && ucSetting.getThresholdForCaptcha() >= 0) {
            data.setErrorCnt(data.getErrorCnt() + 1);
            if (data.getErrorCnt() >= ucSetting.getThresholdForCaptcha()) {
                data.setNeedCaptcha(true);
            }
            saveLoginTmpData(username, data);
            if (data.isNeedCaptcha()) {
                throw new AuthenticationError(new NeedCaptchaError(exception.getMessage()));
            }
        }
        throw new AuthenticationError(ServiceError.USER_PWD_INCORRECT);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private void validateCaptcha(@NonNull AuthTempData data, @NonNull UcSetting ucSetting) {
        if (properties.isCaptchaDisabled()) {
            return;
        }
        val threshold = ucSetting.getThresholdForCaptcha();
        if (threshold == -1 || threshold > 0 && !data.isNeedCaptcha()) {
            return;
        }

        captchaHelper.validate();
    }

    @NonNull
    private AuthTempData loadLoginTmpData(@NonNull String username) {
        val id = "auth.tmp." + DigestUtil.md5Hex(username);
        val data = tempStorage.load(id, AuthTempData.class);
        return data.orElse(new AuthTempData());
    }

    private void saveLoginTmpData(@NonNull String username, @NonNull AuthTempData data) {
        val id = "auth.tmp." + DigestUtil.md5Hex(username);
        try {
            tempStorage.save(id, data);
        }
        catch (Exception e) {
            log.warn("Cannot save Temp Data for {} - {}", username, data);
        }
    }

}
