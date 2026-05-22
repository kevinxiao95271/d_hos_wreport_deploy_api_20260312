package com.kxhospital.wreport.pojo.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "上报汇总统计")
@Data
public class RecordAggregateResponse {
    @Schema(description = "总记录数")
    private Long total;
    @Schema(description = "未提交/草稿数量")
    private Long draft;
    @Schema(description = "待审核数量")
    private Long submitted;
    @Schema(description = "已通过数量")
    private Long approved;
    @Schema(description = "已驳回数量")
    private Long rejected;
    @Schema(description = "待处理撤回申请数量")
    private Long pendingRejectApply;
}
