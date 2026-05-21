package com.kxhospital.wreport.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 质控平台消息已读/已处理表（qc_message_read，与项目 B 共用） */
@Data
@TableName("qc_message_read")
public class QcMessageRead {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("message_id")
    private Long messageId;

    @TableField("user_id")
    private Long userId;

    @TableField("read_time")
    private LocalDateTime readTime;

    @TableField("process_status")
    private Integer processStatus;

    @TableField("process_time")
    private LocalDateTime processTime;
}
