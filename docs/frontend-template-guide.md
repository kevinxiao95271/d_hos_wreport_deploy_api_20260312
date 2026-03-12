# 模板接口前端接入指引

> 测试环境：`http://localhost:8083`  
> 所有接口需在请求头携带：`Authorization: <token>`（登录后获取）  
> Long 型 ID 以**字符串**返回，前端务必用 `String` 或 `BigInt` 处理

---

## 一、模板类型说明

本系统支持两类模板，前端渲染逻辑**完全不同**：

| 类型 | 特征 | 代表附件 |
|------|------|----------|
| **标准多级表头** | 行由填报人自由新增，列固定（1~3级表头），单元格填文本/数字 | 附件2 |
| **勾选矩阵** | 行/列均固定，行标签由后端配置，单元格只填 `"1"` 或 `"0"` | 附件3 |

**区分方式**：检查列定义中是否存在 `valueType === 'checkbox'` 的列。  
若存在，则按勾选矩阵渲染；否则按标准多级表头渲染。

---

## 二、核心接口：模板完整详情

### `GET /wr/template/detail/full/{templateId}`

**用途**：一次性获取模板的基本信息 + 列定义 + 行定义（矩阵类专用），是前端渲染表格的**唯一入口**。

**响应结构**：

```json
{
  "code": 200,
  "data": {
    "template": {
      "id": "2000000000000001",
      "templateName": "附件2：年度质控工作开展情况统计表",
      "description": "...",
      "status": 1
    },
    "items": [ /* 列定义，见下方说明 */ ],
    "rows":  [ /* 行定义，仅矩阵类模板有值，标准类为 [] */ ]
  }
}
```

---

## 三、列定义（`items`）详解

每个 `item` 代表一个表头节点：

```jsonc
{
  "id": "123456789",          // 列ID（Long as String），填报时作为 itemId
  "templateId": "2000000000000001",
  "parentId": null,           // null = 顶层节点；非null = 父节点ID
  "itemName": "质控工作会议",  // 显示名称
  "headerRow": 1,             // 所在表头行：1=第一层, 2=第二层, 3=第三层
  "colIndex": 4,              // 列序号（从1开始）
  "rowSpan": 1,               // 表头单元格行跨度
  "colSpan": 8,               // 表头单元格列跨度
  "isLeaf": 0,                // 0=非叶（分组头），1=叶（实际填报列）
  "valueType": "text",        // "text" | "number" | "checkbox"
  "unit": null,               // 单位（如"万元"、"%"）
  "placeholder": null,        // 输入提示
  "formatTemplateUrl": null   // 格式模板下载链接
}
```

### 3.1 三级表头树的构建

`items` 是**扁平数组**，前端需用 `parentId` 自行构建树：

```javascript
function buildTree(items) {
  const map = {};
  items.forEach(item => map[item.id] = { ...item, children: [] });
  const roots = [];
  items.forEach(item => {
    if (item.parentId) {
      map[item.parentId]?.children.push(map[item.id]);
    } else {
      roots.push(map[item.id]);
    }
  });
  return roots;
}

// 仅取叶子节点作为实际填报列
const leafItems = items.filter(item => item.isLeaf === 1);
```

### 3.2 表头渲染规则（对应 HTML `<th>` 的 rowspan / colspan）

- `rowSpan`：该表头单元格占几行
- `colSpan`：该表头单元格占几列
- `headerRow`：决定放在第几行 `<tr>` 里

**附件2 示例（三级，48个叶子列）：**

```
第1行: [国家质控 colspan=3] [质控工作会议 colspan=8] [质控培训 colspan=4] ...
第2行: [是否国家级 rowspan=2] [考核情况 colspan=2] [核心委员会议 colspan=4] ...
第3行:                        [2024年] [2025年]     [线上次数][参会人数][线下次数][参会人数] ...
```

---

## 四、行定义（`rows`）详解 — 矩阵类专用

**仅 `valueType='checkbox'` 类模板使用**，标准模板 `rows` 为空数组。

每个 `row` 代表一行标签：

```jsonc
{
  "id": "987654321",
  "templateId": "2000000000000002",
  "rowIndex": 16,              // 对应 wr_record_value.row_index，填报时必须传此值
  "rowLabel": "杭州市",        // 行显示名称
  "rowLevel": 2,               // 层级：1=省级汇总行, 2=市级, 3=县/区级
  "parentRowIndex": null,      // 父行的rowIndex（县/区级有值，市级为null）
  "sortNum": 15                // 排序序号
}
```

### 4.1 层级缩进渲染

```javascript
const levelIndent = { 1: 0, 2: 16, 3: 32 }; // px 缩进

rows.forEach(row => {
  const indent = levelIndent[row.rowLevel] || 0;
  // rowLevel=1: 加粗，背景色区分（省级汇总行）
  // rowLevel=2: 正常（市级）
  // rowLevel=3: 缩进显示（县/区级）
});
```

### 4.2 附件3 表格结构示意

```
              | 防盲中心 | 医院管理中心 | 核医学中心 | ... (26列)
─────────────────────────────────────────────────────────
省市县全部成立  |   ☑    |     ☐      |    ☑      |
市级全部成立   |   ☑    |     ☑      |    ☐      |
市级成立个数   |        |            |           |  ← 省级汇总行（rowLevel=1）
  杭州市级    |   ☑    |     ☐      |    ☑      |  ← 市级（rowLevel=2, 缩进16px）
  宁波市级    |   ☐    |     ☑      |    ☑      |
  ...
县级成立个数   |        |            |           |
  杭州市      |        |            |           |  ← 市级，下面是县（rowLevel=2）
    上城区    |   ☑    |     ☐      |    ☐      |  ← 县级（rowLevel=3, 缩进32px）
    拱墅区    |   ☐    |     ☐      |    ☑      |
```

---

## 五、填报接口（勾选矩阵）

### 保存草稿：`POST /wr/record/save`

矩阵类模板按 `(itemId, rowIndex)` 逐格传值，`value` 填 `"1"`（已成立）或 `"0"`（未成立）：

```json
{
  "taskId": "3000000000000002",
  "rows": [
    {
      "rowIndex": 1,
      "cells": [
        { "itemId": "列ID_防盲", "value": "1" },
        { "itemId": "列ID_医院管理", "value": "0" },
        { "itemId": "列ID_核医学", "value": "1" }
      ]
    },
    {
      "rowIndex": 4,
      "cells": [
        { "itemId": "列ID_防盲", "value": "1" }
      ]
    }
  ]
}
```

> 💡 **只需传有值的格**，未传的格视为"未填"（不存在 ≈ 未成立）。  
> 接口幂等：同一 `(taskId, orgId)` 重复调用自动覆盖。

### 读取已保存数据：`GET /wr/record/detail?recordId=xxx`

响应中 `values` 数组即已填数据，前端按 `(itemId, rowIndex)` 回填：

```json
{
  "data": {
    "record": { "id": "...", "status": 0 },
    "values": [
      { "itemId": "列ID_防盲", "rowIndex": 1, "cellValue": "1" },
      { "itemId": "列ID_核医学", "rowIndex": 4, "cellValue": "1" }
    ],
    "attachments": [],
    "rows": []
  }
}
```

```javascript
// 快速构建 value map
const valueMap = {};
values.forEach(v => {
  valueMap[`${v.itemId}_${v.rowIndex}`] = v.cellValue;
});

// 渲染时取值
const checked = valueMap[`${item.id}_${row.rowIndex}`] === '1';
```

---

## 六、填报接口（标准多级表头 - 附件2）

附件2 每个机构填**一行**（`rowIndex=1`），所有叶子列逐一传值：

```json
{
  "taskId": "3000000000000001",
  "rows": [
    {
      "rowIndex": 1,
      "cells": [
        { "itemId": "是否为国家级质控中心的itemId", "value": "是" },
        { "itemId": "2024年考核情况的itemId", "value": "优秀" },
        { "itemId": "2025年考核情况的itemId", "value": "良好" },
        { "itemId": "线上次数的itemId", "value": "3" },
        { "itemId": "财政拨付金额的itemId", "value": "120.5" }
      ]
    }
  ]
}
```

---

## 七、其他模板接口速查

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| `/wr/template/list` | GET | 启用中的模板列表（不分页） | Admin |
| `/wr/template/page` | GET | 模板分页列表 | Admin |
| `/wr/template/detail/{id}` | GET | 模板基本信息 | Admin |
| `/wr/template/detail/full/{id}` | GET | **完整详情（列+行）** | 全部 |
| `/wr/template/items/{templateId}` | GET | 仅列定义 | 全部 |
| `/wr/template/rows/{templateId}` | GET | **仅行定义（矩阵专用）** | 全部 |
| `/wr/template/items/save/{templateId}` | POST | 覆盖保存列定义 | Admin |
| `/wr/template/rows/save/{templateId}` | POST | **覆盖保存行定义** | Admin |
| `/wr/template/add` | POST | 新建模板 | Admin |
| `/wr/template/status/{id}/{status}` | POST | 启用/停用（1/0） | Admin |

---

## 八、前端推荐流程

```
进入填报页
  └─ GET /wr/task/active                   → 获取进行中任务列表
  └─ 用户选择任务
  └─ GET /wr/template/detail/full/{tplId} → 拿到 items + rows
  └─ 判断 isMatrix = items.some(x => x.valueType === 'checkbox')
  └─ GET /wr/record/detail（如已有草稿）    → 回填 valueMap

  ┌─ isMatrix=true（附件3）
  │   渲染勾选矩阵，行头用 rows，列头用 items
  │   点击格 → 切换 valueMap[`${itemId}_${rowIndex}`]
  │
  └─ isMatrix=false（附件2）
      渲染三级表头，buildTree(items)
      自由新增行（rowIndex 自增）
      每格输入对应 valueType 的控件

保存草稿
  └─ POST /wr/record/save   （可反复调用，幂等）

提交
  └─ POST /wr/record/submit
```

---

## 九、当前测试数据

| 项目 | ID | 说明 |
|------|----|------|
| 附件2模板 | `2000000000000001` | 三级表头，48个叶子列 |
| 附件3模板 | `2000000000000002` | 26列checkbox，116行 |
| 附件2任务 | `3000000000000001` | deadline 2026-05-31 |
| 附件3任务 | `3000000000000002` | deadline 2026-05-31 |
| 测试管理员 | `wr_admin / Admin@2025` | deptAdmin 角色 |
| 测试机构A | `wr_org_a / OrgA@2025` | qcUser 角色 |
| 测试机构B | `wr_org_b / OrgB@2025` | qcUser 角色 |
