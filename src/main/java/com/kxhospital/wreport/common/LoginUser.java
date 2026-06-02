package com.kxhospital.wreport.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JWT 解析后的当前登录用户信息
 * 通过 JwtFilter 写入 request attribute，Controller 中通过 UserContext.get() 获取
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    private static final Set<String> ADMIN_ROLE_CODES = new HashSet<>(
            Arrays.asList("deptAdmin", "superAdmin"));
    /** 可填报机构端：质控中心负责人、机构用户 */
    private static final Set<String> ORG_FILL_ROLE_CODES = new HashSet<>(
            Arrays.asList("zkcwfzr", "qcUser"));

    private Long   userId;
    private String account;
    private String realName;
    private Long   orgId;
    private String orgName;
    /** 主角色（兼容前端展示/旧 Token），多角色时按管理员 > 填报角色 > 首个角色 选取 */
    private String roleCode;
    /** 用户拥有的全部角色码 */
    private List<String> roleCodes = Collections.emptyList();

    public boolean hasRole(String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        if (roleCodes != null && !roleCodes.isEmpty()) {
            return roleCodes.contains(code);
        }
        return code.equals(roleCode);
    }

    public boolean isAdmin() {
        if (roleCodes != null && !roleCodes.isEmpty()) {
            return roleCodes.stream().anyMatch(ADMIN_ROLE_CODES::contains);
        }
        return ADMIN_ROLE_CODES.contains(roleCode);
    }

    /** 是否具备机构端填报权限（角色列表包含 zkcwfzr 或 qcUser 即可） */
    public boolean isOrgUser() {
        if (roleCodes != null && !roleCodes.isEmpty()) {
            return roleCodes.stream().anyMatch(ORG_FILL_ROLE_CODES::contains);
        }
        return ORG_FILL_ROLE_CODES.contains(roleCode);
    }
}
