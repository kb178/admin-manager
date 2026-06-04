package com.ddd.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ddd.admin.entity.SysRole;
import com.ddd.admin.entity.SysUserRole;
import com.ddd.admin.mapper.SysRoleMapper;
import com.ddd.admin.service.SysRoleService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 角色 Service 实现
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysUserRoleServiceImpl sysUserRoleService;

    public SysRoleServiceImpl(SysUserRoleServiceImpl sysUserRoleService) {
        this.sysUserRoleService = sysUserRoleService;
    }

    @Override
    public List<SysRole> getRolesByUserId(Long userId) {
        // 查询用户角色关系
        LambdaQueryWrapper<SysUserRole> urWrapper = new LambdaQueryWrapper<>();
        urWrapper.eq(SysUserRole::getUserId, userId);
        List<SysUserRole> userRoles = sysUserRoleService.list(urWrapper);

        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询角色详情（仅启用且未删除）
        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .toList();

        LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.in(SysRole::getId, roleIds)
                .eq(SysRole::getStatus, 1);
        return baseMapper.selectList(roleWrapper);
    }
}
