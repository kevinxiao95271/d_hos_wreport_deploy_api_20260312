# 日常工作模块 — 前端开发指引

> 后端版本：2025-03  
> Base URL：`http://<host>:8083`  
> 所有接口需携带 `Authorization: Bearer <token>` Header

---

## 一、整体流程

```
任务列表
  └─ GET /wr/task/page  (管理员)
  └─ GET /wr/task/active (机构用户)
        │
        │ taskType === 'daily_work'
        ▼
  进入日常工作填报页 /dw/record/init/:taskId
        │ 首次进入自动创建草稿，再次进入直接返回已有记录
        ▼
  填报各模块（随时保存草稿，status=0）
        │
        ▼
  提交 POST /dw/record/submit/:recordId
        │
        ▼
  管理员审核 POST /dw/record/audit/:recordId
        │  result=1 通过 / result=0 驳回
        ▼
  驳回后机构可修改后再次提交
```

**记录状态值：**

| status | 含义 |
|--------|------|
| 0 | 草稿（可编辑） |
| 1 | 已提交（不可编辑，等待审核） |
| 2 | 已通过 |
| 3 | 已驳回（可再次编辑提交） |

---

## 二、初始化 — 进入填报页

### 机构用户

```
GET /dw/record/init/:taskId
```

**响应：** `DwRecordDetailVO`（见第五节），包含该机构当前 recordId 及所有已填内容。  
`status=0` 或 `status=3` 时可编辑，其余状态只读。

### 管理员查看任意记录

```
GET /dw/record/:recordId
```

---

## 三、页面初始化时的并发请求（机构 + 管理员）

进入填报/审核页时，**并发**发出以下两个请求，结果缓存到本地状态：

```js
const [modules, regions] = await Promise.all([
  GET('/dw/config/modules'),          // 模块配置 + 考核说明 + 动态扩展字段定义
  GET('/dw/config/guidance/regions'), // 质控指导地区树（市级 + 县级）
])
```

### `GET /dw/config/modules` 返回字段说明

```ts
interface DwModuleConfigVO {
  id: string
  moduleKey: string        // 模块标识，见附录A
  moduleName: string       // 模块展示名
  scoreDesc: string        // 考核说明（机构 + 管理员均可见，已去掉具体分值）
  scoreMax: number | null  // 满分值（仅管理员可见，机构端为 null）
  scoreRule: string | null // 完整评分规则含分值（仅管理员可见）
  isEnabled: boolean
  sortOrder: number
  uploadHint: string       // 附件上传提示语
  extraFields: DwFieldConfigVO[]  // 动态扩展字段定义，见第七节
}
```

**渲染要点：**
- `scoreDesc` → 展示在每个模块标题旁边的「考核说明」折叠面板或 tooltip
- `scoreMax` / `scoreRule` → 仅管理员审核界面展示，机构填报页不展示
- `uploadHint` → 附件上传区域上方的灰色提示文字
- `isEnabled=false` 的模块前端不渲染

---

## 四、各模块填报接口

> 所有「保存」接口：`id` 为 null 时新增，有值时更新（upsert）。  
> 操作前需检查 `record.status`，非草稿状态（0/3）时禁用所有表单。

---

### 4.1 质控会议 `moduleKey: meeting`

**表单字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| meetingName | string | 会议名称 |
| meetingTime | date | 举办日期 `yyyy-MM-dd` |
| meetingForm | enum | `online`=线上 / `offline`=线下 |
| meetingContent | string(textarea) | 会议内容摘要 |
| attendeeCount | number | 参会人数 |
| attendanceRate | decimal | 参会率（0~100，%） |

**保存：** `POST /dw/record/meeting/save`

```json
{
  "id": null,
  "recordId": "xxx",
  "meetingName": "2025年第一次质控工作会议",
  "meetingTime": "2025-03-15",
  "meetingForm": "offline",
  "meetingContent": "讨论年度工作计划",
  "attendeeCount": 25,
  "attendanceRate": 95.5
}
```

**删除：** `POST /dw/record/meeting/delete/:id`

**附件槽位（slot）：**

| slot | 说明 | 允许格式 |
|------|------|---------|
| `minutes` | 会议纪要 | PDF、DOCX |
| `photo` | 现场照片 | JPG、JPEG、PNG、PDF |
| `signin` | 签到表 | JPG、JPEG、PNG、PDF、DOCX、XLSX |

---

### 4.2 质控培训 `moduleKey: training`

**表单字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| trainingName | string | 培训名称 |
| trainingTime | date | 举办日期 |
| trainingForm | enum | `online`=线上 / `offline`=线下 |
| trainingContent | string(textarea) | 培训内容摘要 |
| attendeeCount | number | 参培人数 |
| coverageRate | decimal | 覆盖率（%） |

**保存：** `POST /dw/record/training/save`  
**删除：** `POST /dw/record/training/delete/:id`

**附件槽位：**

| slot | 说明 | 允许格式 |
|------|------|---------|
| `material` | 培训材料 | PDF、DOCX |
| `photo` | 现场照片 | JPG、JPEG、PNG、PDF |

---

### 4.3 质控指导 `moduleKey: guidance`

**表单字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| guidanceTime | date | 指导日期 |
| guidanceForm | enum | `online`=线上 / `onsite`=现场 |
| guidanceContent | string(textarea) | 指导内容摘要 |
| cityCenterCount | number(自动) | 市级质控中心数（树勾选后自动填入） |
| cityCenterIds | string(自动) | 市级中心节点ID数组字符串 `"[101,103]"` |
| countyCenterCount | number(自动) | 县级质控中心数（树勾选后自动填入） |
| countyCenterIds | string(自动) | 县级中心节点ID数组字符串 `"[20101,20205]"` |
| hospitalCount | number | 医疗机构数（手动填写） |

**⚠️ 校验规则：** `cityCenterCount + countyCenterCount + hospitalCount` 之和必须 > 0

#### 地区树交互

从 `GET /dw/config/guidance/regions` 获取：

```ts
interface GuidanceRegionsVO {
  cityTree: RegionNodeVO[]    // 11个市级叶节点，用于市级质控中心勾选
  countyTree: RegionNodeVO[]  // 11个市节点，每个含 children（区县）
}

interface RegionNodeVO {
  id: number
  name: string
  children: RegionNodeVO[] | null
}
```

**`cityTree`（省→市级质控中心）：**
- 渲染为**平铺复选框组**（11个），如：☑ 杭州市级  ☑ 宁波市级 ...
- `cityCenterCount = 选中节点数`
- `cityCenterIds = JSON.stringify(选中节点id数组)`

**`countyTree`（省→市→区县）：**
- 渲染为**两级树形选择器**（`el-tree` 配 `show-checkbox`、`check-strictly: false` 父子联动）
- `countyCenterCount = 被勾选的叶节点（区县，children=null）数量`
  ```js
  const checkedNodes = tree.getCheckedNodes()
  const leafNodes = checkedNodes.filter(n => !n.children || n.children.length === 0)
  countyCenterCount = leafNodes.length
  countyCenterIds = JSON.stringify(leafNodes.map(n => n.id))
  ```

**回显（机构填报页）：** 读取 `guidance.cityCenterIds` / `countyCenterIds`（JSON 字符串）反序列化后回填树的勾选状态。

**只读 / 审核视图（管理端）：**
- 市级中心：直接展示 `cityCenterNames`（平铺字符串数组，后端已反查）
- 县级中心：推荐使用 `countyCenterGroups` 按所属市分组展示，格式示例：
  ```
  杭州市：上城区、西湖区
  宁波市：海曙区、鄞州区
  湖州市：吴兴区、南浔区
  ```
  如需平铺也可使用 `countyCenterNames`（兼容字段）。

**保存：** `POST /dw/record/guidance/save`  
**删除：** `POST /dw/record/guidance/delete/:id`

**附件槽位：**

| slot | 说明 | 允许格式 |
|------|------|---------|
| `evidence` | 佐证材料 | PDF、DOCX |

---

### 4.4 质控调研 `moduleKey: survey`

**表单字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| surveyTime | date | 调研日期 |
| surveyTarget | string | 调研对象 |
| surveyType | enum | `baseline`=基线调研 / `special`=专项调研 |
| surveyForm | enum | `online`=线上 / `offline`=线下 |
| surveyContent | string(textarea) | 调研内容摘要 |

**保存：** `POST /dw/record/survey/save`  
**删除：** `POST /dw/record/survey/delete/:id`

**附件槽位：**

| slot | 说明 | 允许格式 |
|------|------|---------|
| `report` | 调研报告 | PDF、DOCX |
| `photo` | 现场照片 | JPG、JPEG、PNG、PDF |

---

### 4.5 年度工作落实推进 `moduleKey: annual_work`

纯附件模块，无表单，只上传佐证材料。

**附件槽位：**

| slot | 说明 |
|------|------|
| `evidence` | 佐证材料（需加盖公章，PDF/DOCX） |

---

### 4.6 信息化建设 `moduleKey: it_construction`

纯附件模块。

**附件槽位：**

| slot | 说明 |
|------|------|
| `evidence` | 佐证材料（需加盖公章，PDF/DOCX） |

---

### 4.7 工作计划总结 `moduleKey: work_plan`

纯附件模块，但有两个独立槽位。

**附件槽位：**

| slot | 说明 |
|------|------|
| `plan` | 年度工作计划（PDF/DOCX，需加盖公章） |
| `summary` | 年度工作总结（PDF/DOCX，需加盖公章） |

---

### 4.8 行政指令响应与传达 `moduleKey: admin_response`

纯附件模块。

**附件槽位：**

| slot | 说明 |
|------|------|
| `evidence` | 响应传达佐证材料（PDF/DOCX，需加盖公章） |

---

### 4.9 质控活动报备 `moduleKey: activity_report`

纯附件模块，两个独立槽位。

**附件槽位：**

| slot | 说明 | 允许格式 |
|------|------|---------|
| `pre_report` | 事前报备截图 | JPG、JPEG、PNG、PDF |
| `post_report` | 事后报备截图 | JPG、JPEG、PNG、PDF |

---

### 4.10 经费执行 `moduleKey: funding`

纯表单模块（第四季度填写），无附件。

**表单字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| fiscalHasFund | boolean | 是否有财政专项经费拨款 |
| fiscalExecutionRate | decimal | 财政专项经费执行率（%，fiscalHasFund=true 时必填） |
| hospitalHasFund | boolean | 是否有医院配套经费 |
| hospitalExecutionRate | decimal | 医院配套经费执行率（%，hospitalHasFund=true 时必填） |

**保存：** `POST /dw/record/funding/save`（同一 record 只有一份，重复调用覆盖）

---

### 4.11 加分项-丛书/指南 `moduleKey: bonus_pub`

**保存：** `POST /dw/record/bonus/save`，`bonusType: "publication"`

**表单字段（publication 类型）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| pubName | string | 丛书/指南名称 |
| pubCategory | enum | `book_guide_consensus`=丛书/指南/共识 / `standard_norm`=标准/规范 |
| pubDate | date | 出版日期（2024-2025年期间） |

**附件槽位：**

| slot | 说明 |
|------|------|
| `evidence` | 出版证明（PDF/DOCX） |

---

### 4.12 加分项-技能竞赛 `moduleKey: bonus_comp`

**保存：** `POST /dw/record/bonus/save`，`bonusType: "competition"`

**表单字段（competition 类型）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| compName | string | 竞赛名称 |
| compSponsor | enum | `provincial_joint`=省总工会+省卫健委联合主办 / `other`=其他形式 |
| compDate | date | 举办日期（2024-2025年期间） |

**附件槽位：**

| slot | 说明 |
|------|------|
| `evidence` | 竞赛证明（PDF/DOCX） |

---

## 五、附件上传接口

```
POST /dw/record/attachment/upload
Content-Type: multipart/form-data
```

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| recordId | Long | ✅ | 填报记录 ID |
| moduleType | string | ✅ | 模块标识（同 moduleKey） |
| subRecordId | Long | 条件 | 多条记录型模块（meeting/training/guidance/survey/bonus）传子记录 ID；纯上传/单条型模块传 null |
| slot | string | ✅ | 附件槽位（见各模块说明） |
| file | File | ✅ | 文件二进制 |

**响应：**
```json
{
  "code": 200,
  "data": {
    "id": "xxx",
    "fileUrl": "https://...",
    "fileName": "会议纪要.pdf",
    "fileSize": 102400,
    "slot": "minutes"
  }
}
```

**删除：** `POST /dw/record/attachment/delete/:id`

---

## 六、DwRecordDetailVO 完整结构

```ts
interface DwRecordDetailVO {
  recordId: string
  taskId: string
  taskName: string
  orgName: string
  status: 0 | 1 | 2 | 3
  auditRemark: string | null

  // 多条记录型
  meetings:  DwMeetingVO[]
  trainings: DwTrainingVO[]
  guidances: DwGuidanceVO[]
  surveys:   DwSurveyVO[]

  // 纯上传型
  annualWorkFiles:      DwAttachmentVO[]
  itConstructionFiles:  DwAttachmentVO[]
  workPlanFiles:        { plan: DwAttachmentVO[], summary: DwAttachmentVO[] }
  adminResponseFiles:   DwAttachmentVO[]
  activityReportFiles:  { pre_report: DwAttachmentVO[], post_report: DwAttachmentVO[] }

  // 纯上传型扩展字段值（record 级）
  annualWorkExtra:       Record<string, string>
  itConstructionExtra:   Record<string, string>
  workPlanExtra:         Record<string, string>
  adminResponseExtra:    Record<string, string>
  activityReportExtra:   Record<string, string>

  // 表单型
  funding: DwFunding | null
  fundingExtra: Record<string, string>
  bonuses: DwBonusVO[]
}

interface DwMeetingVO {
  id: string
  meetingName: string
  meetingTime: string       // yyyy-MM-dd
  meetingForm: 'online' | 'offline'
  meetingContent: string
  attendeeCount: number
  attendanceRate: number
  minutes: DwAttachmentVO[]
  photos:  DwAttachmentVO[]
  signins: DwAttachmentVO[]
  extraValues: Record<string, string>  // 动态扩展字段值
}

interface DwGuidanceVO {
  id: string
  guidanceTime: string
  guidanceForm: 'online' | 'onsite'
  guidanceContent: string
  cityCenterCount: number
  cityCenterIds: string          // "[101,103]" — 供回显树勾选状态
  cityCenterNames: string[]      // ["杭州市级","温州市级"] — 平铺名称，供只读展示
  countyCenterCount: number
  countyCenterIds: string        // "[20101,20205]" — 供回显树勾选状态
  countyCenterNames: string[]    // ["上城区","秀洲区"] — 平铺名称，兼容保留
  countyCenterGroups: CountyCenterGroupVO[]  // 按所属市分组，管理端推荐使用此字段展示
  hospitalCount: number
  evidences: DwAttachmentVO[]
  extraValues: Record<string, string>
}

/** 县级质控中心按所属市分组（仅用于管理端展示） */
interface CountyCenterGroupVO {
  cityName: string    // 所属市，如 "杭州市"
  counties: string[]  // 该市下被选中的区县名称列表，如 ["上城区", "西湖区"]
}

// TrainingVO / SurveyVO 结构类似，参考模块字段定义
```

---

## 七、动态扩展字段（extraFields / extraValues）

管理员可在后台为任意模块新增自定义字段（无需改代码）。

### 7.1 读取字段定义

从 `GET /dw/config/modules` 返回的 `extraFields` 数组：

```ts
interface DwFieldConfigVO {
  id: string
  fieldKey: string     // 字段唯一标识，如 "budget_amount"
  fieldName: string    // 展示名，如 "经费金额"
  fieldType: 'text' | 'number' | 'enum' | 'checkbox'
  fieldOptions: string | null  // enum/checkbox 时为 JSON数组字符串 '["线上","线下"]'
  isRequired: boolean
  sortOrder: number
  placeholder: string | null
}
```

### 7.2 渲染规则

| fieldType | 渲染组件 | 值格式 |
|-----------|---------|--------|
| `text` | 单行/多行文本输入 | 字符串 |
| `number` | 数字输入框 | 数字字符串，如 `"3500"` |
| `enum` | 单选（Radio/Select） | 选中项字符串，如 `"线上"` |
| `checkbox` | 多选（Checkbox Group） | JSON数组字符串，如 `'["线上","专科医院"]'` |

### 7.3 保存扩展字段值

```
POST /dw/config/field/values/save
```

```json
{
  "recordId": "xxx",
  "moduleKey": "meeting",
  "subRecordId": "yyy",       // 多条记录型传子记录ID，纯上传/单条型传 null
  "values": {
    "budget_amount": "3500",
    "target_group": "[\"线上\",\"专科医院\"]"
  }
}
```

### 7.4 读取已保存值

从 `DwRecordDetailVO` 中各子记录 VO 的 `extraValues` 字段读取：
```js
const val = meetingVO.extraValues['budget_amount']  // "3500"
```

---

## 八、提交与审核

### 机构提交

```
POST /dw/record/submit/:recordId
```

提交前前端自行校验（后端也会拦截）：
- 各已填模块至少有对应附件
- 质控指导：`cityCenterCount + countyCenterCount + hospitalCount > 0`

### 管理员审核

```
POST /dw/record/audit/:recordId?result=1&remark=xxx
```

- `result=1`：通过，`status → 2`
- `result=0`：驳回，`status → 3`，`remark` 必填（告知驳回原因）

驳回后机构可重新编辑，再次调用 submit 提交。

---

## 九、任务列表路由分流

```js
// 机构端任务列表
GET /wr/task/active

// 根据 taskType 路由
tasks.forEach(task => {
  if (task.taskType === 'daily_work') {
    router.push(`/dw/record/init/${task.id}`)  // 日常工作填报页
  } else {
    router.push(`/wr/record/${task.id}`)        // 普通表单填报页
  }
})
```

---

## 附录 A：moduleKey 完整列表

| moduleKey | 模块名 | 类型 |
|-----------|--------|------|
| `meeting` | 质控会议 | 多条记录 |
| `training` | 质控培训 | 多条记录 |
| `guidance` | 质控指导 | 多条记录 |
| `survey` | 质控调研（检查） | 多条记录 |
| `annual_work` | 年度工作落实推进 | 纯上传 |
| `it_construction` | 信息化建设 | 纯上传 |
| `work_plan` | 工作计划总结 | 纯上传（双槽） |
| `admin_response` | 行政指令响应与传达 | 纯上传 |
| `activity_report` | 质控活动报备 | 纯上传（双槽） |
| `funding` | 经费执行 | 纯表单 |
| `bonus_pub` | 加分项-丛书/指南 | 多条记录（含附件） |
| `bonus_comp` | 加分项-技能竞赛 | 多条记录（含附件） |

## 附录 B：常见错误码

| code | 含义 | 处理建议 |
|------|------|---------|
| 400 | 参数校验失败（如指导人数全为0） | 展示 `message` 字段 |
| 401 | 未登录或 Token 过期 | 跳转登录页 |
| 403 | 权限不足或记录不属于本机构 | 提示无权限 |
| 404 | 任务/记录不存在 | 提示数据不存在 |
| 4033 | 附件数量不足最低要求 | 提示须上传附件 |
| 4034 | 附件数量超出上限 | 提示数量超限 |
| 4035 | 文件格式不允许 | 提示允许的格式列表 |
