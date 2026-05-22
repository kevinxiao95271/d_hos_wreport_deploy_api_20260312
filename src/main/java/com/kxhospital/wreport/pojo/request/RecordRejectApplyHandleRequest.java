package com.kxhospital.wreport.pojo.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class RecordRejectApplyHandleRequest {

    @NotNull(message = "recordId 不能为空")
    private Long recordId;

    /**
     * true=同意驳回（记录变为已驳回），false=拒绝申请
     */
    @NotNull(message = "approved 不能为空")
    private Boolean approved;

    private String handleRemark;

    /**
     * 同意驳回时可指定重提截止时间；不填则默认 +7 天
     */
    private LocalDateTime resubmitDeadline;
}
