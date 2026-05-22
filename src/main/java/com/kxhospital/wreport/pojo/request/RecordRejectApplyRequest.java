package com.kxhospital.wreport.pojo.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class RecordRejectApplyRequest {

    @NotNull(message = "recordId 不能为空")
    private Long recordId;

    @NotBlank(message = "申请原因不能为空")
    private String reason;
}
