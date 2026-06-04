package com.ddd.admin.config;

import com.ddd.admin.security.UserRealm;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.pam.AtLeastOneSuccessfulStrategy;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.DefaultWebSessionStorageEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Apache Shiro 配置类
 * <p>
 * JWT 认证由 Spring 的 JwtFilter（OncePerRequestFilter）负责，
 * Shiro 只负责授权（@RequiresRoles 注解）和 Subject 生命周期管理。
 * <p>
 * 关键：显式调用 SecurityUtils.setSecurityManager() 确保无论
 * JwtFilter 还是 ShiroFilter 先执行，都能获取到 SecurityManager。
 */
@Configuration
@RequiredArgsConstructor
public class ShiroConfig {

    private final UserRealm userRealm;

    /**
     * 安全管理器 — 同时注册为全局静态单例
     */
    @Bean
    public DefaultWebSecurityManager securityManager() {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();

        // ======== 禁用 Session（无状态 JWT 认证） ========
        DefaultSessionStorageEvaluator sessionStorageEvaluator = new DefaultWebSessionStorageEvaluator();
        sessionStorageEvaluator.setSessionStorageEnabled(false);
        DefaultSubjectDAO subjectDAO = new DefaultSubjectDAO();
        subjectDAO.setSessionStorageEvaluator(sessionStorageEvaluator);
        securityManager.setSubjectDAO(subjectDAO);

        // ======== 先设置认证器，再设置 Realm（否则 afterRealmsSet 会注入到默认认证器） ========
        ModularRealmAuthenticator authenticator = new ModularRealmAuthenticator();
        authenticator.setAuthenticationStrategy(new AtLeastOneSuccessfulStrategy());
        securityManager.setAuthenticator(authenticator);

        // ★ 设置 Realm（此时 afterRealmsSet 会将 Realm 注入已配置的 authenticator）
        securityManager.setRealm(userRealm);

        // ★ 显式注册为 VM 静态单例
        SecurityUtils.setSecurityManager(securityManager);

        return securityManager;
    }

    /**
     * Shiro 过滤器工厂
     * <p>
     * JWT 认证已由 Spring JwtFilter 处理，此处 Shiro 链全部放行，
     * 仅维护 Subject 生命周期，支持 @RequiresRoles 等注解鉴权。
     */
    @Bean
    public ShiroFilterFactoryBean shiroFilter(SecurityManager securityManager) {
        ShiroFilterFactoryBean factory = new ShiroFilterFactoryBean();
        factory.setSecurityManager(securityManager);

        Map<String, String> filterChainMap = new LinkedHashMap<>();
        filterChainMap.put("/**", "anon");
        factory.setFilterChainDefinitionMap(filterChainMap);

        return factory;
    }
}
