package com.ddd.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ddd.admin.dto.*;
import com.ddd.admin.entity.SysUser;

import java.util.List;
import java.util.Set;

/**
 * 用户 Service
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 登录
     */
    TokenDTO login(LoginDTO loginDTO, String clientIp);

    /**
     * 刷新 Token
     */
    TokenDTO refreshToken(String refreshToken);

    /**
     * 退出登录
     */
    void logout(String accessToken);

    /**
     * 分页查询用户
     */
    Page<SysUser> queryPage(UserQueryDTO queryDTO);

    /**
     * 创建用户
     */
    void createUser(UserCreateDTO dto, Long operatorId);

    /**
     * 更新用户
     */
    void updateUser(Long userId, UserCreateDTO dto, Long operatorId);

    /**
     * 删除用户（逻辑删除）
     */
    void deleteUser(Long userId);

    /**
     * 修改密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 获取用户角色编码
     */
    Set<String> getUserRoles(Long userId);

    /**
     * 根据用户名查找用户
     */
    SysUser findByUsername(String username);

    /**
     * 更新最后登录信息
     */
    void updateLoginInfo(Long userId, String ip);
}
