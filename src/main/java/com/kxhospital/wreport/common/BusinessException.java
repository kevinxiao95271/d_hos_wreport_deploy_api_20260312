package com.kxhospital.wreport.common;

/**
 * 业务异常，携带自定义 HTTP 响应码。
 * 由 GlobalExceptionHandler 捕获后按 code 返回给前端。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
