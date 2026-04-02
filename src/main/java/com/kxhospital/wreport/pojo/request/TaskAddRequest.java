package com.kxhospital.wreport.pojo.request;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
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
    /**
     * 统计季度 1–4（Q1–Q4），仅 taskType=daily_work 时有意义。
     * 传 1–4 表示季度任务；不传（null）表示全年度任务。
     */
    @Min(value = 1, message = "统计季度须为 1–4")
    @Max(value = 4, message = "统计季度须为 1–4")
    private Integer statQuarter;
    private LocalDateTime deadline;
    private String remark;
    private List<Long> orgIds;
}
