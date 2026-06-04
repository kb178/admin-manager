package com.ddd.admin.common;

import cn.hutool.core.text.StrFormatter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthorizedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ======================== 业务异常 ========================

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    // ======================== Shiro 认证异常 ========================

    @ExceptionHandler(UnknownAccountException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleUnknownAccount(UnknownAccountException e) {
        return Result.fail(ResultCode.USER_NOT_FOUND);
    }

    @ExceptionHandler(IncorrectCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleIncorrectCredentials(IncorrectCredentialsException e) {
        return Result.fail(ResultCode.BAD_CREDENTIALS);
    }

    @ExceptionHandler(LockedAccountException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleLockedAccount(LockedAccountException e) {
        return Result.fail(ResultCode.USER_LOCKED);
    }

    @ExceptionHandler(DisabledAccountException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleDisabledAccount(DisabledAccountException e) {
        return Result.fail(ResultCode.USER_DISABLED);
    }

    @ExceptionHandler(ExcessiveAttemptsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleExcessiveAttempts(ExcessiveAttemptsException e) {
        return Result.fail(ResultCode.TOO_MANY_REQUESTS, "登录尝试次数过多，请15分钟后再试");
    }

    @ExceptionHandler(ExpiredCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleExpiredCredentials(ExpiredCredentialsException e) {
        return Result.fail(ResultCode.TOKEN_EXPIRED);
    }

    // ======================== Shiro 授权异常 ========================

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleUnauthorized(UnauthorizedException e) {
        return Result.fail(ResultCode.FORBIDDEN);
    }

    @ExceptionHandler(AuthorizationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAuthorization(AuthorizationException e) {
        return Result.fail(ResultCode.FORBIDDEN);
    }

    // ======================== 参数校验异常 ========================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBind(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), message);
    }

    // ======================== 其他异常 ========================

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFound(NoHandlerFoundException e) {
        return Result.fail(ResultCode.NOT_FOUND, "接口不存在: " + e.getRequestURL());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<Void> handleDataIntegrity(DataIntegrityViolationException e) {
        log.error("数据完整性异常", e);
        return Result.fail(ResultCode.CONFLICT, "数据操作冲突，请重试");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ResultCode.INTERNAL_ERROR.getCode(),
                ResultCode.INTERNAL_ERROR.getMessage());
    }
}
