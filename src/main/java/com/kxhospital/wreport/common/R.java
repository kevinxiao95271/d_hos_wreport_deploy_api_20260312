package com.kxhospital.wreport.common;

import lombok.Data;

/**
 * 统一响应体
 * code: 200=成功, 非200=失败
 */
@Data
public class R<T> {

    private int    code;
    private String message;
    private T      data;

    private R() {}

    public static <T> R<T> ok() {
        R<T> r = new R<>();
        r.code    = 200;
        r.message = "success";
        return r;
    }

    public static <T> R<T> ok(T data) {
        R<T> r = ok();
        r.data = data;
        return r;
    }

    public static <T> R<T> fail(String message) {
        R<T> r = new R<>();
        r.code    = 500;
        r.message = message;
        return r;
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.code    = code;
        r.message = message;
        return r;
    }
}
