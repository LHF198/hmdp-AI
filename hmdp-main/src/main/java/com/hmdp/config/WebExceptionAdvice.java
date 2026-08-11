package com.hmdp.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.hmdp.dto.Result;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class WebExceptionAdvice {

    /**
     * 通用异常兜底：尽量返回具体原因，避免只提示"服务器异常"
     */
    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("接口 [{} {}] 处理异常", request.getMethod(), request.getRequestURI(), e);
        // 优先取异常自带消息，其次取根因消息
        String msg = e.getMessage();
        if (StrUtil.isBlank(msg) && e.getCause() != null) {
            msg = e.getCause().getMessage();
        }
        if (StrUtil.isBlank(msg)) {
            return Result.fail("服务器异常，请稍后重试");
        }
        // 截断消息，避免内部细节过多暴露给前端
        return Result.fail("服务器异常：" + StrUtil.sub(msg, 0, 120));
    }

    /**
     * 请求体参数校验失败（@Valid 校验 @RequestBody DTO）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        log.warn("参数校验失败: 字段={}, 原因={}", fieldError != null ? fieldError.getField() : "-", msg);
        return Result.fail(msg);
    }

    /**
     * 请求体不是合法 JSON
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体格式错误: {}", e.getMessage());
        return Result.fail("请求体格式错误，请检查 JSON 数据");
    }

    /**
     * 路径/查询参数类型不匹配，如 /shop/{id} 传入非数字
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        Class<?> requiredType = e.getRequiredType();
        String expected = requiredType != null ? requiredType.getSimpleName() : "合法";
        String msg = "参数 " + e.getName() + " 类型不正确，应为" + expected;
        log.warn("参数类型不匹配: {}", msg);
        return Result.fail(msg);
    }

    /**
     * Content-Type 不支持
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("不支持的 Content-Type: {}", e.getContentType());
        return Result.fail("请求格式不支持，请使用 application/json");
    }

    /**
     * 请求方法不支持，如用 GET 调 POST 接口
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMethod());
        return Result.fail("请求方法不支持: " + e.getMethod());
    }

    /**
     * 接口不存在
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result handleNoResource(NoResourceFoundException e) {
        log.warn("接口不存在: {}", e.getResourcePath());
        return Result.fail("请求的接口不存在: " + e.getResourcePath());
    }

    /**
     * 缺少必填请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return Result.fail("缺少必要参数: " + e.getParameterName());
    }

    /**
     * 方法级参数校验失败（@Validated 作用于 @RequestParam/@PathVariable 时抛此异常）
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public Result handleMethodValidation(HandlerMethodValidationException e) {
        String msg = e.getAllValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("参数校验失败");
        log.warn("方法参数校验失败: {}", msg);
        return Result.fail(msg);
    }

    /**
     * 兼容暴露 ConstraintViolation 的校验配置（部分场景方法校验会抛此异常）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .findFirst()
                .map(jakarta.validation.ConstraintViolation::getMessage)
                .orElse("参数校验失败");
        log.warn("方法参数校验失败: {}", msg);
        return Result.fail(msg);
    }
}
