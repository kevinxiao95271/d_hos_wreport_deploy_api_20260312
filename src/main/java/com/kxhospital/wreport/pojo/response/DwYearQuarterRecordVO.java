package com.kxhospital.wreport.pojo.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.List;

/**
 * 某统计年度内单个季度日常工作任务 + 机构填报快照（年度汇总用，只读）。
 * 管理端在查看年度任务时，旁边展示各季度已审核通过的数据参考。
 */
@Data
public class DwYearQuarterRecordVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long taskId;
    private String taskName;
    /** wr_task.status：0=草稿 1=进行中 2=已关闭 */
    private Integer taskStatus;
    private String statYear;
    /** 统计季度 1–4；null 表示全年度任务（历史数据兼容） */
    private Integer statQuarter;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long recordId;
    /** wr_record.status；无记录时为 null */
    private Integer recordStatus;
    /** 年度汇总接口中恒为 true，前端整体灰态只读 */
    private Boolean readOnly;
    /** 该任务启用的模块 key 列表 */
    private List<String> enabledModuleKeys;
    /** 无填报记录时为 null */
    private DwRecordDetailVO detail;
}
