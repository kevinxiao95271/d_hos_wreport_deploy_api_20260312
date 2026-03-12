package com.kxhospital.wreport.pojo.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class TaskAddRequest {
    @NotBlank(message = "任务名称不能为空")
    private String taskName;
    @NotNull(message = "模板ID不能为空")
    private Long templateId;
    private String statYear;
    private LocalDateTime deadline;
    private String remark;
}
