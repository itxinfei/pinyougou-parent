package com.pinyougou.manager.exception;

import com.pinyougou.exception.ValidationException;
import entity.Result;
import org.apache.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Set;

/**
 * 全局异常处理器
 * <p>
 * 功能：
 * 1. 统一处理Controller层异常
 * 2. 记录异常日志
 * 3. 返回统一的错误格式
 * 4. 避免敏感信息泄露
 *
 * @author Administrator
 * @since 1.0-SNAPSHOT
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = Logger.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理参数校验异常（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        logger.warn("参数校验失败", e);
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return new Result(false, message);
    }

    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public Result handleBindException(BindException e) {
        logger.warn("参数绑定失败", e);
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数绑定失败");
        return new Result(false, message);
    }

    /**
     * 处理参数验证异常（@Validated + @RequestParam）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result handleConstraintViolation(ConstraintViolationException e) {
        logger.warn("参数验证失败", e);
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String message = violations.stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数验证失败");
        return new Result(false, message);
    }

    /**
     * 处理业务验证异常
     */
    @ExceptionHandler(ValidationException.class)
    public Result handleValidation(ValidationException e) {
        logger.warn("业务验证失败: " + e.getMessage());
        return new Result(false, e.getMessage());
    }

    /**
     * 处理权限不足异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result handleAccessDenied(AccessDeniedException e) {
        logger.warn("权限不足", e);
        return new Result(false, "权限不足，请联系管理员");
    }

    /**
     * 处理认证失败异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public Result handleAuthentication(AuthenticationException e) {
        logger.warn("认证失败", e);
        if (e instanceof BadCredentialsException) {
            return new Result(false, "用户名或密码错误");
        }
        return new Result(false, "认证失败，请重新登录");
    }

    /**
     * 处理数据完整性违反异常（如唯一约束冲突）
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result handleDataIntegrityViolation(DataIntegrityViolationException e) {
        logger.error("数据完整性异常", e);
        String message = "数据已存在或存在关联数据，请检查后重试";
        if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
            message = "数据重复，请检查后重试";
        }
        return new Result(false, message);
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public Result handleNullPointerException(NullPointerException e) {
        logger.error("空指针异常", e);
        return new Result(false, "系统繁忙，请稍后重试");
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("非法参数: " + e.getMessage());
        return new Result(false, "参数错误: " + e.getMessage());
    }

    /**
     * 处理其他所有异常
     * <p>
     * 注意：生产环境不返回详细异常信息，避免泄露系统细节
     */
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        logger.error("系统异常", e);
        // 生产环境返回通用错误信息
        return new Result(false, "系统繁忙，请稍后重试");
    }
}
