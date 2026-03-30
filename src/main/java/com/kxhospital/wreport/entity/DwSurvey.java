package com.kxhospital.wreport.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 质控调研记录（多条，关联 wr_record） */
@Data
@TableName("dw_survey")
public class DwSurvey implements Serializable {
    @TableId(value = "id", type = IdType.ASSIGN_ID) private Long id;
    private Long recordId;
    private LocalDate surveyStartDate;
    /** AM/PM */
    private String surveyStartHalf;
    private LocalDate surveyEndDate;
    /** AM/PM */
    private String surveyEndHalf;
    private String surveyTarget;
    /** 'baseline'=基线调研  'special'=专项调研 */
    private String surveyType;
    /** 'online'=线上  'offline'=线下 */
    private String surveyForm;
    private String surveyContent;
    @TableLogic(value = "0", delval = "1") private Integer delFlag;
    @TableField(fill = FieldFill.INSERT) private Long createUser;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
}
