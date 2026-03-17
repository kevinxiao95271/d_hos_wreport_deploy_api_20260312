package com.kxhospital.wreport.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("wr_dict_item")
public class WrDictItem implements Serializable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long dictTypeId;

    /** 展示文字，如 "是" */
    private String itemLabel;

    /** 存储值，如 "1" */
    private String itemValue;

    private Integer sortNum;

    /** 1=启用 0=禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)      private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    @TableLogic(value = "0", delval = "1")   private Integer delFlag;
}
