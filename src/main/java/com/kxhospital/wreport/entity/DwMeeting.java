package com.kxhospital.wreport.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 质控会议记录（多条，关联 wr_record） */
@Data
@TableName("dw_meeting")
public class DwMeeting implements Serializable {
    @TableId(value = "id", type = IdType.ASSIGN_ID) private Long id;
    private Long recordId;
    private String meetingName;
    private LocalDate meetingTime;
    /** 'online'=线上  'offline'=线下 */
    private String meetingForm;
    private String meetingContent;
    private Integer attendeeCount;
    /** 参会率（%） */
    private BigDecimal attendanceRate;
    @TableLogic(value = "0", delval = "1") private Integer delFlag;
    @TableField(fill = FieldFill.INSERT) private Long createUser;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
}
