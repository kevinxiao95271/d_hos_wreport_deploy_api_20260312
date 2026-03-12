package com.kxhospital.wreport.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("wr_attachment")
public class WrAttachment implements Serializable {
    @TableId(value = "id", type = IdType.ASSIGN_ID) private Long id;
    private Long recordId;
    private Long itemId;
    private Long fileId;
    private String attachName;
    private String attachPath;
    private Long attachSize;
    private String attachType;
    @TableField(fill = FieldFill.INSERT) private Long createUser;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableLogic(value = "0", delval = "1") private Integer delFlag;
}
