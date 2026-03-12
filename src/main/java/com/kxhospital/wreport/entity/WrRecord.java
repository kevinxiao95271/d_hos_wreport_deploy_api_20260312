package com.kxhospital.wreport.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("wr_record")
public class WrRecord implements Serializable {
    @TableId(value = "id", type = IdType.ASSIGN_ID) private Long id;
    private Long taskId;
    private Long templateId;
    private Long orgId;
    private String orgName;
    private Integer status;
    private Long submitUser;
    private LocalDateTime submitTime;
    private Long auditUser;
    private LocalDateTime auditTime;
    private Integer auditResult;
    private String auditRemark;
    /**
     * 驳回后允许重提的截止时间
     * null = 不限期（管理员未指定）
     * 由 audit() 在驳回时自动设为 auditTime + 7天，也可由管理员手动指定
     */
    private LocalDateTime resubmitDeadline;
    @TableField(fill = FieldFill.INSERT) private Long createUser;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private Long updateUser;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    @TableLogic(value = "0", delval = "1") private Integer delFlag;
}
