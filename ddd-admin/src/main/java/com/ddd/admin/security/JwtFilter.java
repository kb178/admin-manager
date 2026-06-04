package com.ddd.admin.security;

import cn.hutool.json.JSONUtil;
import com.ddd.admin.common.Result;
import com.ddd.admin.common.ResultCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器 — Spring OncePerRequestFilter，不依赖 Shiro 的 Filter 体系
 */
@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {

    @Value("${jwt.header:Authorization}")
    private String headerName;

    @Value("${jwt.token-prefix:Bearer}")
    private String tokenPrefix;

    private final JwtUtils jwtUtils;
    private final SecurityManager securityManager;

    /** 不需要 Token 的公开路径 */
    private static final String[] PUBLIC_PATHS = {
            "/api/auth/login",
            "/api/auth/captcha",
            "/api/auth/refresh",
            "/api/auth/logout",          // ← 退出登录无需提前认证（Controller 自行取 token 处理）
            "/api/doc.html",
            "/api/v3/api-docs",
            "/api/swagger-ui",
            "/api/webjars",
            "/api/favicon.ico"
    };

    public JwtFilter(JwtUtils jwtUtils, SecurityManager securityManager) {
        this.jwtUtils = jwtUtils;
        this.securityManager = securityManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        // 1. OPTIONS 预检直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 公开路径直接放行
        String requestUri = request.getRequestURI();
        if (isPublicPath(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 提取 Token
        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            sendError(response, ResultCode.UNAUTHORIZED, "请先登录");
            return;
        }

        // 4. 验证 Token
        if (!jwtUtils.validateToken(token)) {
            sendError(response, ResultCode.TOKEN_EXPIRED, "Token已过期或无效，请重新登录");
            return;
        }

        // 5. 手动调用 Shiro 认证
        try {
            JwtToken jwtToken = new JwtToken(token);
            // 用注入的 SecurityManager 创建 Subject，确保不依赖 ThreadContext 中的绑定
            Subject subject = new Subject.Builder(securityManager).buildSubject();
            //触发认证流程调用doGetAuthenticationInfo
            subject.login(jwtToken);
            ThreadContext.bind(subject);
            log.debug("JWT认证成功: uri={}", requestUri);
        } catch (Exception e) {
            log.warn("JWT认证失败: uri={}, error={}", requestUri, e.getMessage());
            sendError(response, ResultCode.TOKEN_INVALID, "认证失败，请重新登录");
            return;
        }

        // 6. 放行
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 请求结束后解绑 Subject，防止内存泄漏
            ThreadContext.unbindSubject();
        }
    }

    // ======================== 私有方法 ========================

    private boolean isPublicPath(String uri) {
        for (String path : PUBLIC_PATHS) {
            if (uri.equals(path) || (path.endsWith("/**") && uri.startsWith(path.substring(0, path.length() - 3)))) {
                return true;
            }
        }
        if (uri.startsWith("/api/static/") || uri.equals("/api/favicon.ico")) {
            return true;
        }
        return false;
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(headerName);
        // 判断 header 是否以 "Bearer " 开头
        if (StringUtils.hasText(header) && header.startsWith(tokenPrefix)) {
            // 截取 "Bearer " 之后的部分，并去除首尾空格，得到纯 Token 字符串
            return header.substring(tokenPrefix.length()).trim();
        }
        return null;
    }

    private void sendError(HttpServletResponse response, ResultCode resultCode, String message) {
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        try {
            // 响应结果
            response.getWriter().write(JSONUtil.toJsonStr(Result.fail(resultCode, message)));
            response.getWriter().flush();
        } catch (IOException e) {
            log.error("响应写入失败", e);
        }
    }
}
