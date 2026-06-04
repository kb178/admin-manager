package com.ddd.admin.config;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ddd.admin.entity.SysUser;
import com.ddd.admin.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器 — 确保默认管理员密码是 BCrypt 加密的
 * <p>
 * 首次启动时自动执行，将数据库中已有用户的密码更新为正确的 BCrypt 哈希。
 */
@Slf4j
@Component
@RequiredArgsConstructor //自动生成包含 final 字段的构造函数
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper sysUserMapper;

    @Override
    public void run(String... args) {
        log.info("======== 开始检查数据初始化 ========");

        // 检查 admin 用户是否存在
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, "admin");
        SysUser admin = sysUserMapper.selectOne(wrapper);

        if (admin != null) {
            // 检查密码是否已经是 BCrypt 格式（以 $2a$ 开头）
            if (admin.getPassword() == null || !admin.getPassword().startsWith("$2")) {
                log.warn("检测到 admin 密码未加密，正在更新...");
                admin.setPassword(BCrypt.hashpw("admin123"));
                sysUserMapper.updateById(admin);
                log.info("admin 密码已更新为 BCrypt 加密");
            } else {
                log.info("admin 密码已经是 BCrypt 加密，无需更新");
            }
        } else {
            log.info("admin 用户不存在，跳过密码检查");
        }

        // 同样检查其他测试用户
        updatePasswordIfNeeded("zhangsan", "admin123");
        updatePasswordIfNeeded("lisi", "admin123");
        updatePasswordIfNeeded("wangwu", "admin123");

        log.info("======== 数据初始化检查完成 ========");
    }

    private void updatePasswordIfNeeded(String username, String defaultPassword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser user = sysUserMapper.selectOne(wrapper);

        if (user != null && (user.getPassword() == null || !user.getPassword().startsWith("$2"))) {
            user.setPassword(BCrypt.hashpw(defaultPassword));
            sysUserMapper.updateById(user);
            log.info("{} 密码已更新为 BCrypt 加密", username);
        }
    }
}
