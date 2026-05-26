package com.kxhospital.wreport.pojo.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据看板 — 按任务与机构维度统计各模块填报数量
 */
@Data
public class DwDashboardModuleStatsVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long taskId;

    private String taskName;

    private String statYear;

    private Integer statQuarter;

    /** 如：2025年Q1 / 2025年度任务 */
    private String taskPeriodLabel;

    /** null=所有；1=质控中心；2=技术指导中心 */
    private Integer orgCategory;

    private String orgCategoryLabel;

    private List<ModuleItem> modules;

    private List<OrgRow> orgs;

    @Data
    public static class ModuleItem {
        private String moduleKey;
        private String moduleName;
    }

    @Data
    public static class OrgRow {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long orgId;
        private String orgName;
        private Integer orgCategory;
        /** 该机构在本任务的上报记录 ID；未开始填报时为 null */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long recordId;
        /** moduleKey → 统计值（会议/培训等为条数；纯上传模块为 0/1 表示是否已填报） */
        private Map<String, Integer> moduleCounts = new LinkedHashMap<>();
    }
}
