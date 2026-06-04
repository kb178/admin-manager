package com.ddd.admin.controller;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.IdUtil;
import com.ddd.admin.common.Result;
import com.ddd.admin.dto.LoginDTO;
import com.ddd.admin.dto.TokenDTO;
import com.ddd.admin.security.JwtUtils;
import com.ddd.admin.service.SysUserService;
import com.google.code.kaptcha.Producer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 认证控制器 — 登录、退出、验证码
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;
    private final JwtUtils jwtUtils;
    private final Producer kaptchaProducer;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${shiro.login.captcha-expire:300}")
    private long captchaExpire;

    /**
     * 获取图形验证码
     * <p>
     * GET /api/auth/captcha
     *
     * @return { captchaKey: "uuid", captchaImage: "data:image/png;base64,..." }
     */
    @GetMapping("/captcha")
    public Result<Map<String, String>> getCaptcha() throws IOException {
        // 1. 生成验证码文本（4位字符）
        String captchaText = kaptchaProducer.createText();

        // 2. 生成验证码图片
        BufferedImage image = kaptchaProducer.createImage(captchaText);

        // 3. 将图片转为 Base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        String base64Image = "data:image/png;base64," +
                Base64.encode(baos.toByteArray());

        // 4. 生成唯一标识，存入 Redis
        String captchaKey = IdUtil.fastSimpleUUID();
        String redisKey = "captcha:" + captchaKey;
        redisTemplate.opsForValue().set(redisKey, captchaText,
                captchaExpire, TimeUnit.SECONDS);

        // 5. 返回结果
        Map<String, String> result = new HashMap<>();
        result.put("captchaKey", captchaKey);
        result.put("captchaImage", base64Image);

        log.debug("生成验证码: key={}, code={}", captchaKey, captchaText);
        return Result.ok(result);
    }

    /**
     * 用户登录
     * <p>
     * POST /api/auth/login
     * <pre>
     * {
     *   "username": "admin",
     *   "password": "admin123",
     *   "captcha": "AB3K",
     *   "captchaKey": "uuid-from-captcha-api"
     * }
     * </pre>
     */
    @PostMapping("/login")
    public Result<TokenDTO> login(@Valid @RequestBody LoginDTO loginDTO,
                                   HttpServletRequest request) {
        // 1. 校验验证码
        String redisKey = "captcha:" + loginDTO.getCaptchaKey();
        String storedCaptcha = redisTemplate.opsForValue().get(redisKey);

        if (storedCaptcha == null) {
            return Result.fail(com.ddd.admin.common.ResultCode.CAPTCHA_EXPIRED);
        }
        if (!storedCaptcha.equalsIgnoreCase(loginDTO.getCaptcha())) {
            // 验证码错误，立即删除（防止暴力破解）
            redisTemplate.delete(redisKey);
            return Result.fail(com.ddd.admin.common.ResultCode.CAPTCHA_ERROR);
        }

        // 2. 验证码使用后立即删除（一次性）
        redisTemplate.delete(redisKey);

        // 3. 执行登录
        String clientIp = getClientIp(request);
        TokenDTO tokenDTO = sysUserService.login(loginDTO, clientIp);

        return Result.ok("登录成功", tokenDTO);
    }

    /**
     * 刷新 Token
     * <p>
     * POST /api/auth/refresh
     * <pre>
     * { "refreshToken": "xxx" }
     * </pre>
     */
    @PostMapping("/refresh")
    public Result<TokenDTO> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return Result.fail(com.ddd.admin.common.ResultCode.TOKEN_INVALID);
        }
        TokenDTO tokenDTO = sysUserService.refreshToken(refreshToken);
        return Result.ok(tokenDTO);
    }

    /**
     * 退出登录
     * <p>
     * POST /api/auth/logout  (需要携带 JWT)
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        sysUserService.logout(token);
        return Result.ok("退出成功", null);
    }

    // ======================== 私有方法 ========================

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 从请求中提取 JWT Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }
}
