package com.kxhospital.wreport.config;

import com.kxhospital.wreport.common.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String ROLE_CODES_CLAIM = "roleCodes";

    private final JwtProperties jwtProperties;

    public String generateToken(LoginUser user) {
        Instant now = Instant.now();
        String roleCodesClaim = encodeRoleCodes(user.getRoleCodes());
        return Jwts.builder()
                .setSubject(String.valueOf(user.getUserId()))
                .claim("account",  user.getAccount())
                .claim("realName", user.getRealName())
                .claim("orgId",    user.getOrgId() != null ? String.valueOf(user.getOrgId()) : null)
                .claim("orgName",  user.getOrgName())
                .claim("roleCode", user.getRoleCode())
                .claim(ROLE_CODES_CLAIM, roleCodesClaim)
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
        List<String> roleCodes = decodeRoleCodes(body.get(ROLE_CODES_CLAIM, String.class));
        if (roleCodes.isEmpty() && StringUtils.hasText(u.getRoleCode())) {
            roleCodes = Collections.singletonList(u.getRoleCode());
        }
        u.setRoleCodes(roleCodes);
        return u;
    }

    private String encodeRoleCodes(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return "";
        }
        return roleCodes.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(","));
    }

    private List<String> decodeRoleCodes(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private Key key() {
        byte[] bytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("JWT secret 长度不足 32 字节");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
