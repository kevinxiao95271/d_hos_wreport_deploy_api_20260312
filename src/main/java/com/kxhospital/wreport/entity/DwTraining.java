package com.kxhospital.wreport.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 质控培训记录（多条，关联 wr_record） */
@Data
@TableName("dw_training")
public class DwTraining implements Serializable {
    @TableId(value = "id", type = IdType.ASSIGN_ID) private Long id;
    private Long recordId;
    private String trainingName;
    private LocalDate trainingStartDate;
    /** AM/PM */
    private String trainingStartHalf;
    private LocalDate trainingEndDate;
    /** AM/PM */
    private String trainingEndHalf;
    /** 'online'=线上  'offline'=线下 */
    private String trainingForm;
    private String trainingContent;
    private Integer attendeeCount;
    /** 培训覆盖率（%） */
    private BigDecimal coverageRate;
    @TableLogic(value = "0", delval = "1") private Integer delFlag;
    @TableField(fill = FieldFill.INSERT) private Long createUser;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
}
