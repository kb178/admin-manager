package com.ddd.admin.security;

import com.ddd.admin.entity.SysUser;
import com.ddd.admin.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Shiro 认证授权 Realm
 * <p>
 * 负责：
 * <ul>
 *   <li>认证（Authentication）：验证 JWT Token 的合法性</li>
 *   <li>授权（Authorization）：获取用户的角色和权限</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRealm extends AuthorizingRealm {

    private final SysUserService sysUserService;
    private final JwtUtils jwtUtils;

    /**
     * 必须重写此方法，让 Realm 支持自定义的 JwtToken
     */
    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JwtToken;
    }

    /**
     * 授权 — 获取用户的角色和权限
     * <p>
     * 每次调用 Subject.hasRole() 或 @RequiresRoles 注解时触发。
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        // 从 Principal 中获取用户ID
        Long userId = (Long) principals.getPrimaryPrincipal();

        // 查询用户角色
        Set<String> roles = sysUserService.getUserRoles(userId);

        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        info.setRoles(roles);
        // 角色编码作为权限标识（可根据业务扩展为独立的权限表）
        info.setStringPermissions(roles);

        log.debug("用户授权: userId={}, roles={}", userId, roles);
        return info;
    }

    /**
     * 认证 — 验证 JWT Token
     * <p>
     * 每次请求时通过 JwtFilter 触发。
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token)
            throws AuthenticationException {

        String jwtToken = (String) token.getCredentials();

        // 1. 验证 Token 合法性
        if (!jwtUtils.validateToken(jwtToken)) {
            throw new ExpiredCredentialsException("Token已过期或无效");
        }

        // 2. 从 Token 中提取用户信息
        Long userId = jwtUtils.getUserId(jwtToken);
        String username = jwtUtils.getUsername(jwtToken);

        // 3. 查询用户是否存在且启用
        SysUser user = sysUserService.findByUsername(username);
        if (user == null) {
            throw new UnknownAccountException("用户不存在: " + username);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new DisabledAccountException("用户已被禁用: " + username);
        }

        log.debug("JWT认证成功: userId={}, username={}", userId, username);

        // 返回认证信息
        return new SimpleAuthenticationInfo(userId, jwtToken, getName());
    }

    /**
     * 清除授权缓存（当用户角色变更时调用）
     */
    @Override
    public void clearCachedAuthorizationInfo(PrincipalCollection principals) {
        super.clearCachedAuthorizationInfo(principals);
    }
}
