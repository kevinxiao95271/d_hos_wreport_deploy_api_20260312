package com.kxhospital.wreport.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dw_task_module_scope")
public class DwTaskModuleScope {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private Long taskId;
    private String moduleKey;
    private Integer sortOrder;
    private LocalDateTime createTime;
}
