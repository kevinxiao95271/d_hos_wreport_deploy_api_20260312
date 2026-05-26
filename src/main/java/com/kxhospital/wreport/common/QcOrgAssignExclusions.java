package com.kxhospital.wreport.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务「分配机构」选择列表排除规则（机构名称关键字黑名单）。
 * 名称包含下列关键字的机构不出现在可选列表，保存 scope 时也会自动剔除。
 */
public final class QcOrgAssignExclusions {

    private QcOrgAssignExclusions() {}

    /** 机构名称包含以下任一关键字则排除（如：医疗废物相关质控中心） */
    public static final List<String> EXCLUDED_NAME_KEYWORDS = Collections.unmodifiableList(
            Arrays.asList("医疗废物")
    );

    public static boolean isExcludedOrgName(String orgName) {
        if (orgName == null || orgName.trim().isEmpty()) {
            return false;
        }
        for (String keyword : EXCLUDED_NAME_KEYWORDS) {
            if (keyword != null && !keyword.isEmpty() && orgName.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public static List<Map<String, Object>> filterAssignableOrgs(List<Map<String, Object>> orgs) {
        if (orgs == null || orgs.isEmpty()) {
            return orgs;
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> org : orgs) {
            Object nameObj = org.get("orgName");
            if (nameObj == null) {
                nameObj = org.get("orgname");
            }
            if (!isExcludedOrgName(nameObj != null ? String.valueOf(nameObj) : null)) {
                out.add(org);
            }
        }
        return out;
    }

    public static List<Long> filterAssignableOrgIds(List<Long> orgIds, List<Map<String, Object>> allQcOrgs) {
        if (orgIds == null || orgIds.isEmpty()) {
            return orgIds;
        }
        java.util.Set<Long> excludedIds = allQcOrgs.stream()
                .filter(o -> {
                    Object nameObj = o.get("orgName");
                    if (nameObj == null) nameObj = o.get("orgname");
                    return isExcludedOrgName(nameObj != null ? String.valueOf(nameObj) : null);
                })
                .map(o -> {
                    Object idObj = o.get("orgId");
                    if (idObj == null) idObj = o.get("orgid");
                    return idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(String.valueOf(idObj));
                })
                .collect(Collectors.toSet());
        if (excludedIds.isEmpty()) {
            return orgIds;
        }
        return orgIds.stream()
                .filter(id -> id != null && !excludedIds.contains(id))
                .collect(Collectors.toList());
    }
}
