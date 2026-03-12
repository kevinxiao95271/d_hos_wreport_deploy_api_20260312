package com.kxhospital.wreport.config;

import com.kxhospital.wreport.common.LoginUser;
import com.kxhospital.wreport.common.UserContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT 认证过滤器
 * 白名单：/api/auth/**、Swagger、actuator
 * 其余接口强制要求 Authorization: Bearer <token>
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {

        String path = req.getRequestURI();

        // ---- 白名单 ----
        if (path.startsWith("/api/auth/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/actuator")
                || path.equals("/")) {
            chain.doFilter(req, res);
            return;
        }

        // ---- 提取 Token ----
        String token = extractToken(req);
        if (token == null) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"code\":401,\"message\":\"未登录或 Token 已过期\"}");
            return;
        }

        // ---- 验证 Token ----
        Jws<Claims> jws;
        try {
            jws = jwtService.parse(token);
        } catch (Exception e) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"code\":401,\"message\":\"Token 无效或已过期\"}");
            return;
        }

        // ---- 写入用户上下文 ----
        LoginUser loginUser = jwtService.toLoginUser(jws.getBody());
        UserContext.set(req, loginUser);

        chain.doFilter(req, res);
    }

    private String extractToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header != null && !header.isEmpty()) {
            String trimmed = header.trim();
            if (trimmed.toLowerCase().startsWith("bearer ")) {
                return trimmed.substring(7).trim();
            }
            return trimmed;
        }
        // 兼容 ?token= 参数
        String param = req.getParameter("token");
        return (param != null && !param.isEmpty()) ? param : null;
    }
}
