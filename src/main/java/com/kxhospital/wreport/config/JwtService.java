package com.kxhospital.wreport.config;

import com.kxhospital.wreport.common.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    public String generateToken(LoginUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(String.valueOf(user.getUserId()))
                .claim("account",  user.getAccount())
                .claim("realName", user.getRealName())
                .claim("orgId",    user.getOrgId() != null ? String.valueOf(user.getOrgId()) : null)
                .claim("orgName",  user.getOrgName())
                .claim("roleCode", user.getRoleCode())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(jwtProperties.getExpireMinutes(), ChronoUnit.MINUTES)))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token);
    }

    /** 从已解析的 Claims 构建 LoginUser */
    public LoginUser toLoginUser(Claims body) {
        LoginUser u = new LoginUser();
        u.setUserId(Long.parseLong(body.getSubject()));
        u.setAccount(body.get("account", String.class));
        u.setRealName(body.get("realName", String.class));
        String orgIdStr = body.get("orgId", String.class);
        u.setOrgId(orgIdStr != null && !"null".equals(orgIdStr) ? Long.parseLong(orgIdStr) : null);
        u.setOrgName(body.get("orgName", String.class));
        u.setRoleCode(body.get("roleCode", String.class));
        return u;
    }

    private Key key() {
        byte[] bytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("JWT secret 长度不足 32 字节");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
