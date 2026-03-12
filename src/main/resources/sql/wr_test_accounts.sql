-- =====================================================
-- 日常工作管理模块 - 测试账号创建脚本
--
-- 使用前: 先运行 WrTestAccountGen.main() 获取三个账号的密码哈希，
--         将下方 <HASH_ADMIN> / <HASH_ORG_A> / <HASH_ORG_B> 替换为实际值。
--
-- 账号说明:
--   admin_dept  / Admin@2025  / deptAdmin  → 省卫健委质控管理处
--   org_a_user  / OrgA@2025   / qcUser     → 超声质控中心
--   org_b_user  / OrgB@2025   / qcUser     → 日间手术技术指导中心
--
-- ID 规划:
--   机构(sys_org/sys_organization): 8000000000000000001 ~ 8000000000000000003
--   用户(sys_user):                 8000000000000010001 ~ 8000000000000010003
-- =====================================================

SET search_path TO zjylzl;

-- =====================================================
-- 一、机构数据（若已有机构则跳过，按实际调整）
-- =====================================================

-- 主管单位（deptAdmin 所属）
INSERT INTO sys_organization (id, org_name, org_code, status, create_time)
VALUES (8000000000000000001, '省卫健委质控管理处', 'ORG_DEPT_001', 1, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 超声质控中心（org_a 所属）
INSERT INTO sys_organization (id, org_name, org_code, status, create_time)
VALUES (8000000000000000002, '超声质控中心', 'ORG_QC_001', 1, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 日间手术技术指导中心（org_b 所属）
INSERT INTO sys_organization (id, org_name, org_code, status, create_time)
VALUES (8000000000000000003, '日间手术技术指导中心', 'ORG_QC_002', 1, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 二、用户数据
-- 密码字段填入 WrTestAccountGen 生成的 BCrypt 哈希
-- =====================================================

-- admin_dept: 系统管理员/科室管理员
INSERT INTO sys_user (id, account, password, real_name, org_id, status, del_flag, create_time)
VALUES (
    8000000000000010001,
    'admin_dept',
    '<HASH_ADMIN>',   -- 替换为 Admin@2025 的 BCrypt 哈希
    '测试管理员',
    8000000000000000001,
    1,
    0,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- org_a_user: 超声质控中心机构用户
INSERT INTO sys_user (id, account, password, real_name, org_id, status, del_flag, create_time)
VALUES (
    8000000000000010002,
    'org_a_user',
    '<HASH_ORG_A>',   -- 替换为 OrgA@2025 的 BCrypt 哈希
    '超声质控中心-联络员',
    8000000000000000002,
    1,
    0,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- org_b_user: 日间手术技术指导中心机构用户
INSERT INTO sys_user (id, account, password, real_name, org_id, status, del_flag, create_time)
VALUES (
    8000000000000010003,
    'org_b_user',
    '<HASH_ORG_B>',   -- 替换为 OrgB@2025 的 BCrypt 哈希
    '日间手术中心-联络员',
    8000000000000000003,
    1,
    0,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 三、用户-角色关联
-- =====================================================

INSERT INTO sys_user_role (id, user_id, role_id, create_time)
SELECT
    (8100000000000000000 + ROW_NUMBER() OVER ()) AS id,
    u.id AS user_id,
    r.id AS role_id,
    CURRENT_TIMESTAMP
FROM sys_user u
JOIN sys_role r ON r.role_code = 'deptAdmin'
WHERE u.account = 'admin_dept'
ON CONFLICT DO NOTHING;

INSERT INTO sys_user_role (id, user_id, role_id, create_time)
SELECT
    (8100000000000001000 + ROW_NUMBER() OVER ()) AS id,
    u.id AS user_id,
    r.id AS role_id,
    CURRENT_TIMESTAMP
FROM sys_user u
JOIN sys_role r ON r.role_code = 'qcUser'
WHERE u.account IN ('org_a_user', 'org_b_user')
ON CONFLICT DO NOTHING;
