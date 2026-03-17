package com.kxhospital.wreport.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("wr_dict_type")
public class WrDictType implements Serializable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 字典编码，全局唯一，如 yes_no / meeting_form */
    private String dictCode;

    /** 字典名称，如 "是/否" */
    private String dictName;

    private String remark;

    /** 1=启用 0=禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)      private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    @TableLogic(value = "0", delval = "1")   private Integer delFlag;
}
