package com.ddd.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ddd.admin.common.Result;
import com.ddd.admin.dto.TokenDTO;
import com.ddd.admin.dto.UserCreateDTO;
import com.ddd.admin.dto.UserQueryDTO;
import com.ddd.admin.entity.SysUser;
import com.ddd.admin.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService sysUserService;

    /**
     * 获取当前登录用户信息
     * <p>
     * GET /api/user/me
     */
    @GetMapping("/me")
    public Result<Object> getCurrentUser() {
        Long userId = (Long) SecurityUtils.getSubject().getPrincipal();
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return Result.fail(com.ddd.admin.common.ResultCode.USER_NOT_FOUND);
        }

        // 脱敏（不返回密码）
        user.setPassword(null);

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("user", user);
        data.put("roles", sysUserService.getUserRoles(userId));

        return Result.ok(data);
    }

    /**
     * 分页查询用户列表（管理员权限）
     * <p>
     * GET /api/user/list?username=&realName=&status=&page=1&pageSize=10
     */
    @GetMapping("/list")
    @RequiresRoles({"SUPER_ADMIN", "ADMIN"})
    public Result<Page<SysUser>> list(UserQueryDTO queryDTO) {
        Page<SysUser> page = sysUserService.queryPage(queryDTO);
        // 脱敏
        page.getRecords().forEach(u -> u.setPassword(null));
        return Result.ok(page);
    }

    /**
     * 根据ID获取用户详情
     * <p>
     * GET /api/user/{id}
     */
    @GetMapping("/{id}")
    @RequiresRoles({"SUPER_ADMIN", "ADMIN"})
    public Result<SysUser> getById(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return Result.fail(com.ddd.admin.common.ResultCode.USER_NOT_FOUND);
        }
        user.setPassword(null);
        return Result.ok(user);
    }

    /**
     * 创建用户
     * <p>
     * POST /api/user
     */
    @PostMapping
    @RequiresRoles({"SUPER_ADMIN"})
    public Result<Void> create(@Valid @RequestBody UserCreateDTO dto) {
        Long operatorId = (Long) SecurityUtils.getSubject().getPrincipal();
        sysUserService.createUser(dto, operatorId);
        return Result.ok("创建成功", null);
    }

    /**
     * 更新用户
     * <p>
     * PUT /api/user/{id}
     */
    @PutMapping("/{id}")
    @RequiresRoles({"SUPER_ADMIN", "ADMIN"})
    public Result<Void> update(@PathVariable Long id,
                                @Valid @RequestBody UserCreateDTO dto) {
        Long operatorId = (Long) SecurityUtils.getSubject().getPrincipal();
        sysUserService.updateUser(id, dto, operatorId);
        return Result.ok("更新成功", null);
    }

    /**
     * 删除用户（逻辑删除）
     * <p>
     * DELETE /api/user/{id}
     */
    @DeleteMapping("/{id}")
    @RequiresRoles({"SUPER_ADMIN"})
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.deleteUser(id);
        return Result.ok("删除成功", null);
    }

    /**
     * 修改密码
     * <p>
     * PUT /api/user/change-password
     * <pre>
     * { "oldPassword": "xxx", "newPassword": "yyy" }
     * </pre>
     */
    @PutMapping("/change-password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        if (oldPassword == null || newPassword == null ||
                oldPassword.isBlank() || newPassword.isBlank()) {
            return Result.fail(com.ddd.admin.common.ResultCode.BAD_REQUEST, "密码不能为空");
        }
        if (newPassword.length() < 6) {
            return Result.fail(com.ddd.admin.common.ResultCode.BAD_REQUEST, "新密码长度至少6位");
        }

        Long userId = (Long) SecurityUtils.getSubject().getPrincipal();
        sysUserService.changePassword(userId, oldPassword, newPassword);
        return Result.ok("密码修改成功", null);
    }

    /**
     * 重置密码（管理员专用）
     * <p>
     * PUT /api/user/{id}/reset-password
     */
    @PutMapping("/{id}/reset-password")
    @RequiresRoles({"SUPER_ADMIN"})
    public Result<Void> resetPassword(@PathVariable Long id,
                                       @RequestBody Map<String, String> body) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            return Result.fail(com.ddd.admin.common.ResultCode.BAD_REQUEST, "密码不能为空");
        }

        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return Result.fail(com.ddd.admin.common.ResultCode.USER_NOT_FOUND);
        }

        user.setPassword(cn.hutool.crypto.digest.BCrypt.hashpw(newPassword));
        sysUserService.updateById(user);

        log.info("管理员重置用户密码: userId={}", id);
        return Result.ok("密码重置成功", null);
    }
}
