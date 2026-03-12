package com.kxhospital.wreport.common;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 从当前 Request 中获取已认证的登录用户
 * JwtFilter 解析 Token 后将 LoginUser 写入 request attribute "loginUser"
 */
public class UserContext {

    private static final String ATTR_KEY = "loginUser";

    public static LoginUser get() {
        HttpServletRequest req = currentRequest();
        return req == null ? null : (LoginUser) req.getAttribute(ATTR_KEY);
    }

    public static void set(HttpServletRequest request, LoginUser user) {
        request.setAttribute(ATTR_KEY, user);
    }

    private static HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }
}
