package com.ddd.admin.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ddd.admin.common.BusinessException;
import com.ddd.admin.common.ResultCode;
import com.ddd.admin.dto.*;
import com.ddd.admin.entity.SysRole;
import com.ddd.admin.entity.SysUser;
import com.ddd.admin.entity.SysUserRole;
import com.ddd.admin.mapper.SysUserMapper;
import com.ddd.admin.security.JwtUtils;
import com.ddd.admin.service.SysRoleService;
import com.ddd.admin.service.SysUserRoleService;
import com.ddd.admin.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleService sysRoleService;
    private final SysUserRoleService sysUserRoleService;
    private final JwtUtils jwtUtils;
    private final RedisTemplate<String, String> redisTemplate;

    // ======================== 登录相关 ========================

    @Override
    public TokenDTO login(LoginDTO loginDTO, String clientIp) {
        String username = loginDTO.getUsername();

        // 1. 检查是否被锁定
        String lockKey = "login:lock:" + username;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
            throw new BusinessException(ResultCode.USER_LOCKED,
                    "账户已锁定，请" + (ttl != null ? ttl / 60 + 1 : 15) + "分钟后再试");
        }

        // 2. 查询用户
        SysUser user = findByUsername(username);
        if (user == null) {
            recordLoginFailure(username);
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 3. 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 4. 验证密码 (BCrypt)
        if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
            recordLoginFailure(username);
            throw new BusinessException(ResultCode.BAD_CREDENTIALS);
        }

        // 5. 登录成功，清除失败记录
        String failKey = "login:fail:" + username;
        redisTemplate.delete(failKey);
        redisTemplate.delete(lockKey);

        // 6. 查询用户角色
        Set<String> roles = getUserRoles(user.getId());
        // 角色编码即为权限标识（可根据业务扩展）
        Set<String> permissions = new HashSet<>(roles);

        // 7. 生成 Token
        String accessToken = jwtUtils.generateToken(user.getId(), user.getUsername(), roles);
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());

        // 8. 存储 Token 到 Redis（用于退出登录时加入黑名单）
        String tokenKey = "token:access:" + user.getId();
        redisTemplate.opsForValue().set(tokenKey, accessToken,
                jwtUtils.getExpire(), TimeUnit.SECONDS);

        // 9. 更新最后登录信息
        updateLoginInfo(user.getId(), clientIp);

        // 10. 构建用户信息
        UserInfoDTO userInfo = UserInfoDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .gender(user.getGender())
                .roles(roles)
                .permissions(permissions)
                .build();

        log.info("用户登录成功: username={}, ip={}", username, clientIp);

        return TokenDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getExpire())
                .userInfo(userInfo)
                .build();
    }

    @Override
    public TokenDTO refreshToken(String refreshToken) {
        // 验证 Refresh Token
        if (!jwtUtils.validateToken(refreshToken)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        Long userId = jwtUtils.getUserId(refreshToken);
        String username = jwtUtils.getUsername(refreshToken);

        // 查询用户是否存在且启用
        SysUser user = findByUsername(username);
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 生成新的 Access Token
        Set<String> roles = getUserRoles(userId);
        String newAccessToken = jwtUtils.generateToken(userId, username, roles);

        // 更新 Redis 中的 Token
        String tokenKey = "token:access:" + userId;
        redisTemplate.opsForValue().set(tokenKey, newAccessToken,
                jwtUtils.getExpire(), TimeUnit.SECONDS);

        return TokenDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getExpire())
                .build();
    }

    @Override
    public void logout(String accessToken) {
        try {
            Long userId = jwtUtils.getUserId(accessToken);
            // 将当前 Token 加入黑名单
            String blackKey = "token:blacklist:" + accessToken;
            redisTemplate.opsForValue().set(blackKey, "1",
                    jwtUtils.getExpire(), TimeUnit.SECONDS);
            // 删除用户 Token 记录
            redisTemplate.delete("token:access:" + userId);
            log.info("用户退出登录: userId={}", userId);
        } catch (Exception e) {
            log.warn("退出登录处理异常: {}", e.getMessage());
        }
    }

    // ======================== CRUD ========================

    @Override
    public Page<SysUser> queryPage(UserQueryDTO queryDTO) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(queryDTO.getUsername()), SysUser::getUsername, queryDTO.getUsername())
                .like(StrUtil.isNotBlank(queryDTO.getRealName()), SysUser::getRealName, queryDTO.getRealName())
                .like(StrUtil.isNotBlank(queryDTO.getPhone()), SysUser::getPhone, queryDTO.getPhone())
                .eq(StrUtil.isNotBlank(queryDTO.getEmail()), SysUser::getEmail, queryDTO.getEmail())
                .eq(queryDTO.getStatus() != null, SysUser::getStatus, queryDTO.getStatus())
                .eq(queryDTO.getDeptId() != null, SysUser::getDeptId, queryDTO.getDeptId())
                .orderByDesc(SysUser::getCreatedTime);

        return baseMapper.selectPage(
                new Page<>(queryDTO.getPage(), queryDTO.getPageSize()), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(UserCreateDTO dto, Long operatorId) {
        // 检查用户名是否已存在
        SysUser existUser = findByUsername(dto.getUsername());
        if (existUser != null) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));  // BCrypt 加密
        user.setRealName(dto.getRealName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setGender(Optional.ofNullable(dto.getGender()).orElse(0));
        user.setStatus(Optional.ofNullable(dto.getStatus()).orElse(1));
        user.setDeptId(dto.getDeptId());
        user.setRemark(dto.getRemark());
        user.setCreatedBy(operatorId);
        baseMapper.insert(user);

        // 分配角色
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            sysUserRoleService.assignRoles(user.getId(), dto.getRoleIds(), operatorId);
        }

        log.info("创建用户成功: username={}, operatorId={}", dto.getUsername(), operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long userId, UserCreateDTO dto, Long operatorId) {
        SysUser user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 如果修改了用户名，检查唯一性
        if (!user.getUsername().equals(dto.getUsername())) {
            SysUser existUser = findByUsername(dto.getUsername());
            if (existUser != null && !existUser.getId().equals(userId)) {
                throw new BusinessException(ResultCode.USERNAME_EXISTS);
            }
            user.setUsername(dto.getUsername());
        }

        // 如果传了新密码，则重新加密
        if (StrUtil.isNotBlank(dto.getPassword())) {
            user.setPassword(BCrypt.hashpw(dto.getPassword()));
        }

        user.setRealName(dto.getRealName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setGender(dto.getGender());
        user.setStatus(dto.getStatus());
        user.setDeptId(dto.getDeptId());
        user.setRemark(dto.getRemark());
        user.setUpdatedBy(operatorId);
        baseMapper.updateById(user);

        // 重新分配角色
        if (dto.getRoleIds() != null) {
            sysUserRoleService.assignRoles(userId, dto.getRoleIds(), operatorId);
        }

        log.info("更新用户成功: userId={}, operatorId={}", userId, operatorId);
    }

    @Override
    public void deleteUser(Long userId) {
        SysUser user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        // MyBatis-Plus 会自动执行逻辑删除
        baseMapper.deleteById(userId);
        log.info("删除用户: userId={}", userId);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 验证原密码
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }

        // 更新密码
        user.setPassword(BCrypt.hashpw(newPassword));
        baseMapper.updateById(user);

        // 退出所有已登录的会话
        String tokenKey = "token:access:" + userId;
        String oldToken = redisTemplate.opsForValue().get(tokenKey);
        if (StrUtil.isNotBlank(oldToken)) {
            String blackKey = "token:blacklist:" + oldToken;
            redisTemplate.opsForValue().set(blackKey, "1",
                    jwtUtils.getExpire(), TimeUnit.SECONDS);
        }
        redisTemplate.delete(tokenKey);

        log.info("用户修改密码成功: userId={}", userId);
    }

    // ======================== 工具方法 ========================

    @Override
    public Set<String> getUserRoles(Long userId) {
        List<SysRole> roles = sysRoleService.getRolesByUserId(userId);
        return roles.stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toSet());
    }

    @Override
    public SysUser findByUsername(String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public void updateLoginInfo(Long userId, String ip) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ip);
        baseMapper.updateById(user);
    }

    // ======================== 私有方法 ========================

    /**
     * 记录登录失败
     */
    private void recordLoginFailure(String username) {
        String failKey = "login:fail:" + username;
        Long failCount = redisTemplate.opsForValue().increment(failKey);
        redisTemplate.expire(failKey, 30, TimeUnit.MINUTES);

        log.warn("登录失败: username={}, failCount={}", username, failCount);

        // 失败超过5次，锁定15分钟
        if (failCount != null && failCount >= 5) {
            String lockKey = "login:lock:" + username;
            redisTemplate.opsForValue().set(lockKey, "LOCKED", 15, TimeUnit.MINUTES);
            redisTemplate.delete(failKey);
            log.warn("账户已锁定: username={}", username);
        }
    }
}
