-- =====================================================
-- 日常工作管理模块 - 权限数据初始化
-- 向 sys_resource / sys_menu / sys_menu_button 插入记录
-- 并为 deptAdmin / qcUser / medicalUser 角色授权
--
-- 执行前提: wr_module_ddl.sql 已执行
-- 注意: ID 采用固定雪花段，避免冲突请根据实际环境调整前缀
-- =====================================================

SET search_path TO zjylzl;

-- =====================================================
-- 一、菜单（sys_menu）
-- =====================================================
-- 根据实际 sys_menu 表结构调整字段; 下方是常见 Roses/Guns 菜单字段
INSERT INTO sys_menu (id, menu_parent_id, menu_name, menu_code, menu_sort, menu_type, router, component, visible, status, create_time)
VALUES
-- 一级菜单：日常工作管理
(7100000000000000001, 0, '日常工作管理', 'work_report', 1, 0,
 '/workReport', 'Layout', 1, 1, CURRENT_TIMESTAMP),

-- 二级菜单（deptAdmin 可见）
(7100000000000000010, 7100000000000000001, '模板管理', 'wr_template',     1, 1,
 '/workReport/template', 'workReport/template/index', 1, 1, CURRENT_TIMESTAMP),
(7100000000000000020, 7100000000000000001, '任务管理', 'wr_task',         2, 1,
 '/workReport/task',     'workReport/task/index',     1, 1, CURRENT_TIMESTAMP),
(7100000000000000030, 7100000000000000001, '上报审阅', 'wr_review',       3, 1,
 '/workReport/review',   'workReport/review/index',   1, 1, CURRENT_TIMESTAMP),

-- 二级菜单（qcUser / medicalUser 可见）
(7100000000000000040, 7100000000000000001, '我的上报', 'wr_my_report',    4, 1,
 '/workReport/myReport', 'workReport/myReport/index', 1, 1, CURRENT_TIMESTAMP)

ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 二、菜单按钮（sys_menu_button）
-- =====================================================
INSERT INTO sys_menu_button (id, menu_id, button_name, button_code, create_time)
VALUES
-- 模板管理
(7200000000000000001, 7100000000000000010, '新增', 'wr:template:add',    CURRENT_TIMESTAMP),
(7200000000000000002, 7100000000000000010, '编辑', 'wr:template:edit',   CURRENT_TIMESTAMP),
(7200000000000000003, 7100000000000000010, '删除', 'wr:template:delete', CURRENT_TIMESTAMP),
(7200000000000000004, 7100000000000000010, '启用/停用', 'wr:template:updateStatus', CURRENT_TIMESTAMP),
(7200000000000000005, 7100000000000000010, '配置表头', 'wr:template:saveHeaders',   CURRENT_TIMESTAMP),

-- 任务管理
(7200000000000000011, 7100000000000000020, '新建任务',   'wr:task:add',          CURRENT_TIMESTAMP),
(7200000000000000012, 7100000000000000020, '编辑任务',   'wr:task:edit',         CURRENT_TIMESTAMP),
(7200000000000000013, 7100000000000000020, '删除任务',   'wr:task:delete',       CURRENT_TIMESTAMP),
(7200000000000000014, 7100000000000000020, '发布/结束',  'wr:task:updateStatus', CURRENT_TIMESTAMP),

-- 上报审阅
(7200000000000000021, 7100000000000000030, '审核',   'wr:record:audit',     CURRENT_TIMESTAMP),
(7200000000000000022, 7100000000000000030, '聚合审阅','wr:record:aggregate', CURRENT_TIMESTAMP),
(7200000000000000023, 7100000000000000030, '导出Excel','wr:record:export',   CURRENT_TIMESTAMP),

-- 我的上报
(7200000000000000031, 7100000000000000040, '保存草稿', 'wr:record:save',   CURRENT_TIMESTAMP),
(7200000000000000032, 7100000000000000040, '提交上报', 'wr:record:submit', CURRENT_TIMESTAMP),
(7200000000000000033, 7100000000000000040, '上传附件', 'wr:attach:upload', CURRENT_TIMESTAMP),
(7200000000000000034, 7100000000000000040, '删除附件', 'wr:attach:delete', CURRENT_TIMESTAMP)

ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 三、接口资源（sys_resource）
-- 字段参考 Roses kernel-s-resource 模块
-- resource_code 格式: 模块:操作  (与 @SaCheckPermission 一致)
-- =====================================================
INSERT INTO sys_resource (id, resource_name, resource_code, resource_url, http_method, view_flag, module_code, create_time)
VALUES
-- 模板管理
(7300000000000000001, '新增上报模板',        'wr:template:add',             '/wr/template/add',                'POST', 0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000002, '编辑上报模板',        'wr:template:edit',            '/wr/template/edit',               'POST', 0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000003, '删除上报模板',        'wr:template:delete',          '/wr/template/delete',             'POST', 0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000004, '启用/停用模板',       'wr:template:updateStatus',    '/wr/template/updateStatus',       'POST', 0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000005, '模板详情',            'wr:template:detail',          '/wr/template/detail',             'GET',  0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000006, '模板分页列表',        'wr:template:page',            '/wr/template/page',               'GET',  0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000007, '模板不分页列表',      'wr:template:list',            '/wr/template/list',               'GET',  0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000008, '模板表头树',          'wr:template:headerTree',      '/wr/template/headerTree',         'GET',  0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000009, '批量保存表头',        'wr:template:saveHeaders',     '/wr/template/saveHeaders',        'POST', 0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000010, '单节点精细编辑',      'wr:template:item:edit',       '/wr/template/item/edit',          'POST', 0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000011, '上传格式模板',        'wr:template:item:uploadFmt',  '/wr/template/item/uploadFormatTemplate',  'POST', 0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000012, '删除格式模板',        'wr:template:item:deleteFmt',  '/wr/template/item/deleteFormatTemplate',  'POST', 0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000013, '节点详情',            'wr:template:item:detail',     '/wr/template/item/detail',        'GET',  0, 'workreport', CURRENT_TIMESTAMP),

-- 任务管理
(7300000000000000021, '新建上报任务',        'wr:task:add',          '/wr/task/add',          'POST', 0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000022, '编辑任务',            'wr:task:edit',         '/wr/task/edit',         'POST', 0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000023, '删除任务',            'wr:task:delete',       '/wr/task/delete',       'POST', 0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000024, '发布/结束任务',       'wr:task:updateStatus', '/wr/task/updateStatus', 'POST', 0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000025, '任务详情',            'wr:task:detail',       '/wr/task/detail',       'GET',  0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000026, '任务分页列表',        'wr:task:page',         '/wr/task/page',         'GET',  0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000027, '进行中任务(机构端)',  'wr:task:activeTasks',  '/wr/task/activeTasks',  'GET',  0, 'workreport', CURRENT_TIMESTAMP),

-- 上报记录
(7300000000000000031, '管理端上报列表',      'wr:record:adminPage',  '/wr/record/adminPage',  'GET',  0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000032, '上报详情',            'wr:record:detail',     '/wr/record/detail',     'GET',  0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000033, '审核上报',            'wr:record:audit',      '/wr/record/audit',      'POST', 0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000034, '按字段聚合',          'wr:record:aggregate',  '/wr/record/aggregate',  'GET',  0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000035, '导出Excel',           'wr:record:export',     '/wr/record/export',     'GET',  0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000036, '保存草稿',            'wr:record:save',       '/wr/record/save',       'POST', 0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000037, '提交上报',            'wr:record:submit',     '/wr/record/submit',     'POST', 0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000038, '机构端上报列表',      'wr:record:orgPage',    '/wr/record/orgPage',    'GET',  0, 'workreport', CURRENT_TIMESTAMP),

-- 附件管理
(7300000000000000041, '上传佐证附件',        'wr:attach:upload',     '/wr/attachment/upload', 'POST', 0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000042, '删除附件',            'wr:attach:delete',     '/wr/attachment/delete', 'POST', 0, 'workreport', CURRENT_TIMESTAMP),
(7300000000000000043, '附件列表',            'wr:attach:list',       '/wr/attachment/list',   'GET',  0, 'workreport', CURRENT_TIMESTAMP)

ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 四、菜单-资源关联 (sys_menu_resource)
-- =====================================================
-- 模板管理菜单关联其所有接口资源
INSERT INTO sys_menu_resource (id, menu_id, resource_id, create_time) VALUES
(7400000000000000001, 7100000000000000010, 7300000000000000001, CURRENT_TIMESTAMP),
(7400000000000000002, 7100000000000000010, 7300000000000000002, CURRENT_TIMESTAMP),
(7400000000000000003, 7100000000000000010, 7300000000000000003, CURRENT_TIMESTAMP),
(7400000000000000004, 7100000000000000010, 7300000000000000004, CURRENT_TIMESTAMP),
(7400000000000000005, 7100000000000000010, 7300000000000000005, CURRENT_TIMESTAMP),
(7400000000000000006, 7100000000000000010, 7300000000000000006, CURRENT_TIMESTAMP),
(7400000000000000007, 7100000000000000010, 7300000000000000007, CURRENT_TIMESTAMP),
(7400000000000000008, 7100000000000000010, 7300000000000000008, CURRENT_TIMESTAMP),
(7400000000000000009, 7100000000000000010, 7300000000000000009, CURRENT_TIMESTAMP),
(7400000000000000010, 7100000000000000010, 7300000000000000010, CURRENT_TIMESTAMP),
(7400000000000000011, 7100000000000000010, 7300000000000000011, CURRENT_TIMESTAMP),
(7400000000000000012, 7100000000000000010, 7300000000000000012, CURRENT_TIMESTAMP),
(7400000000000000013, 7100000000000000010, 7300000000000000013, CURRENT_TIMESTAMP),
-- 任务管理
(7400000000000000021, 7100000000000000020, 7300000000000000021, CURRENT_TIMESTAMP),
(7400000000000000022, 7100000000000000020, 7300000000000000022, CURRENT_TIMESTAMP),
(7400000000000000023, 7100000000000000020, 7300000000000000023, CURRENT_TIMESTAMP),
(7400000000000000024, 7100000000000000020, 7300000000000000024, CURRENT_TIMESTAMP),
(7400000000000000025, 7100000000000000020, 7300000000000000025, CURRENT_TIMESTAMP),
(7400000000000000026, 7100000000000000020, 7300000000000000026, CURRENT_TIMESTAMP),
-- 上报审阅
(7400000000000000031, 7100000000000000030, 7300000000000000031, CURRENT_TIMESTAMP),
(7400000000000000032, 7100000000000000030, 7300000000000000032, CURRENT_TIMESTAMP),
(7400000000000000033, 7100000000000000030, 7300000000000000033, CURRENT_TIMESTAMP),
(7400000000000000034, 7100000000000000030, 7300000000000000034, CURRENT_TIMESTAMP),
(7400000000000000035, 7100000000000000030, 7300000000000000035, CURRENT_TIMESTAMP),
-- 我的上报
(7400000000000000041, 7100000000000000040, 7300000000000000027, CURRENT_TIMESTAMP),
(7400000000000000042, 7100000000000000040, 7300000000000000036, CURRENT_TIMESTAMP),
(7400000000000000043, 7100000000000000040, 7300000000000000037, CURRENT_TIMESTAMP),
(7400000000000000044, 7100000000000000040, 7300000000000000038, CURRENT_TIMESTAMP),
(7400000000000000045, 7100000000000000040, 7300000000000000032, CURRENT_TIMESTAMP),
(7400000000000000046, 7100000000000000040, 7300000000000000041, CURRENT_TIMESTAMP),
(7400000000000000047, 7100000000000000040, 7300000000000000042, CURRENT_TIMESTAMP),
(7400000000000000048, 7100000000000000040, 7300000000000000043, CURRENT_TIMESTAMP)

ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 五、角色-菜单授权 (sys_role_menu)
-- 查出已有角色 ID 后按实际替换; 下方用子查询动态获取
-- =====================================================

-- deptAdmin: 拥有所有4个菜单
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT
    (7500000000000000000 + ROW_NUMBER() OVER ()) AS id,
    r.id AS role_id,
    m.id AS menu_id,
    CURRENT_TIMESTAMP
FROM sys_role r
CROSS JOIN (
    VALUES
        (7100000000000000001::BIGINT),
        (7100000000000000010::BIGINT),
        (7100000000000000020::BIGINT),
        (7100000000000000030::BIGINT),
        (7100000000000000040::BIGINT)
) AS m(id)
WHERE r.role_code = 'deptAdmin'
ON CONFLICT DO NOTHING;

-- qcUser: 只有"日常工作管理"一级菜单 + "我的上报"
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT
    (7500000000000001000 + ROW_NUMBER() OVER ()) AS id,
    r.id AS role_id,
    m.id AS menu_id,
    CURRENT_TIMESTAMP
FROM sys_role r
CROSS JOIN (
    VALUES
        (7100000000000000001::BIGINT),
        (7100000000000000040::BIGINT)
) AS m(id)
WHERE r.role_code = 'qcUser'
ON CONFLICT DO NOTHING;

-- medicalUser: 同 qcUser
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT
    (7500000000000002000 + ROW_NUMBER() OVER ()) AS id,
    r.id AS role_id,
    m.id AS menu_id,
    CURRENT_TIMESTAMP
FROM sys_role r
CROSS JOIN (
    VALUES
        (7100000000000000001::BIGINT),
        (7100000000000000040::BIGINT)
) AS m(id)
WHERE r.role_code = 'medicalUser'
ON CONFLICT DO NOTHING;

-- =====================================================
-- 六、角色-资源授权 (sys_role_resource)
-- =====================================================

-- deptAdmin: 所有接口资源
INSERT INTO sys_role_resource (id, role_id, resource_id, create_time)
SELECT
    (7600000000000000000 + ROW_NUMBER() OVER ()) AS id,
    r.id AS role_id,
    res.id AS resource_id,
    CURRENT_TIMESTAMP
FROM sys_role r
CROSS JOIN sys_resource res
WHERE r.role_code = 'deptAdmin'
  AND res.resource_code LIKE 'wr:%'
ON CONFLICT DO NOTHING;

-- qcUser: 机构端接口资源
INSERT INTO sys_role_resource (id, role_id, resource_id, create_time)
SELECT
    (7600000000000001000 + ROW_NUMBER() OVER ()) AS id,
    r.id AS role_id,
    res.id AS resource_id,
    CURRENT_TIMESTAMP
FROM sys_role r
CROSS JOIN sys_resource res
WHERE r.role_code = 'qcUser'
  AND res.resource_code IN (
      'wr:template:headerTree',
      'wr:template:detail',
      'wr:template:item:detail',
      'wr:task:activeTasks',
      'wr:record:save',
      'wr:record:submit',
      'wr:record:orgPage',
      'wr:record:detail',
      'wr:attach:upload',
      'wr:attach:delete',
      'wr:attach:list'
  )
ON CONFLICT DO NOTHING;

-- medicalUser: 同 qcUser
INSERT INTO sys_role_resource (id, role_id, resource_id, create_time)
SELECT
    (7600000000000002000 + ROW_NUMBER() OVER ()) AS id,
    r.id AS role_id,
    res.id AS resource_id,
    CURRENT_TIMESTAMP
FROM sys_role r
CROSS JOIN sys_resource res
WHERE r.role_code = 'medicalUser'
  AND res.resource_code IN (
      'wr:template:headerTree',
      'wr:template:detail',
      'wr:template:item:detail',
      'wr:task:activeTasks',
      'wr:record:save',
      'wr:record:submit',
      'wr:record:orgPage',
      'wr:record:detail',
      'wr:attach:upload',
      'wr:attach:delete',
      'wr:attach:list'
  )
ON CONFLICT DO NOTHING;
