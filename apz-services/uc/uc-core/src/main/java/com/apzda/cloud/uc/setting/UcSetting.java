package com.apzda.cloud.uc.setting;

import cn.hutool.core.collection.CollectionUtil;
import com.apzda.cloud.config.Setting;
import com.apzda.cloud.gsvc.utils.I18nUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UcSetting implements Setting, Serializable {

    @Serial
    private static final long serialVersionUID = -474134184414429761L;

    /**
     * 小于0表示不开启验证码; 等于0表示强制开启验证码; 大于0表示密码错误多少次后开启验证码
     */
    private int thresholdForCaptcha = 0;

    /**
     * 启用多重因素认证
     */
    private boolean mfaEnabled = false;

    /**
     * 密码过期时间，0表示永不过期。单位为秒
     */
    private int passwordExpired = 15552000;

    /**
     * 认证领域提供器，默认使用数据库.
     */
    private String provider = "db";

    /**
     * 创建账户时，如果没有指定角色，则使用默认的角色列表
     */
    private List<String> defaultRoles = new ArrayList<>();

    /**
     * 注册时手机号是否必须
     */
    private boolean phoneNeed;

    /**
     * 注册时邮箱是否必须
     */
    private boolean emailNeed;

    /**
     * 推荐码最大使用次数，0表示可以无限使用
     */
    private int recCodeMaxReferenced = 0;

    /**
     * 开户时必须有推荐码
     */
    private boolean recCodeNeed;

    /**
     * 自动绑定
     */
    private boolean autoBind;

    private LdapConfig ldapConfig = new LdapConfig();

    @Override
    public String name() {
        return I18nUtils.t("uc.setting.name", "User Center Setting");
    }

    public List<String> getDefaultRoles() {
        return CollectionUtil.isEmpty(defaultRoles) ? List.of("user") : defaultRoles;
    }

    public static class LdapConfig implements Serializable {

    }

}
