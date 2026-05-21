package com.kxhospital.wreport.pojo.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.kxhospital.wreport.entity.*;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 日常工作填报记录完整详情
 * 聚合 wr_record 基础信息 + 各模块填报数据 + 附件 + 动态扩展字段值
 *
 * extraValues 说明：
 *   key   = field_key（对应 DwFieldConfig.fieldKey）
 *   value = 字段值字符串（checkbox 类型为 JSON 数组字符串）
 *   前端根据 GET /dw/config/modules 返回的 extraFields 定义来决定如何渲染
 */
@Data
public class DwRecordDetailVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long recordId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long taskId;
    private String taskName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orgId;
    private String orgName;
    /** normal / daily_work */
    private String taskType;
    /** 统计年度，daily_work 任务时有值，如 "2025" */
    private String statYear;
    /** 统计季度 1-4，null 表示年度任务 */
    private Integer statQuarter;
    /** 0=草稿 1=已提交 2=已通过 3=已驳回 */
    private Integer status;
    private String auditRemark;
    /**
     * 机构端：任务已关闭、记录已提交/通过时为 true，前端整体灰态只读；
     * 年度汇总接口中强制为 true（forceReadOnly）；管理员查看时始终为 false。
     */
    private Boolean readOnly;
    /** 该任务启用的模块 key 列表，前端据此隐藏未勾选模块 */
    private List<String> enabledModuleKeys;

    // ── 多条记录型模块 ──────────────────────────────
    /** 各模块总体自评分（record 级，key=moduleKey，如 meeting/training/.../bonus_pub/bonus_comp） */
    private Map<String, java.math.BigDecimal> moduleSelfScores;
    /** 质控会议：按开始时间倒序（新→旧）；子项含 startYearQuarter、quarterIndex 供分季度配色 */
    private List<DwMeetingVO>  meetings;
    /** 质控培训：排序与配色规则同 meetings */
    private List<DwTrainingVO> trainings;
    /** 质控指导：排序与配色规则同 meetings */
    private List<DwGuidanceVO> guidances;
    /** 质控调研：排序与配色规则同 meetings */
    private List<DwSurveyVO>   surveys;

    // ── 纯上传模块（附件按 slot 分组） ──────────────
    private List<DwAttachmentVO>              annualWorkFiles;
    private List<DwAttachmentVO>              itConstructionFiles;
    private Map<String, List<DwAttachmentVO>> workPlanFiles;
    private List<DwAttachmentVO>              adminResponseFiles;
    private Map<String, List<DwAttachmentVO>> activityReportFiles;

    // ── 新增纯上传模块 ────────────────────────────
    /** 1.3 质控指标数据库建设 */
    private List<DwAttachmentVO> indicatorDbFiles;
    /** 4.1 质控指标监测 */
    private List<DwAttachmentVO> indicatorMonitorFiles;
    /** 4.2 国家质量安全报告分册（按年度 y2026 等 slot 分组） */
    private Map<String, List<DwAttachmentVO>> nationalReportFiles;
    /** 4.3 浙江省质量安全报告（按年度 slot 分组） */
    private Map<String, List<DwAttachmentVO>> provReportFiles;
    /** 加分项3：行政指令性任务（双槽：national_task / prov_task） */
    private Map<String, List<DwAttachmentVO>> bonusAdminFiles;

    // ── 纯上传模块扩展字段值（record 级） ────────────
    private Map<String, String> annualWorkExtra;
    private Map<String, String> itConstructionExtra;
    private Map<String, String> workPlanExtra;
    private Map<String, String> adminResponseExtra;
    private Map<String, String> activityReportExtra;
    private Map<String, String> indicatorDbExtra;
    private Map<String, String> indicatorMonitorExtra;
    private Map<String, String> nationalReportExtra;
    private Map<String, String> provReportExtra;
    private Map<String, String> bonusAdminExtra;

    // ── 表单型模块 ──────────────────────────────────
    private DwFunding       funding;
    private Map<String, String> fundingExtra;
    private List<DwBonusVO> bonuses;

    // ── 2.1 三级质控网络完善（单条树选择） ────────────
    private DwNetworkBuildVO networkBuild;

    // ── 质控数据分析报告（多条记录型，季度可填） ────────
    /** 按报告日期倒序；季度任务条目含 startYearQuarter/quarterIndex */
    private List<DwDataAnalysisVO> dataAnalysisReports;

    /**
     * 仅年度任务（statQuarter=null）时有值。
     * 嵌入该年度 Q4→Q1 各季度只读快照，前端无需额外调用 year-summary 接口，
     * 也无需自行合并/排序，直接按此列表顺序渲染即可（readOnly 恒为 true）。
     */
    private List<DwYearQuarterRecordVO> quarterlySnapshots;

    // ── 嵌套 VO ────────────────────────────────────

    @Data
    public static class DwMeetingVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String meetingName;
        private String meetingStartDate;
        private String meetingStartHalf;
        private String meetingEndDate;
        private String meetingEndHalf;
        private String meetingForm;
        private String meetingContent;
        /** 开始日期所属自然季度，如 2025-Q2，与 quarterIndex 一致，供前端配色/分组 */
        private String startYearQuarter;
        /** 1–4，对应 Q1–Q4（按开始日期的月份） */
        private Integer quarterIndex;
        private Integer attendeeCount;
        private java.math.BigDecimal attendanceRate;
        private List<DwAttachmentVO> minutes;
        private List<DwAttachmentVO> photos;
        private List<DwAttachmentVO> signins;
        /** 动态扩展字段值 { fieldKey -> fieldValue } */
        private Map<String, String> extraValues;
    }

    @Data
    public static class DwTrainingVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String trainingName;
        private String trainingStartDate;
        private String trainingStartHalf;
        private String trainingEndDate;
        private String trainingEndHalf;
        private String trainingForm;
        private String trainingContent;
        private String startYearQuarter;
        private Integer quarterIndex;
        /** 培训人数 */
        private Integer trainingPeopleCount;
        private List<DwAttachmentVO> materials;
        private List<DwAttachmentVO> photos;
        private Map<String, String> extraValues;
    }

    @Data
    public static class DwGuidanceVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String guidanceStartDate;
        private String guidanceStartHalf;
        private String guidanceEndDate;
        private String guidanceEndHalf;
        private String guidanceForm;
        private String guidanceContent;
        private String startYearQuarter;
        private Integer quarterIndex;
        private Integer cityCenterCount;
        /** 勾选的市级中心 ID 列表（供前端回显树选择器） */
        private String cityCenterIds;
        /** 勾选的市级中心名称列表（平铺，供快速展示） */
        private List<String> cityCenterNames;
        private Integer countyCenterCount;
        /** 勾选的县级中心 ID 列表（供前端回显树选择器） */
        private String countyCenterIds;
        /** 勾选的县级中心名称列表（平铺，保留兼容） */
        private List<String> countyCenterNames;
        /** 勾选的县级中心按所属市分组（供管理端分组展示） */
        private List<CountyCenterGroupVO> countyCenterGroups;
        private Integer hospitalCount;
        private List<DwAttachmentVO> evidences;
        private Map<String, String> extraValues;
    }

    /** 县级质控中心按所属市分组 */
    @Data
    public static class CountyCenterGroupVO {
        /** 所属市名称，如"杭州市" */
        private String cityName;
        /** 该市下被选中的区县名称列表，如["上城区","西湖区"] */
        private List<String> counties;
    }

    @Data
    public static class DwSurveyVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String surveyStartDate;
        private String surveyStartHalf;
        private String surveyEndDate;
        private String surveyEndHalf;
        private String surveyTarget;
        private String surveyType;
        private String surveyForm;
        private String surveyContent;
        private String startYearQuarter;
        private Integer quarterIndex;
        private List<DwAttachmentVO> reports;
        private List<DwAttachmentVO> photos;
        private Map<String, String> extraValues;
    }

    @Data
    public static class DwBonusVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String bonusType;
        private String pubName;
        private String pubCategory;
        private String pubDate;
        private String compName;
        private String compSponsor;
        private String compStartDate;
        private String compStartHalf;
        private String compEndDate;
        private String compEndHalf;
        private List<DwAttachmentVO> evidences;
        private Map<String, String> extraValues;
    }

    /** 质控数据分析报告（多条，季度可填） */
    @Data
    public static class DwDataAnalysisVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String reportName;
        private String reportDate;
        /** 开始日期所属自然季度，如 2025-Q2；仅季度任务填报的条目有值 */
        private String startYearQuarter;
        /** 1–4；仅季度任务填报的条目有值 */
        private Integer quarterIndex;
        private List<DwAttachmentVO> files;
    }

    /** 2.1 三级质控网络完善（单条，复用指导模块的树回显结构） */
    @Data
    public static class DwNetworkBuildVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private Integer cityCenterCount;
        /** 已勾选市级中心 ID 列表（JSON 数组字符串，供前端回显树选择器） */
        private String cityCenterIds;
        /** 已勾选市级中心名称列表 */
        private List<String> cityCenterNames;
        private Integer countyCenterCount;
        /** 已勾选区县中心 ID 列表（JSON 数组字符串） */
        private String countyCenterIds;
        /** 已勾选区县中心名称列表（平铺） */
        private List<String> countyCenterNames;
        /** 已勾选区县中心按所属市分组（供管理端分组展示） */
        private List<CountyCenterGroupVO> countyCenterGroups;
        private java.math.BigDecimal selfScore;
        /** 佐证材料 */
        private List<DwAttachmentVO> evidences;
        private Map<String, String> extraValues;
    }
}
