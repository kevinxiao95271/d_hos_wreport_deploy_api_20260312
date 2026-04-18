-- =====================================================
-- 日常工作（DW）模块 - 测试账号初始化脚本
--
-- 匹配 SysUserMapper 实际表结构：
--   sys_user: user_id / account / password / real_name / person_id / del_flag='N' / status_flag=1
--   hr_person: person_id / org_id / del_flag='N'
--   hr_organization: org_id / org_name
--   sys_user_role: user_id / role_id
--   sys_role: role_id / role_code
--
-- 账号说明（三个现成可用账号）：
--   dw_admin    / DwAdmin@2025  / deptAdmin  → 管理员，可审核/配置/查看全部机构
--   dw_org_a    / DwOrgA@2025   / qcUser     → 超声质控中心（机构A，填报用）
--   dw_org_b    / DwOrgB@2025   / qcUser     → 日间手术技术指导中心（机构B，填报用）
--
-- ID 规划（高位前缀 8900 避免与现有数据冲突）：
--   hr_organization: 8900000000000000001 ~ 003
--   hr_person:       8900000000000010001 ~ 003
--   sys_user:        8900000000000020001 ~ 003
--
-- 密码哈希（BCrypt $2b$10$，与 Spring BCryptPasswordEncoder 完全兼容）：
--   DwAdmin@2025 => $2b$10$LXjYQ1ZGFzZjDnubOlU4zeJUz4j2aPmY3CGxO33o/N5WBTwaUHwBO
--   DwOrgA@2025  => $2b$10$L9ErmnDf2tRYN4IMiu5wG.pagrZWias/cw3XQpyCEPuGVIlsNOY5O
--   DwOrgB@2025  => $2b$10$7uOTYe0aQ5MfdGviHWZPyeDAbJcQe03GTWXgANuPRqdEvS2SItDgq
--
-- 执行方式：
--   psql -h 119.167.165.27 -U postgres -d zjylzl -f wr_dw_test_accounts.sql
--   或在 DBeaver / pgAdmin 中选择 schema=zjylzl 后执行
-- =====================================================

SET search_path TO zjylzl;

-- =====================================================
-- 一、机构（hr_organization）
-- =====================================================

INSERT INTO hr_organization (org_id, org_name)
VALUES
    (8900000000000000001, '省卫健委质控管理处（测试）'),
    (8900000000000000002, '超声质控中心（测试）'),
    (8900000000000000003, '日间手术技术指导中心（测试）')
ON CONFLICT (org_id) DO UPDATE SET org_name = EXCLUDED.org_name;

-- =====================================================
-- 二、人员档案（hr_person）
-- =====================================================

INSERT INTO hr_person (person_id, org_id, del_flag)
VALUES
    (8900000000000010001, 8900000000000000001, 'N'),
    (8900000000000010002, 8900000000000000002, 'N'),
    (8900000000000010003, 8900000000000000003, 'N')
ON CONFLICT (person_id) DO UPDATE SET
    org_id   = EXCLUDED.org_id,
    del_flag = 'N';

-- =====================================================
-- 三、系统用户（sys_user）
-- =====================================================

INSERT INTO sys_user (user_id, account, password, real_name, person_id, del_flag, status_flag)
VALUES
    (8900000000000020001,
     'dw_admin',
     '$2b$10$LXjYQ1ZGFzZjDnubOlU4zeJUz4j2aPmY3CGxO33o/N5WBTwaUHwBO',
     'DW测试管理员',
     8900000000000010001,
     'N', 1),
    (8900000000000020002,
     'dw_org_a',
     '$2b$10$L9ErmnDf2tRYN4IMiu5wG.pagrZWias/cw3XQpyCEPuGVIlsNOY5O',
     '超声质控-联络员',
     8900000000000010002,
     'N', 1),
    (8900000000000020003,
     'dw_org_b',
     '$2b$10$7uOTYe0aQ5MfdGviHWZPyeDAbJcQe03GTWXgANuPRqdEvS2SItDgq',
     '日间手术-联络员',
     8900000000000010003,
     'N', 1)
ON CONFLICT (user_id) DO UPDATE SET
    account     = EXCLUDED.account,
    password    = EXCLUDED.password,
    real_name   = EXCLUDED.real_name,
    person_id   = EXCLUDED.person_id,
    del_flag    = 'N',
    status_flag = 1;

-- =====================================================
-- 四、用户-角色关联（sys_user_role）
-- 通过子查询动态获取 role_id，无需硬编码
-- =====================================================

-- dw_admin → deptAdmin
INSERT INTO sys_user_role (user_id, role_id)
SELECT 8900000000000020001, role_id
FROM sys_role WHERE role_code = 'deptAdmin'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- dw_org_a → qcUser
INSERT INTO sys_user_role (user_id, role_id)
SELECT 8900000000000020002, role_id
FROM sys_role WHERE role_code = 'qcUser'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- dw_org_b → qcUser
INSERT INTO sys_user_role (user_id, role_id)
SELECT 8900000000000020003, role_id
FROM sys_role WHERE role_code = 'qcUser'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- =====================================================
-- 五、验证（执行后运行以下查询确认）
-- =====================================================
-- SELECT u.user_id, u.account, u.real_name, r.role_code, o.org_name
-- FROM sys_user u
-- JOIN hr_person p ON p.person_id = u.person_id
-- JOIN hr_organization o ON o.org_id = p.org_id
-- LEFT JOIN sys_user_role ur ON ur.user_id = u.user_id
-- LEFT JOIN sys_role r ON r.role_id = ur.role_id
-- WHERE u.account IN ('dw_admin', 'dw_org_a', 'dw_org_b');
