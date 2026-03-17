# 前端开发指引

> 测试环境：`http://localhost:8083`  
> 所有接口需在请求头携带：`Authorization: Bearer <token>`  
> **Long 型 ID 以字符串返回，前端务必用 `String` 处理，禁止用 JS `number`**

---

## 一、认证

### 登录

```
POST /api/auth/login
Body: { "account": "wr_admin", "password": "Admin@2025" }
```

```json
{
  "code": 200,
  "data": {
    "token":    "Bearer eyJ...",
    "userId":   "177330561143451283",
    "account":  "wr_admin",
    "realName": "质控管理员",
    "orgName":  "浙江省",
    "orgId":    "1986677412049780738",
    "roleCode": "deptAdmin"
  }
}
```

**roleCode 说明：**

| roleCode | 身份 | 能做什么 |
|---|---|---|
| `deptAdmin` | 管理员 | 发布任务、配置模板、查看所有上报、导出 |
| `qcUser` | 机构用户 | 填报自己机构的数据 |

---

## 二、模板类型

系统有两类模板，前端渲染逻辑完全不同：

| 类型 | 判断方式 | 代表 |
|---|---|---|
| **标准多级表头** | `rows` 为空数组 | 附件2 |
| **勾选矩阵** | `rows` 不为空 | 附件3 |

---

## 三、获取模板完整结构

### `GET /wr/template/detail/full/{templateId}`

一次拿到列定义 + 行定义，是渲染表格的**唯一入口**。

```json
{
  "code": 200,
  "data": {
    "template": { "id": "...", "templateName": "附件2：...", "status": 1 },
    "items": [ /* 列定义数组，见下 */ ],
    "rows":  [ /* 行定义数组，矩阵类有值，标准类为 [] */ ]
  }
}
```

---

## 四、列定义（`items`）字段说明

```jsonc
{
  "id":          "2000000000100001",  // 列ID（填报时作为 itemId）
  "parentId":    null,                // null=顶层；非null=父节点ID
  "itemName":    "线上次数",
  "headerRow":   3,                   // 所在表头层级（1/2/3）
  "colIndex":    5,
  "rowSpan":     1,
  "colSpan":     1,
  "isLeaf":      1,                   // 1=叶子（实际填报列），0=分组表头
  "valueType":   "number",            // "text" | "number" | "checkbox"
  "dictCode":    null,                // 非null时=绑定了字典，前端渲染下拉
  "headerPath":  ["质控工作会议", "质控核心委员会议", "线上次数"],  // 从根到本节点的完整路径
  "unit":        null,
  "placeholder": null,
  "formatTemplateUrl": null           // 格式模板下载链接
}
```

### 4.1 headerPath — 填报标签

`headerPath` 是非持久化计算字段，**仅叶子节点有意义**。

填报页面渲染输入框时，用 `headerPath.join(" / ")` 作为字段标签，
避免同名列（如"线上次数"在2024和2025下都有）产生歧义：

```
2024年 / 质控核心委员会议 / 线上次数  [____]
2025年 / 质控核心委员会议 / 线上次数  [____]
```

### 4.2 dictCode — 下拉字典

当 `item.dictCode` 非空时，该字段渲染为**下拉选择框**：

```javascript
if (item.dictCode) {
  // 拉取选项列表
  // GET /wr/dict/items/{dictCode}  → [{ itemLabel: "是", itemValue: "1" }, ...]
  // 渲染 <Select>，存 itemValue，展示 itemLabel
} else {
  // 按 valueType 渲染普通输入框
}
```

**当前已有字典：**

| dictCode | 用途 | 选项 |
|---|---|---|
| `yes_no` | 是/否类字段 | 是=`1` / 否=`0` |
| `meeting_form` | 开展形式 | 线上=`online` / 线下=`offline` / 线上+线下=`both` |
| `task_status` | 完成情况 | 已完成=`done` / 进行中=`in_progress` / 未完成=`not_done` |

**附件2 已绑定字典的叶子节点：**

| 字段名 | dictCode |
|---|---|
| 是否为国家级质控中心 | `yes_no` |
| 是否有专职人员 | `yes_no` |
| 是否有专用场所 | `yes_no` |
| 是否需要跨专业协同 | `yes_no` |
| 开展形式（线上/线下） | `meeting_form` |
| 完成情况 | `task_status` |

### 4.3 构建多级表头树（附件2用）

```javascript
function buildTree(items) {
  const map = {};
  items.forEach(i => map[i.id] = { ...i, children: [] });
  const roots = [];
  items.forEach(i => {
    if (i.parentId) map[i.parentId]?.children.push(map[i.id]);
    else roots.push(map[i.id]);
  });
  return roots;
}
const leafItems = items.filter(i => i.isLeaf === 1);
```

---

## 五、行定义（`rows`）— 矩阵类专用

```jsonc
{
  "rowIndex":       16,       // 对应 wr_record_value.row_index，填报时传此值
  "rowLabel":       "杭州市",
  "rowLevel":       2,        // 1=省级汇总行, 2=市级, 3=县/区级
  "parentRowIndex": 1         // null=根节点，非null=父行rowIndex
}
```

### 5.1 附件3 行树结构

附件3 有**两棵独立的树**，互不联动：

```
树A — 省市县勾选
[1] 省市县全部成立   parentRowIndex=null  ← 树A根
  [16] 杭州市        parentRowIndex=1
    [17] 上城区      parentRowIndex=16
    [18] 拱墅区      parentRowIndex=16
    ...
  [30] 宁波市        parentRowIndex=1
    ...

树B — 市级勾选
[2] 市级全部成立     parentRowIndex=null  ← 树B根
  [4]  杭州市级      parentRowIndex=2
  [5]  宁波市级      parentRowIndex=2
  ...
  [14] 丽水市级      parentRowIndex=2
```

### 5.2 勾选联动算法

```javascript
// 按 parentRowIndex 建树
const rowMap = {};
rows.forEach(r => rowMap[r.rowIndex] = r);
const values = {}; // { rowIndex: "1"/"0" }

// 向下：勾选/取消父行 → 递归所有子行
function cascadeDown(rowIndex, checked) {
  values[rowIndex] = checked ? "1" : "0";
  rows.filter(r => r.parentRowIndex === rowIndex)
      .forEach(child => cascadeDown(child.rowIndex, checked));
}

// 向上：勾选子行后 → 检查父行是否满足全勾
function bubbleUp(rowIndex) {
  const row = rowMap[rowIndex];
  if (!row?.parentRowIndex) return;
  const siblings = rows.filter(r => r.parentRowIndex === row.parentRowIndex);
  const allChecked = siblings.every(s => values[s.rowIndex] === "1");
  values[row.parentRowIndex] = allChecked ? "1" : "0";
  bubbleUp(row.parentRowIndex);
}

// 用户点击某格
function onCellClick(rowIndex, itemId) {
  const newVal = values[`${rowIndex}`] === "1" ? "0" : "1";
  cascadeDown(rowIndex, newVal === "1");
  if (newVal === "1") bubbleUp(rowIndex);
  else {
    // 取消：父行也取消
    let cur = rowMap[rowIndex];
    while (cur?.parentRowIndex) {
      values[cur.parentRowIndex] = "0";
      cur = rowMap[cur.parentRowIndex];
    }
  }
}
```

**两棵树联动规则：**

| 操作 | 效果 |
|---|---|
| 勾选 Row1（省市县全部成立）| 所有市（16+）及其下属县区全勾 |
| 勾选 Row2（市级全部成立）| 所有市级（4-14）全勾，与树A无关 |
| 勾选某市（如杭州市）| 杭州所有县区勾选；11市全勾→Row1自动勾 |
| 勾选某县区 | 所属市下县区全勾→该市勾；11市全勾→Row1勾 |

---

## 六、附件3 的 items 结构

附件3 共 **3个 item**（不再有26个机构列）：

| id | itemName | valueType | 用途 |
|---|---|---|---|
| `2000000000100001` | 已成立 | `checkbox` | 矩阵勾选，配合所有行使用 |
| `2000000000100002` | 市级成立个数 | `number` | 独立数字输入框，不参与矩阵 |
| `2000000000100003` | 县级成立个数 | `number` | 独立数字输入框，不参与矩阵 |

渲染建议：先渲染两个独立数字输入框，再渲染矩阵表格。

---

## 七、保存填报数据

### `POST /wr/record/save`（草稿，可反复调用，幂等）
### `POST /wr/record/submit`（提交审核）

#### 附件2（标准表头，每机构一行）

```json
{
  "taskId": "3000000000000001",
  "rows": [
    {
      "rowIndex": 1,
      "cells": [
        { "itemId": "是否为国家级质控中心的itemId", "value": "1" },
        { "itemId": "线上次数的itemId",             "value": "3" },
        { "itemId": "完成情况的itemId",             "value": "done" }
      ]
    }
  ]
}
```

#### 附件3（矩阵 + 独立数字项）

```json
{
  "taskId": "3000000000000002",
  "rows": [
    {
      "rowIndex": 0,
      "cells": [
        { "itemId": "2000000000100002", "value": "8"  },
        { "itemId": "2000000000100003", "value": "45" }
      ]
    },
    { "rowIndex": 1,  "cells": [{ "itemId": "2000000000100001", "value": "1" }] },
    { "rowIndex": 4,  "cells": [{ "itemId": "2000000000100001", "value": "1" }] },
    { "rowIndex": 17, "cells": [{ "itemId": "2000000000100001", "value": "1" }] }
  ]
}
```

> `rowIndex=0` 专门给独立数字项（市级/县级成立个数），矩阵行从 1 开始，不冲突。

---

## 八、读取已保存记录

### `GET /wr/record/detail/{recordId}`

```json
{
  "data": {
    "record":      { "id": "...", "status": 0, "orgName": "超声质控中心" },
    "statusLabel": "草稿",
    "values": [
      { "itemId": "2000000000100001", "rowIndex": 1,  "cellValue": "1" },
      { "itemId": "2000000000100001", "rowIndex": 4,  "cellValue": "0" },
      { "itemId": "2000000000100002", "rowIndex": 0,  "cellValue": "8"  },
      { "itemId": "2000000000100003", "rowIndex": 0,  "cellValue": "45" }
    ],
    "attachments": [],
    "items": [ /* 本条记录涉及的列定义（含 headerPath / dictCode）*/ ],
    "rows":  [ /* 附件3有值，附件2为[] */ ]
  }
}
```

**回填逻辑：**

```javascript
// 构建 valueMap
const valueMap = {};
values.forEach(v => {
  valueMap[`${v.itemId}_${v.rowIndex}`] = v.cellValue;
});

// 矩阵格取值
const checked = valueMap[`${itemId}_${rowIndex}`] === "1";

// 独立数字项取值（rowIndex=0）
const cityCount = valueMap[`2000000000100002_0`];

// 附件2字段取值
const fieldVal = valueMap[`${item.id}_1`]; // rowIndex=1
```

---

## 九、字典接口

```
GET  /wr/dict/types               → 所有字典类型列表
GET  /wr/dict/items/{dictCode}    → 指定字典的选项列表（填报时调用）
POST /wr/dict/type/save           → 新增/修改字典类型（Admin）
POST /wr/dict/type/delete/{id}    → 删除字典类型（Admin）
POST /wr/dict/items/save/{typeId} → 全量覆盖字典条目（Admin）
```

选项结构：

```json
[
  { "id": "...", "dictTypeId": "...", "itemLabel": "是", "itemValue": "1", "sortNum": 1 },
  { "id": "...", "dictTypeId": "...", "itemLabel": "否", "itemValue": "0", "sortNum": 2 }
]
```

---

## 十、任务与上报流程

```
qcUser 填报流程：
  GET /wr/task/active                       → 进行中任务列表
  GET /wr/template/detail/full/{templateId} → 模板结构
  GET /wr/record/detail/{recordId}          → 草稿回填（如已有）
  POST /wr/record/save                      → 保存草稿（幂等）
  POST /wr/record/submit                    → 提交审核

deptAdmin 管理流程：
  GET /wr/record/admin/page                 → 所有机构上报列表
  GET /wr/record/admin/aggregate            → 汇总视图
  POST /wr/record/approve/{id}              → 审核通过
  POST /wr/record/reject                    → 驳回（可附 resubmitDeadline）
```

---

## 十一、其他常用接口速查

| 接口 | 说明 |
|---|---|
| `POST /api/auth/login` | 登录 |
| `GET /api/auth/users` | 用户列表（Admin） |
| `GET /wr/task/active` | 当前进行中任务（qcUser） |
| `GET /wr/task/page` | 任务分页（Admin） |
| `POST /wr/task/publish` | 发布任务（Admin） |
| `GET /wr/record/my/page` | 我的上报列表（qcUser） |
| `GET /wr/record/admin/page` | 所有上报列表（Admin） |
| `POST /wr/attachment/upload` | 上传佐证附件 |
| `GET /wr/attachment/list/{recordId}` | 记录附件列表 |

---

## 十二、测试数据

| 项目 | 值 |
|---|---|
| 附件2 templateId | `2000000000000001` |
| 附件3 templateId | `2000000000000002` |
| 附件2 taskId | `3000000000000001` |
| 附件3 taskId | `3000000000000002` |
| 管理员账号 | `wr_admin / Admin@2025`（deptAdmin） |
| 机构账号A | `wr_org_a / OrgA@2025`（qcUser） |
| 机构账号B | `wr_org_b / OrgB@2025`（qcUser） |
| Swagger UI | `http://localhost:8083/swagger-ui/index.html` |
