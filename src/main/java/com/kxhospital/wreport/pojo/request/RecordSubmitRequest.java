package com.kxhospital.wreport.pojo.request;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class RecordSubmitRequest {
    @NotNull(message = "recordId 不能为空")
    private Long recordId;
}
