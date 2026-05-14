package com.kxhospital.wreport.controller;

import com.kxhospital.wreport.common.LoginUser;
import com.kxhospital.wreport.common.R;
import com.kxhospital.wreport.config.JwtService;
import com.kxhospital.wreport.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证接口（白名单，无需 Token）
 * POST /api/auth/login
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserMapper  sysUserMapper;
    private final JwtService     jwtService;

    @Value("${wr.sso.a-system-url:http://localhost:8080}")
    private String aSystemUrl;

    @Value("${wr.sso.app-secret:wr-sso-secret-2025}")
    private String ssoAppSecret;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    /**
     * 账号密码登录（明文密码，不需要验证码）
     */
    @Operation(summary = "登录", description = "账号+明文密码，返回 JWT Token（无需验证码）")
    @PostMapping("/login")
    public R<?> login(@Valid @RequestBody LoginRequest req) {
        // 1. 查用户（同时通过 person_id → hr_person → hr_organization 取出 org_id / org_name）
        Map<String, Object> user = sysUserMapper.findByAccount(req.getAccount());
        if (user == null) {
            return R.fail(401, "账号不存在或已禁用");
        }

        // 2. 校验密码（BCrypt）
        String storedHash = (String) user.get("password");
        if (storedHash == null || !ENCODER.matches(req.getPassword(), storedHash)) {
            return R.fail(401, "密码错误");
        }

        // 3. 查角色
        Long userId = toLong(user.get("id"));
        String roleCode = sysUserMapper.findRoleCode(userId);
        if (roleCode == null) roleCode = "qcUser";

        // 4. 直接取查询结果中的机构名
        Long orgId = toLong(user.get("org_id"));
        String orgName = user.get("org_name") != null ? (String) user.get("org_name") : "";

        // 5. 构建 LoginUser
        LoginUser loginUser = new LoginUser(
                userId,
                (String) user.get("account"),
                (String) user.get("real_name"),
                orgId,
                orgName,
                roleCode
        );

        // 6. 签发 Token
        String token = jwtService.generateToken(loginUser);

        // 7. 返回
        Map<String, Object> result = new HashMap<>();
        result.put("token",    "Bearer " + token);
        result.put("userId",   String.valueOf(loginUser.getUserId()));
        result.put("account",  loginUser.getAccount());
        result.put("realName", loginUser.getRealName());
        result.put("orgId",    loginUser.getOrgId() != null ? String.valueOf(loginUser.getOrgId()) : null);
        result.put("orgName",  loginUser.getOrgName());
        result.put("roleCode", loginUser.getRoleCode());
        return R.ok(result);
    }

    /**
     * SSO 单点登录：b 系统用 ticket 换取本系统 JWT
     * GET /api/auth/sso-login?ticket=xxx
     * 无需鉴权（白名单），由前端路由守卫调用
     */
    @Operation(summary = "SSO 单点登录", description = "用 a 系统颁发的 ticket 换取 b 系统 JWT")
    @GetMapping("/sso-login")
    public R<?> ssoLogin(@RequestParam("ticket") String ticket) {
        // 1. 调用 a 系统 verifyTicket 接口
        String verifyUrl = aSystemUrl + "/sso/verifyTicket?ticket=" + ticket + "&appSecret=" + ssoAppSecret;
        System.out.println("[SSO] verifyTicket URL: " + verifyUrl);
        Map<String, Object> aResp;
        try {
            RestTemplate rest = new RestTemplate();
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = rest.getForObject(verifyUrl, Map.class);
            System.out.println("[SSO] verifyTicket raw response: " + raw);
            if (raw == null || !Boolean.TRUE.equals(raw.get("success"))) {
                System.out.println("[SSO] ticket 无效，success=" + (raw == null ? "null" : raw.get("success")));
                return R.fail(401, "ticket 无效或已过期");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) raw.get("data");
            aResp = data;
            System.out.println("[SSO] 用户信息: " + aResp);
        } catch (Exception e) {
            System.out.println("[SSO] verifyTicket 异常: " + e.getMessage());
            return R.fail(401, "ticket 验证失败: " + e.getMessage());
        }

        // 2. 从 a 系统返回的用户信息中取 account
        String account = (String) aResp.get("account");
        if (account == null) {
            System.out.println("[SSO] ticket 数据异常：缺少 account，aResp=" + aResp);
            return R.fail(401, "ticket 数据异常：缺少 account");
        }

        // 3. 在本地 sys_user 查用户（共享同一张表）
        Map<String, Object> user = sysUserMapper.findByAccount(account);
        System.out.println("[SSO] 本地查用户 account=" + account + " result=" + (user != null ? "found" : "not found"));
        if (user == null) {
            return R.fail(401, "本系统不存在该用户: " + account);
        }

        // 4. 查角色和机构名
        Long userId = toLong(user.get("id"));
        String roleCode = sysUserMapper.findRoleCode(userId);
        if (roleCode == null) roleCode = "qcUser";

        Long orgId = toLong(user.get("org_id"));
        String orgName = user.get("org_name") != null ? (String) user.get("org_name") : "";

        // 5. 构建 LoginUser 并签发 b 系统自己的 JWT
        LoginUser loginUser = new LoginUser(
                userId,
                (String) user.get("account"),
                (String) user.get("real_name"),
                orgId,
                orgName,
                roleCode
        );
        String token = jwtService.generateToken(loginUser);

        // 6. 返回
        Map<String, Object> result = new HashMap<>();
        result.put("token",    "Bearer " + token);
        result.put("userId",   String.valueOf(loginUser.getUserId()));
        result.put("account",  loginUser.getAccount());
        result.put("realName", loginUser.getRealName());
        result.put("orgId",    loginUser.getOrgId() != null ? String.valueOf(loginUser.getOrgId()) : null);
        result.put("orgName",  loginUser.getOrgName());
        result.put("roleCode", loginUser.getRoleCode());
        return R.ok(result);
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Long)    return (Long) val;
        if (val instanceof Number)  return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }


    @Data
    public static class LoginRequest {
        @NotBlank(message = "account 不能为空")
        private String account;
        @NotBlank(message = "password 不能为空")
        private String password;
    }
}
