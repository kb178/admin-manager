package com.ddd.admin.security;

import org.apache.shiro.authc.AuthenticationToken;

/**
 * JWT 认证 Token — 替代 Shiro 默认的 UsernamePasswordToken
 * <p>
 * 用于无状态认证：前端每次请求携带 JWT，Shiro 通过此类进行认证。
 */
public class JwtToken implements AuthenticationToken {

    private static final long serialVersionUID = 1L;

    /** JWT 令牌字符串 */
    private final String token;

    public JwtToken(String token) {
        this.token = token;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }

    @Override
    public Object getCredentials() {
        return token;
    }
}
