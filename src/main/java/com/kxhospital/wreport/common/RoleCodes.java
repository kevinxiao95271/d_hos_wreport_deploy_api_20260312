package com.kxhospital.wreport.common;

import org.springframework.util.StringUtils;

import java.util.List;

/** 角色码判定（与主平台 qc* 质控角色规则一致） */
public final class RoleCodes {

    private RoleCodes() {
    }

    public static boolean isQcRoleCode(String code) {
        if (!StringUtils.hasText(code)) {
            return false;
        }
        return code.trim().toLowerCase().startsWith("qc");
    }

    /** 机构端填报：质控常务副主任、技术指导中心用户，或任意 qc 前缀角色（qcUser、qcAdmin 等） */
    public static boolean isOrgFillRole(String code) {
        if (!StringUtils.hasText(code)) {
            return false;
        }
        String normalized = code.trim().toLowerCase();
        return "zkcwfzr".equals(normalized)
                || "jszdzx".equals(normalized)
                || isQcRoleCode(code);
    }

    public static boolean hasOrgFillRole(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return false;
        }
        return roleCodes.stream().anyMatch(RoleCodes::isOrgFillRole);
    }

    public static boolean hasOrgFillRole(String roleCode) {
        return isOrgFillRole(roleCode);
    }
}
