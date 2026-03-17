package com.kxhospital.wreport.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidation(MethodArgumentNotValidException ex) {
        BindingResult br = ex.getBindingResult();
        String msg = br.getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return R.fail(400, msg);
    }

    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusiness(BusinessException ex) {
        log.warn("业务拦截 [{}]: {}", ex.getCode(), ex.getMessage());
        return R.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public R<Void> handleRuntime(RuntimeException ex) {
        log.warn("业务异常: {}", ex.getMessage());
        return R.fail(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception ex) {
        log.error("系统异常", ex);
        return R.fail(500, "系统错误: " + ex.getMessage());
    }
}
