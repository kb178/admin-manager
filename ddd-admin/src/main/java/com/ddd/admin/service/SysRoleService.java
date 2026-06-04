package com.ddd.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ddd.admin.entity.SysRole;

/**
 * 角色 Service
 */
public interface SysRoleService extends IService<SysRole> {

    /**
     * 获取用户的角色列表
     */
    java.util.List<SysRole> getRolesByUserId(Long userId);
}
