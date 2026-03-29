package com.kxhospital.wreport.pojo.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.List;

/**
 * 日常工作任务 — 管理端汇总视图（GET /dw/record/admin/overview）
 */
@Data
public class DwAdminOverviewVO {

    // ── 状态汇总 ────────────────────────────────────────────────────────────
    private long total;       // 任务分配的机构总数
    private long notStarted;  // 尚未创建记录（未开始）
    private long draft;       // 草稿
    private long submitted;   // 已提交（待审核）
    private long approved;    // 已通过
    private long rejected;    // 已驳回

    // ── 各机构明细行 ─────────────────────────────────────────────────────────
    private List<OrgRow> orgRows;

    @Data
    public static class OrgRow {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long orgId;
        private String orgName;

        /** null 代表未开始 */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long recordId;

        /** null 代表未开始；0=草稿 1=已提交 2=已通过 3=已驳回 */
        private Integer status;

        /** 未开始/草稿/已提交/已通过/已驳回 */
        private String statusLabel;

        // ── 各模块数据量（填 0 = 未填写） ───────────────────────────────────
        private int meetingCount;
        private int trainingCount;
        private int guidanceCount;
        private int surveyCount;
        private boolean hasFunding;
        private int bonusCount;
    }
}
