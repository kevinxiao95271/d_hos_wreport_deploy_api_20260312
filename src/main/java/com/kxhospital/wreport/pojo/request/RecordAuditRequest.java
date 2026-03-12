package com.kxhospital.wreport.pojo.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class RecordAuditRequest {

    @NotNull(message = "recordId 不能为空")
    private Long recordId;

    /**
     * 1 = 审核通过，2 = 驳回
     */
    @NotNull(message = "auditResult 不能为空 (1=通过 2=驳回)")
    private Integer auditResult;

    private String auditRemark;

    /**
     * 仅驳回时有效：重提截止时间（可选）
     * - 不填：系统自动设为驳回时间 + 7 天
     * - 填写：使用指定时间（管理员手动延长/缩短重提窗口）
     */
    private LocalDateTime resubmitDeadline;
}
