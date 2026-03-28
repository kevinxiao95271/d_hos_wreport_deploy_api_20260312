package com.kxhospital.wreport.pojo.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskAddRequest {
    @NotBlank(message = "任务名称不能为空")
    private String taskName;
    /** 普通模板任务必填；日常工作任务（taskType='daily_work'）时可为 null */
    private Long templateId;
    /** 'normal'=普通模板任务（默认）  'daily_work'=日常工作模块任务 */
    private String taskType;
    private String statYear;
    private LocalDateTime deadline;
    private String remark;
    private List<Long> orgIds;
}
