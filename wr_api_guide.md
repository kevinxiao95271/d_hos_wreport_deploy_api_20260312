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

*生成日期：2026-03-12（WR 通用模块）*

---

---

# 日常工作模块（Daily Work / dw）前端接入指引

> **本地服务地址**：`http://localhost:8083`  
> **Swagger UI**：`http://localhost:8083/swagger-ui.html`  
> **所有接口须在请求头携带 Token**：`Authorization: Bearer <token>`（登录方式同上）  
> **Long 型 ID 以字符串返回**，JS 侧请用 `String` 或 `BigInt` 接收  
> **本模块 v2 变更日期**：2026-04-18（新增5大类层级、network_build 等新模块）

---

## DW-一、模块体系与层级结构

### 1.1 五大类 + 13 小项 + 3 加分项（2025年度）

| 大类 key | 大类名称 | 满分 | 小项 key | 小项名称 | 小项满分 | 类型 |
|---------|---------|------|---------|---------|---------|------|
| `cat_plan` | 制定规划/目标/流程，建立数据库 | 30 | `work_plan` | 年度计划总结 | 10 | 纯上传（双槽） |
| | | | `annual_work` | 落实国家及省级政策举措 | 10 | 纯上传 |
| | | | `indicator_db` | 质控指标数据库建设 | 10 | 纯上传 ★新增 |
| `cat_network` | 健全质控网络，布置工作任务 | 10 | `network_build` | 三级质控网络完善 | 4 | 树选择+上传 ★新增 |
| | | | `meeting` | 布置年度质控工作任务 | 6 | 多条记录 |
| `cat_training` | 培训、检查、考核 | 20 | `training` | 质控培训 | 6 | 多条记录 |
| | | | `survey` | 质控调研 | 8 | 多条记录 |
| | | | `guidance` | 质控指导 | 6 | 多条记录+树选择 |
| `cat_report` | 收集/分析/反馈/报告 | 30 | `indicator_monitor` | 质控指标监测 | 10 | 纯上传 ★新增 |
| | | | `national_report` | 国家质量安全报告分册 | 10 | 纯上传 ★新增 |
| | | | `prov_report` | 浙江省质量安全报告 | 10 | 纯上传 ★新增 |
| `cat_compliance` | 管理规范性 | 10 | `activity_report` | 质控活动报备 | 4 | 纯上传（双槽） |
| | | | `funding` | 经费管理 | 6 | 表单型 |
| *(加分)* | — | — | `bonus_pub` | 加分项·丛书/指南 | 3 | 多条记录 |
| *(加分)* | — | — | `bonus_comp` | 加分项·技能竞赛 | 3 | 多条记录 |
| *(加分)* | — | — | `bonus_admin` | 加分项·行政指令性任务 | 4 | 纯上传（双槽） ★新增 |

> **已废弃**（数据保留，`is_enabled=false`）：`it_construction`（信息化建设）、`admin_response`（行政指令响应）

### 1.2 节点类型说明

| isLeaf | isBonus | 含义 | 前端处理 |
|--------|---------|------|---------|
| `false` | `false` | 大类容器（cat_xxx） | 只渲染标题+折叠面板，不显示填报控件 |
| `true` | `false` | 常规叶子（可填报） | 按模块类型渲染对应组件 |
| `true` | `true` | 加分项叶子 | 归入"加分项"分组独立展示 |

### 1.3 填报模式说明

| 模式 | 模块 key | 说明 |
|------|---------|------|
| **多条记录型** | `meeting` `training` `survey` `guidance` `bonus_pub` `bonus_comp` | 每次新增一条子记录，多条并列展示 |
| **纯上传型** | `annual_work` `indicator_db` `indicator_monitor` `national_report` `prov_report` `work_plan` `activity_report` `bonus_admin` | 直接上传附件到对应 slot，无子记录概念 |
| **树选择+上传型** | `network_build` | 勾选树节点（市级/区县级）表示已覆盖，附上证明材料 |
| **表单型** | `funding` | 填写数值字段，第四季度专属 |

---

## DW-二、接口汇总

### 【配置接口】（管理员 + 机构均可调用）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| DC1 | GET | `/dw/config/modules` | 获取所有模块配置（管理员含分值，机构端脱敏） |
| DC2 | GET | `/dw/config/guidance/regions` | 获取质控指导/网络完善地区树（两棵树） |
| DC3 | GET | `/dw/config/task-modules/{taskId}` | 查询任务已启用的模块 key 列表 |
| DC4 | POST | `/dw/config/task-modules/{taskId}` | 设置任务的模块范围（管理员） |
| DC5 | POST | `/dw/config/module/update` | 更新模块配置（管理员） |
| DC6 | POST | `/dw/config/field/add` | 新增扩展字段定义（管理员） |
| DC7 | POST | `/dw/config/field/update` | 更新扩展字段（管理员） |
| DC8 | POST | `/dw/config/field/delete/{id}` | 禁用扩展字段（管理员） |
| DC9 | POST | `/dw/config/field/values/save` | 保存扩展字段值（机构填报） |

### 【填报接口】（机构用户）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| DR1 | GET | `/dw/record/init/{taskId}` | 进入任务，自动初始化草稿记录并返回完整详情 |
| DR2 | GET | `/dw/record/{recordId}` | 查看填报详情（机构只能查自己的） |
| DR3 | POST | `/dw/record/submit/{recordId}` | 提交填报 |
| DR4 | POST | `/dw/record/meeting/save` | 新增/编辑质控会议记录 |
| DR5 | POST | `/dw/record/meeting/delete/{id}` | 删除质控会议记录 |
| DR6 | POST | `/dw/record/training/save` | 新增/编辑质控培训记录 |
| DR7 | POST | `/dw/record/training/delete/{id}` | 删除质控培训记录 |
| DR8 | POST | `/dw/record/guidance/save` | 新增/编辑质控指导记录（含树选择） |
| DR9 | POST | `/dw/record/guidance/delete/{id}` | 删除质控指导记录 |
| DR10 | POST | `/dw/record/survey/save` | 新增/编辑质控调研记录 |
| DR11 | POST | `/dw/record/survey/delete/{id}` | 删除质控调研记录 |
| DR12 | POST | `/dw/record/network-build/save` | 保存三级质控网络完善（每条记录唯一，覆盖） ★新 |
| DR13 | POST | `/dw/record/funding/save` | 保存经费执行数据（每条记录唯一，覆盖） |
| DR14 | POST | `/dw/record/bonus/save` | 保存加分项（pub/comp，同类型覆盖） |
| DR15 | POST | `/dw/record/bonus/delete/{id}` | 删除加分项 |
| DR16 | POST | `/dw/record/module/score/save` | 保存模块自评分（便捷端点） ★新 |
| DR17 | POST | `/dw/record/attachment/upload` | 上传附件（通用，含模块标识和 slot） |
| DR18 | POST | `/dw/record/attachment/delete/{id}` | 删除附件 |

### 【管理员接口】

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| DA1 | GET | `/dw/record/admin/overview` | 跨机构汇总视图（所有机构填报状态） |
| DA2 | GET | `/dw/record/year-summary` | 年度汇总（指定机构的多季度快照） |
| DA3 | GET | `/dw/record/{recordId}` | 查看任意机构的填报详情 |
| DA4 | POST | `/dw/record/audit/{recordId}` | 审核（result=1 通过，result=0 驳回） |

---

## DW-三、接口详细说明

### DC1 · 获取模块配置

```
GET /dw/config/modules
```

**关键返回字段（管理员视角）：**
```json
[
  {
    "id": "9000000000000020",
    "moduleKey": "cat_plan",
    "moduleName": "制定规划/目标/流程，建立数据库",
    "scoreMax": 30,
    "scoreRule": "满分30分，含三个子项：年度计划总结（10分）、落实国家及省级政策举措（10分）、质控指标数据库建设（10分）",
    "scoreDesc": "本大类含三项工作：年度计划总结、落实国家及省级政策举措、质控指标数据库建设。",
    "isEnabled": true,
    "sortOrder": 10,
    "uploadHint": "",
    "parentModuleKey": null,
    "isLeaf": false,
    "isBonus": false,
    "extraFields": []
  },
  {
    "id": "9000000000000007",
    "moduleKey": "work_plan",
    "moduleName": "年度计划总结",
    "scoreMax": 10,
    "scoreRule": "有计划、总结（1分）；年度工作计划目标清晰、责任明确、措施可行（4分）；年度工作总结完成情况（3分）；计划与总结按规定时限报送（2分）",
    "scoreDesc": "请提交年度工作计划及年度工作总结（需加盖公章），并确保按规定时限报送。",
    "uploadHint": "请分别上传年度工作计划及年度工作总结（PDF/DOCX），需加盖公章，并确保按规定时限报送",
    "parentModuleKey": "cat_plan",
    "isLeaf": true,
    "isBonus": false,
    "extraFields": []
  },
  {
    "moduleKey": "network_build",
    "moduleName": "三级质控网络完善",
    "scoreMax": 4,
    "scoreRule": "未成立（0分）；部分市、县成立（2分）；市级全覆盖（3分）；省市县全部成立（4分）",
    "scoreDesc": "请通过树选择器标注已建立质控中心的市级及区县单位覆盖范围，并上传相关证明材料。",
    "parentModuleKey": "cat_network",
    "isLeaf": true,
    "isBonus": false,
    "extraFields": []
  },
  {
    "moduleKey": "bonus_admin",
    "moduleName": "加分项·行政指令性任务",
    "scoreMax": 4,
    "scoreRule": "承担卫生健康行政部门交办的工作任务：国家工作任务（2分）；浙江省工作任务（2分）",
    "scoreDesc": "请按国家任务和浙江省任务分别上传承担行政部门交办工作任务的证明材料。",
    "parentModuleKey": null,
    "isLeaf": true,
    "isBonus": true,
    "extraFields": []
  }
]
```

**机构端调用**：`scoreRule` 和 `scoreMax` 均为 `null`，只显示 `scoreDesc` 和 `uploadHint`。

**前端渲染逻辑：**
```
1. 按 sortOrder 排序
2. isLeaf=false → 大类折叠面板标题（不渲染填报控件）
3. isBonus=true → 归入"加分项"独立分组
4. 按 parentModuleKey 将叶子节点挂在对应大类下
5. isEnabled=false 的节点跳过不渲染
```

---

### DC2 · 获取地区树（质控指导 / 网络完善共用）

```
GET /dw/config/guidance/regions
```

**返回说明：**
```json
{
  "data": {
    "cityTree": {
      "name": "浙江省",
      "children": [
        {"id": 101, "name": "杭州市级", "level": 2, "children": []},
        {"id": 102, "name": "宁波市级", "level": 2, "children": []},
        {"id": 103, "name": "温州市级", "level": 2, "children": []},
        "... 共11个叶节点"
      ]
    },
    "countyTree": {
      "name": "浙江省",
      "children": [
        {
          "id": 201, "name": "杭州市", "level": 2,
          "children": [
            {"id": 20101, "name": "上城区", "level": 3, "children": []},
            {"id": 20102, "name": "拱墅区", "level": 3, "children": []},
            "..."
          ]
        },
        "... 共11个市节点，101个县节点"
      ]
    }
  }
}
```

**两棵树的用途：**
- `cityTree`：用于 `guidance.cityCenterIds` / `network_build.cityCenterIds` — 勾选市级质控中心（共11个叶节点）
- `countyTree`：用于 `guidance.countyCenterIds` / `network_build.countyCenterIds` — 勾选区县级质控中心（101个叶节点）

**前端计数规则：**
- `cityCenterCount` = `cityTree` 中被勾选的叶节点数
- `countyCenterCount` = `countyTree` 中被勾选的区县（level=3）叶节点数
- ID 列表以 JSON 数组字符串传入后端：`"[101, 103, 105]"`

---

### DR1 · 初始化/获取填报记录

```
GET /dw/record/init/{taskId}
```

> **首次进入任务时调用**。若该机构该任务下尚无记录，自动创建草稿并返回；已有记录直接返回。

**完整响应结构（`DwRecordDetailVO`）：**
```json
{
  "data": {
    "recordId": "7000000000000000001",
    "taskId":   "6000000000000000001",
    "taskName": "2025年度质控工作日常工作考核",
    "orgId":    "8000000000000000002",
    "orgName":  "超声质控中心",
    "taskType": "daily_work",
    "statYear": "2025",
    "statQuarter": null,
    "status":   0,
    "auditRemark": null,
    "readOnly": false,
    "enabledModuleKeys": ["meeting","training","guidance","survey","work_plan","annual_work",
                          "indicator_db","network_build","indicator_monitor","national_report",
                          "prov_report","activity_report","funding","bonus_pub","bonus_comp","bonus_admin"],

    "moduleSelfScores": {
      "meeting": null, "training": null, "guidance": null, "survey": null,
      "indicator_db": null, "network_build": null, "indicator_monitor": null,
      "national_report": null, "prov_report": null, "bonus_admin": null
    },

    "meetings":   [],
    "trainings":  [],
    "guidances":  [],
    "surveys":    [],

    "workPlanFiles":       {"plan": [], "summary": []},
    "annualWorkFiles":     [],
    "indicatorDbFiles":    [],
    "indicatorMonitorFiles": [],
    "nationalReportFiles": [],
    "provReportFiles":     [],
    "activityReportFiles": {"pre_report": [], "post_report": []},
    "bonusAdminFiles":     {"national_task": [], "prov_task": []},

    "networkBuild": null,
    "funding": null,
    "bonuses": [],

    "workPlanExtra": {},     "annualWorkExtra": {},
    "indicatorDbExtra": {},  "indicatorMonitorExtra": {},
    "nationalReportExtra": {}, "provReportExtra": {},
    "activityReportExtra": {}, "bonusAdminExtra": {},
    "fundingExtra": {}
  }
}
```

**`status` 含义：**

| 值 | 含义 |
|----|------|
| 0 | 草稿（可编辑） |
| 1 | 已提交（待审核） |
| 2 | 已通过 |
| 3 | 已驳回（可重新编辑） |

**`readOnly` 含义：** `true` 时前端整体灰态只读（任务已结束、已提交、已通过）。

---

### DR4 · 保存质控会议（多条记录型）

```
POST /dw/record/meeting/save
Content-Type: application/json

{
  "id": null,                    // null=新增；有值=编辑
  "recordId": "7000000000000000001",
  "meetingName": "2025年第一季度质控工作例会",
  "meetingStartDate": "2025-03-15",
  "meetingStartHalf": "上午",    // 上午 | 下午
  "meetingEndDate": "2025-03-15",
  "meetingEndHalf": "下午",
  "meetingForm": "线下",         // 线上 | 线下 | 混合
  "meetingContent": "讨论2025年质控重点指标及分工",
  "attendeeCount": 25,
  "attendanceRate": 92.5
}
```

**返回：** `DwMeeting` 实体（含生成的 `id`），后续上传附件需用此 `id` 作为 `subRecordId`。

**附件 slot：**
- `minutes`：会议纪要
- `photo`：现场照片
- `signin`：签到表

---

### DR6 · 保存质控培训（多条记录型）

```
POST /dw/record/training/save
Content-Type: application/json

{
  "id": null,
  "recordId": "7000000000000000001",
  "trainingName": "质控指标解读培训",
  "trainingStartDate": "2025-04-10",
  "trainingStartHalf": "上午",
  "trainingEndDate": "2025-04-10",
  "trainingEndHalf": "下午",
  "trainingForm": "线上",
  "trainingContent": "围绕国家质控指标变化解读及填报要求",
  "trainingPeopleCount": 120
}
```

**附件 slot：**
- `material`：培训材料
- `photo`：现场照片

---

### DR8 · 保存质控指导（多条记录型 + 树选择）

```
POST /dw/record/guidance/save
Content-Type: application/json

{
  "id": null,
  "recordId": "7000000000000000001",
  "guidanceStartDate": "2025-05-20",
  "guidanceStartHalf": "上午",
  "guidanceEndDate": "2025-05-21",
  "guidanceEndHalf": "下午",
  "guidanceForm": "线下",
  "guidanceContent": "对温州、绍兴市级质控中心开展现场指导",
  "cityCenterCount": 2,
  "cityCenterIds": "[103, 106]",      // 温州市级=103，绍兴市级=106
  "countyCenterCount": 3,
  "countyCenterIds": "[20301, 20302, 20601]",
  "hospitalCount": 0
}
```

> `cityCenterCount` / `countyCenterCount` 由前端统计勾选节点数后填入，`hospitalCount` 手动填写，三者之和不能为 0。

**附件 slot：** `evidence`（佐证材料）

---

### DR10 · 保存质控调研（多条记录型）

```
POST /dw/record/survey/save
Content-Type: application/json

{
  "id": null,
  "recordId": "7000000000000000001",
  "surveyStartDate": "2025-06-01",
  "surveyStartHalf": "上午",
  "surveyEndDate": "2025-06-03",
  "surveyEndHalf": "下午",
  "surveyTarget": "超声科",
  "surveyType": "实地调研",
  "surveyForm": "线下",
  "surveyContent": "对杭州市各医院超声科质控现状进行实地摸底调研"
}
```

**附件 slot：**
- `report`：调研报告
- `photo`：现场照片

---

### DR16 · 保存模块自评分 ★ 新增

```
POST /dw/record/module/score/save
Content-Type: application/json
```

**请求体：**
```json
{
  "recordId":   "7000000000000000001",
  "moduleKey":  "meeting",
  "subRecordId": "7050000000000000001",   // 多条记录型模块传子记录ID；纯上传/表单型不传（传 null）
  "score":      8.5                        // BigDecimal；传 null 表示清空自评分
}
```

**subRecordId 使用规则：**

| moduleKey | subRecordId |
|-----------|-------------|
| `meeting` `training` `guidance` `survey` `bonus_pub` `bonus_comp` | 必填，填对应子记录的 id |
| `work_plan` `annual_work` `indicator_db` `network_build` `indicator_monitor` `national_report` `prov_report` `activity_report` `funding` `bonus_admin` | 不填（传 `null`） |

**返回：** `{ "code": 200, "data": null }`

> **数据读取：** 自评分通过 `GET /dw/record/{recordId}` 返回的 `moduleSelfScores` 字段查看，key 为 moduleKey，value 为 BigDecimal。

---

### DR12 · 保存三级质控网络完善 ★ 新增

```
POST /dw/record/network-build/save
Content-Type: application/json

{
  "recordId": "7000000000000000001",
  "cityCenterCount": 3,
  "cityCenterIds": "[101, 103, 106]",      // 杭州市级=101, 温州市级=103, 绍兴市级=106
  "countyCenterCount": 5,
  "countyCenterIds": "[20101, 20102, 20301, 20601, 20602]",
  "selfScore": null                         // 机构自评，可不填；管理员据实际情况打分
}
```

> **关键设计**：每条 `recordId` 只有一份网络完善记录，重复调用会覆盖前一次。  
> 系统**不自动计分**，管理员查看覆盖范围后手动在审核时打分。

**返回：** `DwNetworkBuild` 实体（含 `id`），后续上传佐证材料需用 `recordId` 和 `moduleType=network_build`。

**响应中的 `networkBuild` 字段（在 DR2 查询时）：**
```json
{
  "networkBuild": {
    "id": "7100000000000000001",
    "cityCenterCount": 3,
    "cityCenterIds": "[101, 103, 106]",
    "cityCenterNames": ["杭州市级", "温州市级", "绍兴市级"],
    "countyCenterCount": 5,
    "countyCenterIds": "[20101, 20102, 20301, 20601, 20602]",
    "countyCenterNames": ["上城区", "拱墅区", "龙湾区", "越城区", "柯桥区"],
    "countyCenterGroups": [
      {"cityName": "杭州市", "counties": ["上城区", "拱墅区"]},
      {"cityName": "温州市", "counties": ["龙湾区"]},
      {"cityName": "绍兴市", "counties": ["越城区", "柯桥区"]}
    ],
    "selfScore": null,
    "evidences": [],
    "extraValues": {}
  }
}
```

---

### DR13 · 保存经费执行（表单型，第四季度）

```
POST /dw/record/funding/save
Content-Type: application/json

{
  "recordId": "7000000000000000001",
  "fiscalGrantAmount": 28.5,      // 财政专项拨款（万元）
  "fiscalExecRate": 95.2,         // 财政专项执行率（%）
  "hospitalGrantAmount": 12.0,    // 医院配套拨款（万元）
  "hospitalExecRate": 88.0        // 医院配套执行率（%）
}
```

> 同样每条 `recordId` 唯一，重复调用覆盖。

---

### DR14 · 保存加分项（多条记录型）

```
POST /dw/record/bonus/save
Content-Type: application/json

// 加分项1：丛书/指南（bonusType=publication）
{
  "id": null,
  "recordId": "7000000000000000001",
  "bonusType": "publication",
  "pubName": "超声质控中心指南2024",
  "pubCategory": "指南/共识",   // 丛书 | 指南/共识 | 标准/规范
  "pubDate": "2024-11-01"
}

// 加分项2：技能竞赛（bonusType=competition）
{
  "id": null,
  "recordId": "7000000000000000001",
  "bonusType": "competition",
  "compName": "2025年超声技能大赛",
  "compSponsor": "省总工会、省卫健委",
  "compStartDate": "2025-09-01",
  "compStartHalf": "上午",
  "compEndDate": "2025-09-03",
  "compEndHalf": "下午"
}
```

> `bonus_pub` / `bonus_comp` 分别用 `bonusType=publication` / `bonusType=competition` 区分。  
> 加分项3（`bonus_admin`）通过**纯上传**方式操作，无需调用此接口。

**附件 slot：** `evidence`（佐证材料）

---

### DR16 · 上传附件（通用）

```
POST /dw/record/attachment/upload
Content-Type: multipart/form-data

recordId=7000000000000000001
moduleType=<模块标识>        // 见下表
subRecordId=<子记录ID>       // 多条记录型模块必填，纯上传型不传
slot=<附件槽位>              // 见下表
file=<上传文件>
```

**moduleType 与 slot 对照表：**

| moduleType | subRecordId | slot | 说明 |
|-----------|-------------|------|------|
| `meeting` | 会议记录 id | `minutes` / `photo` / `signin` | 会议纪要/照片/签到表 |
| `training` | 培训记录 id | `material` / `photo` | 培训材料/现场照片 |
| `guidance` | 指导记录 id | `evidence` | 佐证材料 |
| `survey` | 调研记录 id | `report` / `photo` | 调研报告/现场照片 |
| `work_plan` | null | `plan` / `summary` | 年度计划/年度总结 |
| `annual_work` | null | `evidence` | 佐证材料 |
| `indicator_db` | null | `evidence` | 数据库/指标材料 ★新 |
| `network_build` | null | `evidence` | 网络完善证明材料 ★新 |
| `indicator_monitor` | null | `evidence` | 监测数据/报告 ★新 |
| `national_report` | null | `evidence` | 国家分册材料 ★新 |
| `prov_report` | null | `evidence` | 省报告材料 ★新 |
| `activity_report` | null | `pre_report` / `post_report` | 事前/事后报备截图 |
| `funding` | null | `evidence` | 经费佐证材料（可选） |
| `bonus_pub` | 加分项记录 id | `evidence` | 出版证明 |
| `bonus_comp` | 加分项记录 id | `evidence` | 竞赛证明 |
| `bonus_admin` | null | `national_task` / `prov_task` | 国家任务/省级任务证明 ★新 |

**返回：**
```json
{
  "data": {
    "id": "7200000000000000001",
    "recordId": "7000000000000000001",
    "moduleType": "network_build",
    "subRecordId": null,
    "slot": "evidence",
    "attachName": "三级网络完善证明.pdf",
    "attachPath": "http://minio-host/wk-registration-files/...",
    "attachType": "pdf",
    "attachSize": 204800,
    "createTime": "2026-04-18 19:30:00"
  }
}
```

---

### DR3 · 提交填报

```
POST /dw/record/submit/{recordId}
```

> 提交后 `status` 变为 1（已提交），`readOnly` 变为 `true`，机构端进入只读模式。

---

### DA1 · 跨机构汇总视图（管理员）

```
GET /dw/record/admin/overview?taskId=6000000000000000001
```

**返回说明：**
```json
{
  "data": {
    "taskId": "6000000000000000001",
    "taskName": "2025年度质控工作日常工作考核",
    "totalOrgs": 20,
    "submittedCount": 15,
    "approvedCount": 8,
    "orgRows": [
      {
        "orgId": "8000000000000000002",
        "orgName": "超声质控中心",
        "recordId": "7000000000000000001",
        "recordStatus": 1,
        "recordStatusLabel": "已提交",
        "submitTime": "2026-04-15 10:00:00",
        "moduleSummary": {
          "meeting": 3,
          "training": 2,
          "guidance": 1,
          "survey": 0,
          "networkBuild": true,
          "funding": true
        }
      }
    ]
  }
}
```

---

### DA4 · 审核（管理员）

```
POST /dw/record/audit/{recordId}?result=1&remark=材料齐全，审核通过
```

| result | 含义 |
|--------|------|
| 1 | 通过（status→2） |
| 0 | 驳回（status→3，机构可重新编辑） |

---

### DC9 · 保存扩展字段值

```
POST /dw/config/field/values/save
Content-Type: application/json

{
  "recordId": "7000000000000000001",
  "moduleKey": "meeting",
  "subRecordId": "7050000000000000001",   // 多条记录型传子记录ID；纯上传型传 null
  "values": {
    "module_self_score": "8.5"             // 该模块自评分（key固定为 module_self_score）
  }
}
```

> `module_self_score` 是系统预置的通用扩展字段 key，用于机构自评分。  
> 管理员在 DC5 更新模块配置时，自评分对评审仅供参考，最终分值由管理员认定。

---

## DW-四、模块层级前端渲染流程

```
1. 调用 DC1 获取全部模块配置 → 本地按 sortOrder 排序
2. 按 parentModuleKey 构建父子关系（parentModuleKey=null 且 isBonus=false → 大类；isBonus=true → 加分项）
3. isLeaf=false 的节点：渲染折叠面板标题（显示 moduleName + scoreMax + scoreDesc）
4. isLeaf=true 的节点：按模块类型渲染对应填报组件：
   - 多条记录型（meeting/training/guidance/survey/bonus_pub/bonus_comp）→ 列表 + 新增按钮
   - 树选择型（network_build/guidance）→ 两棵树 CheckTree 组件
   - 纯上传型（work_plan/annual_work/indicator_db/indicator_monitor/national_report/prov_report/activity_report/bonus_admin）→ 附件上传区（按 slot 分组）
   - 表单型（funding）→ 数字输入表单
5. 调用 DR1 init 获取记录数据 → 回填各模块数据
6. enabledModuleKeys：隐藏不在列表中的模块（任务按季度只开放部分模块时）
```

---

## DW-五、测试账号与登录方式

### 5.1 账号一览

| 账号 | 明文密码 | 角色 | 所属机构 | 用途 |
|------|---------|------|---------|------|
| `dw_admin` | `DwAdmin@2025` | `deptAdmin` | 省卫健委质控管理处（测试） | 管理端：配置/审核/汇总视图 |
| `dw_org_a` | `DwOrgA@2025` | `qcUser` | 超声质控中心（测试） | 机构端A：填报/上传附件 |
| `dw_org_b` | `DwOrgB@2025` | `qcUser` | 日间手术技术指导中心（测试） | 机构端B：另一机构测试 |

> 账号入库脚本：`src/main/resources/sql/wr_dw_test_accounts.sql`  
> BCrypt 哈希已生成嵌入脚本，直接执行即可，**无需手动替换占位符**。

### 5.2 入库步骤

```bash
# 方式一：psql 命令行（需安装 psql 客户端）
psql -h 119.167.165.27 -U postgres -d zjylzl -f src/main/resources/sql/wr_dw_test_accounts.sql

# 方式二：DBeaver / pgAdmin
# 连接到 119.167.165.27:5432/zjylzl，Schema 切换到 zjylzl，打开并执行上述 SQL 文件
```

> ⚠️ 脚本假设 `hr_organization` / `hr_person` / `sys_user` / `sys_user_role` 表结构与 Roses 框架默认一致。  
> 若表结构有差异（如字段名不同、缺少 ON CONFLICT 约束），请根据实际 DDL 调整。  
> 执行后用脚本末尾注释中的 SELECT 语句验证账号关系是否正确。

### 5.3 登录接口（DW 模块）

DW 模块的登录**不需要 RSA 加密**，直接传明文密码：

```
POST /api/auth/login
Content-Type: application/json

{
  "account": "dw_admin",
  "password": "DwAdmin@2025"
}
```

**返回：**
```json
{
  "code": 200,
  "data": {
    "token": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": "8900000000000020001",
    "account": "dw_admin",
    "realName": "DW测试管理员",
    "orgId": "8900000000000000001",
    "orgName": "省卫健委质控管理处（测试）",
    "roleCode": "deptAdmin"
  }
}
```

后续所有接口请求头加：
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## DW-六、完整自测场景（Apifox / Postman）

```
准备工作：
  - 确保 wr_ddl_init.sql 已执行（含新模块 DDL 和 dw_network_build 建表）
  - 执行 wr_dw_test_accounts.sql 完成测试账号入库
  - 登录账号：管理员 dw_admin / DwAdmin@2025；机构端 dw_org_a / DwOrgA@2025

--- 管理员操作 ---
1. 登录获取 Token（POST /api/auth/login，account=dw_admin，password=DwAdmin@2025，明文传输，无需加密）
2. 获取模块配置 DC1 → 确认：
   - 返回 cat_plan/cat_network/cat_training/cat_report/cat_compliance 5个大类
   - 返回 network_build/indicator_db/indicator_monitor/national_report/prov_report/bonus_admin 6个新叶子
   - is_leaf 和 parent_module_key 字段有值
   - scoreRule 字段有值（管理员才能看到）
3. 获取地区树 DC2 → 确认：
   - cityTree 有11个叶节点（杭州市级～丽水市级）
   - countyTree 有11个市节点，总101个区县叶节点

--- 机构端操作 ---
4. 切换为 dw_org_a / DwOrgA@2025 登录
5. 调用 DR1 init 初始化记录 → 确认：
   - 返回 recordId
   - networkBuild=null（未填）
   - indicatorDbFiles/indicatorMonitorFiles 等新字段为空数组
   - scoreRule=null（机构端不可见）
6. 新增质控会议：
   POST /dw/record/meeting/save（id=null，填完整信息）→ 返回会议记录 id=meetingId
7. 上传会议纪要：
   POST /dw/record/attachment/upload（moduleType=meeting, subRecordId=meetingId, slot=minutes, file=xxx.pdf）
8. 保存三级质控网络完善 DR12：
   cityCenterIds="[101,103]"，countyCenterCount=3，countyCenterIds="[20101,20102,20301]"
9. 上传网络完善证明材料：
   moduleType=network_build, subRecordId=null, slot=evidence
10. 上传 indicator_db 材料：
    moduleType=indicator_db, slot=evidence
11. 上传 bonus_admin 双槽：
    slot=national_task（国家任务）；slot=prov_task（省级任务）
12. 查看详情 DR2 → 确认：
    - meetings 列表有1条，含 minutes 附件
    - networkBuild.cityCenterNames=["杭州市级","温州市级"]
    - networkBuild.countyCenterGroups 按市分组展示
    - indicatorDbFiles 有1个文件
    - bonusAdminFiles={national_task:[...], prov_task:[...]}
13. 保存质控会议自评分 DR16：
    POST /dw/record/module/score/save
    { "recordId": "...", "moduleKey": "meeting", "subRecordId": "meetingId", "score": 8.5 }
14. 提交 DR3 POST /dw/record/submit/{recordId}

--- 管理员审核 ---
15. 切换回 dw_admin
16. 查看汇总 DA1 → 确认机构状态变为"已提交"
17. 查看详情 DA3 → 可看到 scoreRule 完整评分规则
18. 审核通过 DA4：result=1，remark="填报完整，审核通过"
19. 再次查看详情 → status=2（已通过），readOnly=true
```

---

## DW-八、多条记录型模块（会议 / 培训 / 指导 / 调研）前端渲染指引

> 涉及接口：`year-summary`（年度汇总只读展示）、`meeting/save`、`training/save`、`guidance/save`、`survey/save` 及对应 `delete` 接口

### 8.1 数据来源与结构

> **重要**：季度历史快照已嵌入年度任务详情，**前端只需调一个接口**，无需合并。

年度任务详情接口（填报者 / 管理端通用）：

```
GET /dw/record/init/{annualTaskId}      # 填报者（自动初始化草稿）
GET /dw/record/{recordId}               # 管理端（查看指定记录）
```

返回 `DwRecordDetailVO`，其中 `quarterlySnapshots` 字段自动嵌入 Q4→Q1 只读快照：

```json
{
  "recordId": "...",
  "statQuarter": null,
  "readOnly": false,
  "meetings":  [],
  "trainings": [],
  "guidances": [],
  "surveys":   [],
  "quarterlySnapshots": [
    {
      "taskId": "...",
      "taskName": "2025年度Q4日常工作",
      "statQuarter": 4,
      "recordId": "...",
      "recordStatus": 2,
      "readOnly": true,
      "detail": {
        "readOnly": true,
        "meetings": [ { "id": "...", "meetingName": "xxx质控会议", ... } ],
        "trainings": [ ... ],
        "guidances": [ ... ],
        "surveys":   [ { "id": "...", "surveyTarget": "xxx质控调研", ... } ]
      }
    },
    { "statQuarter": 3, "readOnly": true, "detail": { ... } },
    { "statQuarter": 2, "readOnly": true, "detail": { ... } },
    { "statQuarter": 1, "readOnly": true, "detail": { ... } }
  ]
}
```

**`quarterlySnapshots` 仅当 `statQuarter=null`（年度任务）时有值，季度任务详情该字段为 null。**
**后端已保证返回顺序为 Q4 → Q3 → Q2 → Q1，前端无需排序或合并。**

`year-summary` 接口保留，供管理端跨机构汇总等独立场景使用。

---

### 8.2 readOnly 字段说明

| 字段路径 | 类型 | 含义 |
|---|---|---|
| `row.readOnly` | `Boolean` | 整个季度块是否只读，`year-summary` 接口中**恒为 `true`** |
| `row.detail.readOnly` | `Boolean` | 详情级只读信号，与 `row.readOnly` 一致 |

**readOnly = true 时，前端必须：**
- 隐藏每条 meeting / training / guidance / survey 条目的「编辑」「删除」按钮
- 不渲染「新增」入口（新增会议 / 培训 / 指导 / 调研的按钮）
- 整个季度数据块以灰态只读形式展示，仅供参考

**readOnly = false 时（机构填报当前季度任务）：**
- 正常显示「新增」「编辑」「删除」按钮
- 各操作调用对应 save / delete 接口

---

### 8.3 条目内排序规则（后端已处理，前端直接渲染）

各季度内的条目排序由后端 SQL 保证，**前端按接口返回顺序直接渲染即可，无需做任何 sort / merge 操作**：

| 模块 | 排序规则 |
|---|---|
| 会议（meetings） | 会议开始日期 DESC → 上下午 DESC（PM > AM）→ id DESC |
| 培训（trainings） | 培训开始日期 DESC → 上下午 DESC → id DESC |
| 指导（guidances） | 指导开始日期 DESC → 上下午 DESC → id DESC |
| 调研（surveys） | 调研开始日期 DESC → 上下午 DESC → id DESC |

> **上下午字段值**：`"AM"` 上午 / `"PM"` 下午；DESC 排序时 PM 排在 AM 前面。

---

### 8.4 年度任务页面渲染流程（伪代码）

```js
// 1. 一个接口拿全部数据（年度自身 + 嵌入的季度历史快照）
const detail = await api.get(`/dw/record/init/${annualTaskId}`)

// detail.readOnly         = false  → 年度任务本身可编辑
// detail.meetings/...     = []     → 年度任务自己新增的条目
// detail.quarterlySnapshots        → Q4→Q1 只读快照，后端已排好序

// 2. 渲染年度任务本身（可编辑区域）
renderAnnualSection({
  readOnly: detail.readOnly,   // false
  meetings:  detail.meetings,
  trainings: detail.trainings,
  guidances: detail.guidances,
  surveys:   detail.surveys,
})

// 3. 渲染各季度历史块（只读，按接口顺序直接渲染，无需 sort/merge）
for (const row of detail.quarterlySnapshots ?? []) {
  renderQuarterBlock({
    title:    row.taskName,          // e.g. "2025年度Q4日常工作"
    quarter:  row.statQuarter,
    readOnly: row.readOnly,          // 恒 true
    meetings:  row.detail?.meetings  ?? [],
    trainings: row.detail?.trainings ?? [],
    guidances: row.detail?.guidances ?? [],
    surveys:   row.detail?.surveys   ?? [],
  })
}

// 4. 渲染单条 meeting 条目（以会议为例）
function renderMeetingItem(item, readOnly) {
  return `
    <div class="meeting-item">
      <span>${item.meetingName}</span>
      <span>${item.meetingStartDate} ${item.meetingStartHalf === 'AM' ? '上午' : '下午'}</span>
      <!-- 仅非只读时显示操作按钮 -->
      ${!readOnly ? `
        <button @click="editMeeting(item.id)">编辑</button>
        <button @click="deleteMeeting(item.id)">删除</button>
      ` : ''}
    </div>
  `
}
```

---

### 8.5 当前季度填报与历史季度展示区分

| 场景 | 数据来源接口 | readOnly |
|---|---|---|
| 填报当前**季度**任务 | `GET /dw/record/init/{taskId}` | `false`（可编辑） |
| 填报**年度**任务（含历史快照） | `GET /dw/record/init/{annualTaskId}` | 年度自身 `false`；`quarterlySnapshots[i].readOnly` 恒 `true` |
| 管理端审核年度记录（含历史快照） | `GET /dw/record/{recordId}` | 同上 |
| 管理端独立跨机构汇总 | `GET /dw/record/year-summary` | `true`（恒只读） |

**前端不再需要单独调 year-summary 来拼接季度数据，年度详情接口已一次性返回全部内容。**

---

## DW-七、错误码参考

| 错误码/HTTP | 含义 | 处理建议 |
|------------|------|---------|
| 401 | Token 无效或过期 | 重新登录获取 Token |
| 403 | 权限不足 | 确认角色（deptAdmin/qcUser） |
| `DW-RECORD-001` | 记录不存在 | 检查 recordId |
| `DW-RECORD-002` | 记录已提交，不可编辑 | 提示"已提交，请联系管理员驳回后修改" |
| `DW-RECORD-003` | 无权操作此记录 | 机构用户只能操作本机构记录 |
| `DW-TASK-002` | 任务未处于进行中 | 提示"任务已结束或未发布" |

---

*DW 模块接入指引 生成日期：2026-04-18*
