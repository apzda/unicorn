/*
 * Copyright (C) 2023-2024 Fengz Ning (windywany@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apzda.cloud.uc.domain.service;

import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.gsvc.security.exception.TokenException;
import com.apzda.cloud.gsvc.security.token.JwtAuthenticationToken;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMeta;
import com.apzda.cloud.uc.domain.entity.Oauth;
import com.apzda.cloud.uc.domain.entity.OauthSession;
import com.apzda.cloud.uc.domain.entity.User;
import com.apzda.cloud.uc.proto.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.val;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public interface UserManager {

    /**
     * 根据用户名获取用户.
     * @param username 用户名.
     * @return 用户实体.
     * @throws org.springframework.security.core.userdetails.UsernameNotFoundException
     * 用户不存在时
     */
    @Nonnull
    User getUserByUsername(String username);

    /**
     * 根据用户名和认证提供者获取用户.
     * @param username 用户名
     * @param provider 认证提供者
     * @return 用户实体
     * @throws org.springframework.security.core.userdetails.UsernameNotFoundException
     * 用户不存在时
     */
    @Nonnull
    User getUserByUsernameAndProvider(@Nonnull String username, @Nonnull String provider);

    /**
     * 根据OpenID和认证提供者获取Oauth
     * @param openId OpenID
     * @param provider 提供者
     * @return Oauth实例
     * @throws org.springframework.security.core.userdetails.UsernameNotFoundException
     * 不存在时
     */
    @Nonnull
    Oauth getOauthByUsernameAndProvider(@Nonnull String openId, @Nonnull String provider);

    /**
     * 密码是否过期.
     * @param user 用户.
     * @return 过期返回true，反之false.
     */
    boolean isCredentialsExpired(@Nonnull User user);

    /**
     * 设置用户密码过期时间.
     * @param user 用户实例
     */
    void setupUserCredentialsExpired(User user, Integer timeout);

    /**
     * 登录成功后续流程.
     * @param token 认证的令牌
     * @param oauth 三方认证信息
     */
    void onAuthenticated(AbstractAuthenticationToken token, Oauth oauth);

    /**
     * 创建登录会话.
     * @param token Token.
     */
    void createOauthSession(JwtAuthenticationToken token);

    /**
     * 获取上一次登录会话.
     * @param oauth 用户
     * @return 会话
     */
    @Nullable
    OauthSession getLastLoginSession(Oauth oauth);

    /**
     * 创建账号
     * @param request 创建账号DTO
     * @param provider 账户提供者
     * @return 创建结果
     */
    CreateAccountResponse createAccount(CreateAccountRequest request, String provider);

    /**
     * 使用本地账户提供者创建账号.
     * @param request 创建账号DTO
     * @return 创建结果
     */
    default CreateAccountResponse createAccount(CreateAccountRequest request) {
        return createAccount(request, Oauth.SIMPLE);
    }

    @Nonnull
    User createUser(@Nonnull CreateAccountRequest dto);

    @Nonnull
    Oauth createOauthAccount(@Nonnull User user, @Nonnull String openId, @Nonnull String unionId,
            @Nonnull String provider, String recCode);

    /**
     * 开通账号
     * @param request 创建账号DTO
     * @return 新创建的账号实例
     */
    OpenAccountResponse openAccount(OpenAccountRequest request);

    /**
     * 更新账号
     * @param request 更新账号DTO.
     * @return 被更新的实体.
     */
    GsvcExt.CommonRes updateAccount(UpdateAccountRequest request);

    /**
     * 重置账户密码
     * @param request 请求.
     * @return 重置结果
     */
    GsvcExt.CommonRes resetPassword(ResetPasswordRequest request);

    /**
     * 保存账户角色信息.
     * @param roles 角色(role)列表
     * @param user 当前用户实例
     * @param force 强制替换
     */
    void updateRoles(@Nonnull List<String> roles, @Nonnull User user, boolean force);

    GsvcExt.CommonRes assignRole(UpdateRoleRequest request);

    /**
     * 获取当前认证用户
     * @return 认证实例
     */
    static JwtAuthenticationToken getAuthentication() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token
                && token.isAuthenticated()) {
            return token;
        }
        throw TokenException.INVALID_TOKEN;
    }

    /**
     * 获取当前用户元数据.
     * @return 用户元数据实例.
     */
    static UserDetailsMeta getUserDetailsMeta() {
        val auth = getAuthentication();
        val userDetails = auth.getUserDetails();
        if (userDetails.isPresent()) {
            return userDetails.get();
        }
        throw TokenException.INVALID_TOKEN;
    }

}
