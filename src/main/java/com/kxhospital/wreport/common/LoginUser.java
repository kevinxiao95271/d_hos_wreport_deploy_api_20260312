package com.kxhospital.wreport.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JWT 解析后的当前登录用户信息
 * 通过 JwtFilter 写入 request attribute，Controller 中通过 UserContext.get() 获取
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    private Long   userId;
    private String account;
    private String realName;
    private Long   orgId;
    private String orgName;
    /** deptAdmin / qcUser / medicalUser / superAdmin / exp */
    private String roleCode;

    public boolean isAdmin() {
        return "deptAdmin".equals(roleCode) || "superAdmin".equals(roleCode);
    }

    public boolean isOrgUser() {
        return "qcUser".equals(roleCode) || "medicalUser".equals(roleCode);
    }
}
