package com.kxhospital.wreport.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 质控平台消息/待办表（qc_message，与项目 B 共用）
 */
@Data
@TableName("qc_message")
public class QcMessage {

    @TableId(value = "message_id", type = IdType.ASSIGN_ID)
    private Long messageId;

    private String title;
    private String content;

    @TableField("message_type")
    private Integer messageType;

    @TableField("business_type")
    private String businessType;

    @TableField("business_id")
    private Long businessId;

    @TableField("target_type")
    private Integer targetType;

    @TableField("target_id")
    private Long targetId;

    @TableField("send_user_id")
    private Long sendUserId;

    @TableField("send_user_name")
    private String sendUserName;

    @TableField("send_time")
    private LocalDateTime sendTime;

    @TableField("create_user")
    private Long createUser;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_user")
    private Long updateUser;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("del_flag")
    private String delFlag;
}
