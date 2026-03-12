package com.kxhospital.wreport.controller;

import com.kxhospital.wreport.common.LoginUser;
import com.kxhospital.wreport.common.R;
import com.kxhospital.wreport.config.JwtService;
import com.kxhospital.wreport.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;


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

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    /**
     * 账号密码登录（明文密码，不需要验证码）
     */
    @Operation(summary = "登录", description = "账号+明文密码，返回 JWT Token（无需验证码）")
    @PostMapping("/login")
    public R<?> login(@Valid @RequestBody LoginRequest req) {
        // 1. 查用户
        Map<String, Object> user = sysUserMapper.findByAccount(req.getAccount());
        if (user == null) {
            return R.fail(401, "E1:user_not_found:" + req.getAccount());
        }

        // 2. 校验密码（BCrypt）
        String storedHash = (String) user.get("password");
        if (storedHash == null || !ENCODER.matches(req.getPassword(), storedHash)) {
            return R.fail(401, "E2:password_mismatch:hash=" + (storedHash != null ? storedHash.substring(0, 10) : "null"));
        }

        // 3. 查角色
        Long userId = toLong(user.get("id"));
        String roleCode = sysUserMapper.findRoleCode(userId);
        if (roleCode == null) roleCode = "qcUser";

        // 4. 查机构名（忽略失败）
        Long orgId = toLong(user.get("org_id"));
        String orgName = "";
        if (orgId != null) {
            try {
                orgName = sysUserMapper.findOrgName(orgId);
                if (orgName == null) orgName = "";
            } catch (Exception ignored) {}
        }

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

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Long)    return (Long) val;
        if (val instanceof Number)  return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }

    /** 列出数据库所有表（调试用，不需要鉴权） */
    @GetMapping("/tables")
    public R<?> tables() {
        try {
            return R.ok(sysUserMapper.listTables());
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /** 查询 sys_user 列名（调试用）*/
    @GetMapping("/user-columns")
    public R<?> userColumns() {
        try {
            return R.ok(sysUserMapper.listUserColumns());
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /** 列出最近 20 个用户账号（调试用）*/
    @GetMapping("/users")
    public R<?> recentUsers() {
        try {
            return R.ok(sysUserMapper.listRecentUsers());
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 开发环境：重置三个测试账号密码
     * wr_admin → Admin@2025, wr_org_a → OrgA@2025, wr_org_b → OrgB@2025
     */
    @PostMapping("/dev-reset-pwd")
    public R<?> devResetPwd() {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        sysUserMapper.updatePassword("wr_admin", enc.encode("Admin@2025"));
        sysUserMapper.updatePassword("wr_org_a", enc.encode("OrgA@2025"));
        sysUserMapper.updatePassword("wr_org_b", enc.encode("OrgB@2025"));
        return R.ok("密码已重置: wr_admin=Admin@2025 / wr_org_a=OrgA@2025 / wr_org_b=OrgB@2025");
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "account 不能为空")
        private String account;
        @NotBlank(message = "password 不能为空")
        private String password;
    }
}
