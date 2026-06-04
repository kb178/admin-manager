package com.ddd.admin.common;

/**
 * 统一响应状态码
 */
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "没有访问权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    CONFLICT(409, "数据冲突"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),

    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    // 业务错误码 (1000+)
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_DISABLED(1002, "用户已被禁用"),
    USER_LOCKED(1003, "用户已被锁定，请{0}分钟后再试"),
    BAD_CREDENTIALS(1004, "用户名或密码错误"),
    CAPTCHA_ERROR(1005, "验证码错误或已过期"),
    CAPTCHA_EXPIRED(1006, "验证码已过期，请刷新"),
    USERNAME_EXISTS(1007, "用户名已存在"),
    ROLE_CODE_EXISTS(1008, "角色编码已存在"),
    OLD_PASSWORD_ERROR(1009, "原密码错误"),
    TOKEN_EXPIRED(1010, "Token已过期，请重新登录"),
    TOKEN_INVALID(1011, "Token无效");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
