# 工作上报模块（workreport）前端接入指引

> 测试环境基础路径：`http://localhost:8080`  
> 所有接口需在请求头携带 Token：`Authorization: xxx`（登录后从 `/sysLoginUser/login` 获取）  
> Long 型 ID 以字符串返回（避免 JS 精度丢失），请用 `String` 或 BigInt 处理

---

## 一、角色与权限对应

| 角色 | roleCode | 可用接口 |
|------|----------|---------|
| 管理员 | `deptAdmin` | 全部接口 |
| 机构用户 | `qcUser` / `medicalUser` | 查进行中任务、填报草稿、提交、上传附件、查看自己的记录 |

---

## 二、接口汇总

### 【管理端】模板管理（`deptAdmin`）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| T1 | POST | `/wr/template/add` | 新增上报模板 |
| T2 | POST | `/wr/template/edit` | 编辑模板基本信息 |
| T3 | POST | `/wr/template/delete` | 删除模板 |
| T4 | POST | `/wr/template/updateStatus` | 启用/停用模板 |
| T5 | GET  | `/wr/template/detail` | 模板详情 |
| T6 | GET  | `/wr/template/page` | 模板分页列表 |
| T7 | GET  | `/wr/template/list` | 模板不分页列表 |
| T8 | GET  | `/wr/template/headerTree` | 获取多级表头树（前端渲染表格列头用） |
| T9 | POST | `/wr/template/saveHeaders` | 批量保存表头（全量覆盖） |
| T10 | POST | `/wr/template/item/edit` | **单节点精细编辑**（配置 requireAttachment / placeholder 等） |
| T11 | POST | `/wr/template/item/uploadFormatTemplate` | 给节点上传格式模板文件（Word/PDF）|
| T12 | POST | `/wr/template/item/deleteFormatTemplate` | 删除节点的格式模板文件 |
| T13 | GET  | `/wr/template/item/detail` | 查看节点详情（含格式模板下载 URL） |

### 【管理端】任务管理（`deptAdmin`）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| A1 | POST | `/wr/task/add` | 新建上报任务（关联模板，设截止日期） |
| A2 | POST | `/wr/task/edit` | 编辑任务 |
| A3 | POST | `/wr/task/delete` | 删除任务（无记录时才可删）|
| A4 | POST | `/wr/task/updateStatus` | 发布任务（草稿→进行中）/ 结束任务（进行中→已结束）|
| A5 | GET  | `/wr/task/detail` | 任务详情 |
| A6 | GET  | `/wr/task/page` | 任务分页列表 |

### 【管理端】审阅上报（`deptAdmin`）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| R1 | GET  | `/wr/record/adminPage` | 查看某任务下所有机构的提交情况（含状态汇总）|
| R2 | GET  | `/wr/record/detail` | 查看某条上报的完整内容（表头 + 数据 + 附件）|
| R3 | POST | `/wr/record/audit` | 审核（通过 / 退回）|
| R4 | GET  | `/wr/record/aggregate` | **按字段聚合审阅**（点击某列，右侧展示所有机构该列数据）|
| R5 | GET  | `/wr/record/export` | **导出 Excel**（全量，含所有机构所有字段）|

### 【机构端】填报（`qcUser` / `medicalUser`）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| O1 | GET  | `/wr/task/activeTasks` | 查询所有进行中的任务（首页任务列表）|
| O2 | POST | `/wr/record/save` | 保存草稿（首次保存自动建记录，重复调用幂等更新）|
| O3 | POST | `/wr/record/submit` | 提交上报（触发必填附件校验）|
| O4 | GET  | `/wr/record/orgPage` | 我的上报记录列表 |
| O5 | GET  | `/wr/record/detail` | 查看某次上报的完整数据（含审核意见）|
| O6 | POST | `/wr/attachment/upload` | 上传佐证附件（multipart/form-data）|
| O7 | POST | `/wr/attachment/delete` | 删除附件 |
| O8 | GET  | `/wr/attachment/list` | 查询某次上报的附件列表 |

---

## 三、接口详细说明与测试用例

> 测试前提：执行 `wr_module_ddl.sql` + `wr_test_data.sql`  
> 测试任务 ID = `2000000000000000001`，模板 ID = `1000000000000000001`

---

### T8 · 获取模板表头树

```
GET /wr/template/headerTree?id=1000000000000000001
```

**返回说明：**
```json
[
  {
    "id": "1000000000000000010",       // 节点 ID（注意：字符串格式）
    "itemName": "国家质控",             // 列头文字，直接渲染
    "isLeaf": 0,                       // 0=分组，不对应填报值；1=叶子，需要填报
    "headerRow": 1,                    // 表头第几行（多行表头时用于合并单元格）
    "colIndex": 1,                     // 第几列（起始列）
    "colSpan": 2,                      // 跨几列（合并单元格用）
    "rowSpan": 1,                      // 跨几行
    "requireAttachment": 0,            // 0=不需要附件，1=必须上传，2=建议上传
    "formatTemplateFileId": null,      // 有值则显示"下载格式模板"按钮
    "formatTemplateName": null,        // 格式模板文件名
    "formatTemplateUrl": null,         // 直接访问的下载链接
    "children": [                      // 子节点（递归，叶子为空数组）
      {
        "id": "1000000000000000011",
        "itemName": "是否为国家级质控中心",
        "isLeaf": 1,                   // 叶子：要填这一格
        "valueType": "select",         // text/number/date/select/attachment
        "unit": null,
        "placeholder": null,
        "children": []
      }
    ]
  },
  {
    "id": "1000000000000000050",
    "itemName": "质控调研（请附调研报告）",
    "isLeaf": 0,
    "requireAttachment": 1,            // ⚠️ 必须上传附件！前端显示红色标记
    "placeholder": "请在填写后，在附件区上传调研报告（见格式模板）",
    "formatTemplateUrl": null,         // 若管理员已上传格式模板则非 null
    "children": [...]
  }
]
```

**前端用法：**
- 遍历树形数据渲染多行合并表头（headerRow / colSpan / rowSpan）
- `isLeaf=1` 的节点才需要前端渲染输入控件
- `requireAttachment=1` 的节点标头显示红色"*"，表单底部显示必传提示
- `formatTemplateUrl` 非 null 时，在对应列标题旁显示"下载格式模板"图标/按钮

---

### T10 · 单节点精细编辑

```
POST /wr/template/item/edit
Content-Type: application/json

{
  "id": "1000000000000000050",
  "requireAttachment": 1,
  "placeholder": "请在填写后上传调研报告，报告格式见下方格式模板"
}
```

**可编辑字段：** `itemName` / `requireAttachment` / `placeholder` / `unit` / `valueType`  
**不可修改：** `parentId` / `colIndex` / `rowSpan` / `colSpan`（结构字段，需重新 saveHeaders）

---

### T11 · 上传格式模板

```
POST /wr/template/item/uploadFormatTemplate
Content-Type: multipart/form-data

file=<调研报告格式模板.docx>
itemId=1000000000000000050
```

**返回说明：**
```json
{
  "code": 200,
  "data": {
    "id": "1000000000000000050",
    "itemName": "质控调研（请附调研报告）",
    "formatTemplateFileId": "900000000000000001",
    "formatTemplateName": "调研报告格式模板.docx",
    "formatTemplateUrl": "/sysFileInfo/publicDownload?fileId=900000000000000001"
  }
}
```

---

### A1 · 新增上报任务

```
POST /wr/task/add
Content-Type: application/json

{
  "taskName": "2025年度省级质控中心工作情况上报",
  "templateId": "1000000000000000001",
  "statYear": "2025",
  "deadline": "2025-06-30 23:59:59",
  "remark": "请各省级质控中心于6月30日前完成上报，对于质控调研请附调研报告。"
}
```

---

### A4 · 发布任务（草稿 → 进行中）

```
POST /wr/task/updateStatus
Content-Type: application/json

{
  "id": "任务ID",
  "status": 1
}
```

**状态流转：** `0(草稿)` → `1(进行中)` → `2(已结束)`  
发布后机构端 `/wr/task/activeTasks` 即可看到该任务。

---

### O1 · 机构端：查询进行中的任务

```
GET /wr/task/activeTasks
```

**返回说明：**
```json
{
  "data": [
    {
      "id": "2000000000000000001",
      "taskName": "2025年度省级质控中心工作情况上报",
      "templateId": "1000000000000000001",
      "templateName": "2025年度省级质控中心/技术指导中心工作开展情况统计表",
      "statYear": "2025",
      "deadline": "2025-06-30 23:59:59",
      "status": 1,
      "statusLabel": "进行中",
      "remark": "请各省级质控中心..."
    }
  ]
}
```

**前端用法：** 机构首页展示任务卡片，点击进入填报页，用 `templateId` 调用 `T8` 获取表头。

---

### O2 · 保存草稿（表单型 / 表格型）

**表单型（每行只有一条记录，rowIndex 默认为 1）：**
```
POST /wr/record/save
Content-Type: application/json

{
  "taskId": "2000000000000000001",
  "values": [
    {"itemId": "1000000000000000011", "value": "是"},
    {"itemId": "1000000000000000012", "value": "优秀"},
    {"itemId": "1000000000000000211", "value": "6"},
    {"itemId": "1000000000000000212", "value": "280"},
    {"itemId": "1000000000000000221", "value": "3"},
    {"itemId": "1000000000000000222", "value": "150"},
    {"itemId": "1000000000000000411", "value": "4"},
    {"itemId": "1000000000000000412", "value": "200"},
    {"itemId": "1000000000000000421", "value": "2"},
    {"itemId": "1000000000000000422", "value": "500"},
    {"itemId": "1000000000000000121", "value": "85"},
    {"itemId": "1000000000000000131", "value": "专职3人，场地200㎡"},
    {"itemId": "1000000000000000150", "value": "承办了全省质控工作现场经验交流会"}
  ]
}
```

**返回：** `{ "data": "3000000000000000001" }` → `recordId`，后续上传附件需要用

**注意：**
- 接口幂等：同一机构同一任务多次调用只更新，不重复创建记录
- 只需传本次有值的字段，未传的字段不会被清空（UPSERT 逻辑）
- 草稿状态（status=1）可反复保存；已提交（status=2）不可再调 save

---

### O6 · 上传佐证附件

```
POST /wr/attachment/upload
Content-Type: multipart/form-data

file=<调研报告.pdf>
recordId=3000000000000000001
itemId=1000000000000000050     ← 可选；填写则挂接到"质控调研"节点；不填则作为整体佐证
```

**返回说明：**
```json
{
  "data": {
    "id": "5000000000000000001",
    "recordId": "3000000000000000001",
    "itemId": "1000000000000000050",   // 挂接的节点 ID，null 表示整体佐证
    "attachName": "调研报告.pdf",       // 原始文件名
    "attachPath": "http://minio/.../调研报告.pdf",  // MinIO 访问 URL
    "fileId": "900000000000000002",    // Roses 框架文件 ID（删除时用）
    "attachSize": 204800,
    "attachType": "pdf"
  }
}
```

---

### O3 · 提交上报

```
POST /wr/record/submit
Content-Type: application/json

{
  "taskId": "2000000000000000001"
}
```

**校验逻辑（后端）：**
1. 过了截止日期 → 报错 `DW-TASK-003`
2. 存在 `requireAttachment=1` 的节点未上传附件 → 报错，提示缺失列名
3. 通过 → status 变为 2（已提交）

---

### R4 · 按字段聚合审阅（核心审阅接口）

```
GET /wr/record/aggregate?taskId=2000000000000000001&itemId=1000000000000000040
```

> 点击左侧"质控指导（技术指导）"一级分组节点 → 展开其下 4 个叶子列

**返回说明：**
```json
{
  "data": {
    "itemId": "1000000000000000040",
    "itemName": "质控指导（技术指导）",
    "isLeaf": 0,
    "leafColumns": [                     // 展开的叶子列，用于渲染右侧表格的列头
      {"itemId": "1000000000000000311", "itemName": "指导次数", "unit": "次", "colIndex": 11},
      {"itemId": "1000000000000000312", "itemName": "参与人数", "unit": "人", "colIndex": 12},
      {"itemId": "1000000000000000321", "itemName": "指导次数", "unit": "次", "colIndex": 13},
      {"itemId": "1000000000000000322", "itemName": "参与人数", "unit": "人", "colIndex": 14}
    ],
    "orgRows": [                         // 每行 = 一个机构
      {
        "orgName": "某市中心医院",
        "recordId": "3000000000000000001",
        "recordStatus": 3,
        "recordStatusLabel": "审核通过",
        "submitTime": "2025-03-15 10:00:00",
        "auditRemark": "材料齐全，数据真实，审核通过",
        "maxRowIndex": 1,
        "valueMap": {                    // key = "itemId_rowIndex"
          "1000000000000000311_1": "5",
          "1000000000000000312_1": "120",
          "1000000000000000321_1": "8",
          "1000000000000000322_1": "200"
        }
      },
      {
        "orgName": "某区卫生院",
        "recordId": "3000000000000000002",
        "recordStatus": 2,
        "recordStatusLabel": "待审核",
        "submitTime": "2025-04-01 16:20:00",
        "auditRemark": null,
        "maxRowIndex": 1,
        "valueMap": {
          "1000000000000000311_1": "2",
          "1000000000000000312_1": "30",
          "1000000000000000321_1": "3",
          "1000000000000000322_1": "60"
        }
      }
    ]
  }
}
```

**前端渲染方式：**
```
左侧：模板表头树（/wr/template/headerTree）
右侧：
  列头 = leafColumns（注意可能有多个叶子列，用父节点二级表头分组展示）
  行   = orgRows
  单元格值 = row.valueMap[`${leafColumn.itemId}_${rowIndex}`]
  多 tab：管理员勾选多个节点，每个节点调一次 aggregate，每个返回对应一个 tab
```

---

### R3 · 审核

```
POST /wr/record/audit
Content-Type: application/json

// 通过
{
  "id": "3000000000000000002",
  "auditResult": 3,
  "auditRemark": "数据属实，调研报告完整，审核通过"
}

// 退回
{
  "id": "3000000000000000002",
  "auditResult": 4,
  "auditRemark": "经费执行说明过于简略，请补充说明后重新提交"
}
```

退回后机构端查看详情时 `auditRemark` 非空，前端可在填报页顶部显示退回意见横幅。

---

### R2 · 查看上报详情（通用，管理端+机构端）

```
GET /wr/record/detail?recordId=3000000000000000001
```

**返回说明：**
```json
{
  "data": {
    "record": {
      "id": "3000000000000000001",
      "orgName": "某市中心医院",
      "status": 3,
      "statusLabel": "审核通过",
      "submitTime": "2025-03-15 10:00:00",
      "auditRemark": "材料齐全，数据真实，审核通过"
    },
    "headerTree": [...],               // 同 T8，用于渲染表头
    "valueMap": {                      // key="itemId_rowIndex", value=填报值
      "1000000000000000011_1": "是",
      "1000000000000000012_1": "优秀",
      "1000000000000000211_1": "6",
      "1000000000000000212_1": "280"
    },
    "maxRowIndex": 1,                  // 表格型时可能 > 1
    "attachments": [
      {
        "id": "5000000000000000001",
        "itemId": "1000000000000000050",
        "attachName": "2025年专项调研报告.pdf",
        "attachPath": "http://minio/.../2025年专项调研报告.pdf",
        "attachType": "pdf",
        "attachSize": 204800
      }
    ]
  }
}
```

**前端用法：**
- 用 `headerTree` 渲染表格列头结构
- 用 `valueMap["itemId_1"]` 取对应格子的值（单行表单用 rowIndex=1）
- `attachments` 按 `itemId` 分组展示到对应列（`itemId=null` 的作为整体附件区展示）

---

### R5 · 导出 Excel

```
GET /wr/record/export?taskId=2000000000000000001
```

**直接触发文件下载，不走统一返回格式。**  
前端写法：
```javascript
window.open('/wr/record/export?taskId=2000000000000000001')
// 或
const link = document.createElement('a')
link.href = `/wr/record/export?taskId=2000000000000000001`
link.click()
```

导出 Excel 列结构：`机构名称 | 提交状态 | 提交时间 | 审核意见 | [所有叶子列...]`

---

## 四、完整测试场景步骤（Apifox / Postman）

```
1. 管理员登录，获取 Token（/sysLoginUser/login）
2. 查看模板表头树（T8）→ 确认 3 级表头返回正常
3. 精细配置"质控调研"节点（T10）→ requireAttachment=1
4. （可选）上传"质控调研"格式模板文件（T11）→ 确认 formatTemplateUrl 返回
5. 查询任务分页列表（A6）→ 确认测试任务在列
6. ----
7. 机构用户登录（qcUser 角色），获取 Token
8. 查询进行中任务（O1）→ 看到测试任务
9. 获取模板表头树（T8）→ 确认 requireAttachment=1 的节点有红色标记提示
10. 保存草稿（O2）→ 返回 recordId
11. 上传附件（O6）→ recordId + itemId=质控调研节点 → 返回 attachment 对象
12. 提交上报（O3）→ 成功（已上传必填附件）
13. 查看我的上报（O4 / O5）→ 状态变为"待审核"
14. ----
15. 管理员：查看任务下所有机构上报情况（R1）→ 看到 3 个机构，状态各不同
16. 管理员：按字段聚合审阅（R4）→ 点击"质控工作会议"节点 → 所有机构数据横排展示
17. 管理员：查看某机构详情（R2）→ 确认 valueMap / attachments 正常
18. 管理员：审核通过（R3）→ auditResult=3
19. 管理员：导出 Excel（R5）→ 浏览器弹出下载
20. ----
21. 机构用户：查看详情（O5）→ auditRemark 应显示审核意见
```

---

## 五、常见错误码

| 错误码 | 含义 | 处理建议 |
|--------|------|---------|
| `DW-TASK-001` | 任务不存在 | 检查 taskId |
| `DW-TASK-002` | 任务未处于进行中 | 提示"任务已结束或未开始" |
| `DW-TASK-003` | 超过截止时间 | 提示"已超过截止日期" |
| `DW-RECORD-002` | 记录已提交不可重复提交 | 提示"已提交，如需修改请联系管理员退回" |
| `DW-RECORD-003` | 记录未提交不可审核 | 管理端拦截，不显示审核按钮 |
| `DW-ATTACH-003` | 必填附件未上传 | 返回 RuntimeException，message 含缺失列名，前端直接 toast 展示 |

---

---

## 六、测试账号与登录方式

### 账号一览

| 账号 | 密码（明文） | 角色 | 所属机构 | 说明 |
|------|------------|------|---------|------|
| `admin_dept` | `Admin@2025` | `deptAdmin` | 省卫健委质控管理处 | 管理端：发布任务、配置模板、审核上报、聚合审阅、导出 |
| `org_a_user` | `OrgA@2025`  | `qcUser`    | 超声质控中心 | 机构端（专业质控中心）：已提交且审核通过，可查看记录及审核意见 |
| `org_b_user` | `OrgB@2025`  | `qcUser`    | 日间手术技术指导中心 | 机构端（专业技术指导中心）：草稿状态，可继续填报、上传附件并提交 |

> 账号创建脚本：`src/main/resources/sql/wr_test_accounts.sql`  
> 密码哈希生成：运行 `WrTestAccountGen.main()` 后按提示替换占位符

---

### 登录接口（RSA 加密密码）

系统使用 RSA 加密传输密码，**登录时客户端需先对明文密码做 RSA 公钥加密**。

**RSA 公钥（Base64，固定值来自 AuthConfig）：**
```
MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCytSVn3ff7eBJckAFYwgJjqE9Z
q2uAL4g+hkfQqGALdT8NJKALFxNzeSD/xTBLAJrtALWbN1dvyktoVNPAuuzCZO1B
xYZNaAU3IKFaj73OSPzca5SGY0ibMw0KvEPkC3sZQeqBqx+VqYAqan90BeG/r9p3
6Eb0wrshj5XmsFeo6QIDAQAB
```

**JavaScript 加密示例（用于 Apifox Pre-request Script 或前端）：**
```javascript
// npm install jsencrypt  或在浏览器引入 jsencrypt CDN
const JSEncrypt = require('jsencrypt').JSEncrypt;

const publicKey = `MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCytSVn3ff7eBJckAFYwgJjqE9Z
q2uAL4g+hkfQqGALdT8NJKALFxNzeSD/xTBLAJrtALWbN1dvyktoVNPAuuzCZO1B
xYZNaAU3IKFaj73OSPzca5SGY0ibMw0KvEPkC3sZQeqBqx+VqYAqan90BeG/r9p3
6Eb0wrshj5XmsFeo6QIDAQAB`;

function rsaEncrypt(plainText) {
    const encrypt = new JSEncrypt();
    encrypt.setPublicKey(publicKey);
    return encrypt.encrypt(plainText);
}

// 使用示例
const encryptedPwd = rsaEncrypt('Admin@2025');
console.log('加密后密码:', encryptedPwd);  // 每次不同，传给登录接口
```

**Node.js 版本（无需浏览器）：**
```javascript
const crypto = require('crypto');

const publicKeyPem = `-----BEGIN PUBLIC KEY-----
MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCytSVn3ff7eBJckAFYwgJjqE9Z
q2uAL4g+hkfQqGALdT8NJKALFxNzeSD/xTBLAJrtALWbN1dvyktoVNPAuuzCZO1B
xYZNaAU3IKFaj73OSPzca5SGY0ibMw0KvEPkC3sZQeqBqx+VqYAqan90BeG/r9p3
6Eb0wrshj5XmsFeo6QIDAQAB
-----END PUBLIC KEY-----`;

function rsaEncrypt(plainText) {
    const buf = crypto.publicEncrypt(
        { key: publicKeyPem, padding: crypto.constants.RSA_PKCS1_PADDING },
        Buffer.from(plainText)
    );
    return buf.toString('base64');
}

console.log(rsaEncrypt('Admin@2025'));
```

**登录接口调用：**
```
POST /sysLoginUser/login
Content-Type: application/json

{
  "account": "admin_dept",
  "password": "<RSA加密后的 Base64 字符串>"
}
```

**返回（取 `token` 字段用于后续请求头）：**
```json
{
  "code": 200,
  "data": {
    "token": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "loginUser": {
      "userId": "8000000000000010001",
      "account": "admin_dept",
      "realName": "测试管理员",
      "organizationId": "8000000000000000001"
    }
  }
}
```

所有后续接口请求头加：
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

> **Apifox 快捷方式**：在集合设置中填入 Pre-request Script，自动加密密码并存入环境变量，无需每次手动加密。

---

*生成日期：2026-03-12*
