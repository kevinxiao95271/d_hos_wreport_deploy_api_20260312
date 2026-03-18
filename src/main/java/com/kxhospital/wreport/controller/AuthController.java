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
 *
 * 机构信息取链路：
 *   sys_user.person_id → hr_person.person_id → hr_person.org_id → hr_organization.org_name
 * 不直接使用 sys_user.org_id。
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserMapper sysUserMapper;
    private final JwtService    jwtService;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

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

        // 4. 取机构信息（来自 findByAccount JOIN 结果，无需再单独查询）
        //    org_id  来自 hr_person.org_id
        //    orgName 来自 hr_organization.org_name
        Long   orgId   = toLong(user.get("org_id"));
        String orgName = user.get("org_name") != null ? (String) user.get("org_name") : "";

        // 5. 构建 LoginUser 并写入 JWT
        LoginUser loginUser = new LoginUser(
                userId,
                (String) user.get("account"),
                (String) user.get("real_name"),
                orgId,
                orgName,
                roleCode
        );

        String token = jwtService.generateToken(loginUser);

        // 6. 返回（接口格式不变，前端无需调整）
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
        if (val instanceof Long)   return (Long) val;
        if (val instanceof Number) return ((Number) val).longValue();
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
