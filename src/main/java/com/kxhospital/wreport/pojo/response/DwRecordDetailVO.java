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
    private String orgName;
    /** 0=草稿 1=已提交 2=已通过 3=已驳回 */
    private Integer status;
    private String auditRemark;

    // ── 多条记录型模块 ──────────────────────────────
    private List<DwMeetingVO>  meetings;
    private List<DwTrainingVO> trainings;
    private List<DwGuidanceVO> guidances;
    private List<DwSurveyVO>   surveys;

    // ── 纯上传模块（附件按 slot 分组） ──────────────
    private List<DwAttachmentVO>              annualWorkFiles;
    private List<DwAttachmentVO>              itConstructionFiles;
    private Map<String, List<DwAttachmentVO>> workPlanFiles;
    private List<DwAttachmentVO>              adminResponseFiles;
    private Map<String, List<DwAttachmentVO>> activityReportFiles;

    // ── 纯上传模块扩展字段值（record 级） ────────────
    private Map<String, String> annualWorkExtra;
    private Map<String, String> itConstructionExtra;
    private Map<String, String> workPlanExtra;
    private Map<String, String> adminResponseExtra;
    private Map<String, String> activityReportExtra;

    // ── 表单型模块 ──────────────────────────────────
    private DwFunding       funding;
    private Map<String, String> fundingExtra;
    private List<DwBonusVO> bonuses;

    // ── 嵌套 VO ────────────────────────────────────

    @Data
    public static class DwMeetingVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String meetingName;
        private String meetingTime;
        private String meetingForm;
        private String meetingContent;
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
        private String trainingTime;
        private String trainingForm;
        private String trainingContent;
        private Integer attendeeCount;
        private java.math.BigDecimal coverageRate;
        private List<DwAttachmentVO> materials;
        private List<DwAttachmentVO> photos;
        private Map<String, String> extraValues;
    }

    @Data
    public static class DwGuidanceVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String guidanceTime;
        private String guidanceForm;
        private String guidanceContent;
        private Integer cityCenterCount;
        private String cityCenterIds;
        private Integer countyCenterCount;
        private String countyCenterIds;
        private Integer hospitalCount;
        private List<DwAttachmentVO> evidences;
        private Map<String, String> extraValues;
    }

    @Data
    public static class DwSurveyVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String surveyTime;
        private String surveyTarget;
        private String surveyType;
        private String surveyForm;
        private String surveyContent;
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
        private String compDate;
        private List<DwAttachmentVO> evidences;
        private Map<String, String> extraValues;
    }
}
