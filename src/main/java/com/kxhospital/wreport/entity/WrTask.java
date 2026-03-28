package com.kxhospital.wreport.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("wr_task")
public class WrTask implements Serializable {
    @TableId(value = "id", type = IdType.ASSIGN_ID) private Long id;
    private String taskName;
    private Long templateId;
    /** 任务类型：'normal'=普通模板任务  'daily_work'=日常工作模块任务 */
    private String taskType;
    private String statYear;
    private LocalDateTime deadline;
    private Integer status;
    private String remark;
    @TableField(fill = FieldFill.INSERT) private Long createUser;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private Long updateUser;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    @TableLogic(value = "0", delval = "1") private Integer delFlag;
}
