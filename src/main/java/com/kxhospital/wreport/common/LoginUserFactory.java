package com.kxhospital.wreport.common;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 登录用户构建辅助 */
public final class LoginUserFactory {

    private static final Set<String> ADMIN_ROLE_CODES = new HashSet<>(
            Arrays.asList("deptAdmin", "superAdmin"));

    private LoginUserFactory() {
    }

    public static LoginUser fromUserRow(java.util.Map<String, Object> user, List<String> roleCodes) {
        List<String> codes = normalizeRoleCodes(roleCodes);
        Long userId = toLong(user.get("id"));
        Long orgId = toLong(user.get("org_id"));
        String orgName = user.get("org_name") != null ? String.valueOf(user.get("org_name")) : "";

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userId);
        loginUser.setAccount((String) user.get("account"));
        loginUser.setRealName((String) user.get("real_name"));
        loginUser.setOrgId(orgId);
        loginUser.setOrgName(orgName);
        loginUser.setRoleCodes(codes);
        loginUser.setRoleCode(resolvePrimaryRoleCode(codes));
        return loginUser;
    }

    private static List<String> normalizeRoleCodes(List<String> roleCodes) {
        if (CollectionUtils.isEmpty(roleCodes)) {
            return Collections.singletonList("qcUser");
        }
        List<String> codes = new ArrayList<>();
        for (String code : roleCodes) {
            if (StringUtils.hasText(code) && !codes.contains(code.trim())) {
                codes.add(code.trim());
            }
        }
        return codes.isEmpty() ? Collections.singletonList("qcUser") : codes;
    }

    /** 前端主角色：管理员优先，其次填报角色，否则取第一个 */
    private static String resolvePrimaryRoleCode(List<String> roleCodes) {
        for (String code : roleCodes) {
            if (ADMIN_ROLE_CODES.contains(code)) {
                return code;
            }
        }
        for (String code : roleCodes) {
            if (RoleCodes.isOrgFillRole(code)) {
                return code;
            }
        }
        return roleCodes.get(0);
    }

    private static Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Long) return (Long) val;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }
}
