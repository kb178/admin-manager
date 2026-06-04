package com.ddd.admin.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置类
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * 插件配置
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 分页插件（MySQL 方言）
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(500L);   // 单页最大500条
        interceptor.addInnerInterceptor(paginationInterceptor);

        // 2. 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 3. 防全表更新/删除插件
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        return interceptor;
    }

    /**
     * 字段自动填充处理器
     * <p>
     * 配合实体类上的 @TableField(fill = FieldFill.INSERT) 等注解使用
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                // 创建时间
                this.strictInsertFill(metaObject, "createdTime", LocalDateTime.class, LocalDateTime.now());
                // 更新时间
                this.strictInsertFill(metaObject, "updatedTime", LocalDateTime.class, LocalDateTime.now());
                // 创建人（需从上下文获取，此处做空值保护）
                this.strictInsertFill(metaObject, "createdBy", Long.class,
                        getCurrentUserId());
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                // 更新时间
                this.strictUpdateFill(metaObject, "updatedTime", LocalDateTime.class, LocalDateTime.now());
                // 修改人
                this.strictUpdateFill(metaObject, "updatedBy", Long.class,
                        getCurrentUserId());
            }

            /**
             * 从 ThreadLocal 或 Shiro Subject 获取当前用户ID
             */
            private Long getCurrentUserId() {
                try {
                    org.apache.shiro.subject.Subject subject =
                            org.apache.shiro.SecurityUtils.getSubject();
                    if (subject != null && subject.getPrincipal() instanceof Long) {
                        return (Long) subject.getPrincipal();
                    }
                } catch (Exception ignored) {
                    // 非登录状态（如定时任务），返回 null
                }
                return null;
            }
        };
    }
}
