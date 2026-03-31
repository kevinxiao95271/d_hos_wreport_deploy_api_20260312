package com.kxhospital.wreport.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 经费执行（每个 wr_record 只有一条，第四季度填写）
 * 计分规则（仅管理员可见）：
 *   fiscalExecutionRate：≥90%→3分；<90%→0分
 *   hospitalExecutionRate：≥90%→3分；≥60%→2分；≥20%→1分；<20%→0分
 */
@Data
@TableName("dw_funding")
public class DwFunding implements Serializable {
    @TableId(value = "id", type = IdType.ASSIGN_ID) private Long id;
    private Long recordId;
    /** 财政专项拨款（万元），库列 fiscal_appropriation_wan */
    private BigDecimal fiscalAppropriationWan;
    /** 财政专项执行率（%） */
    private BigDecimal fiscalExecutionRate;
    /** 医院配套拨款（万元），库列 hospital_appropriation_wan */
    private BigDecimal hospitalAppropriationWan;
    /** 医院配套执行率（%） */
    private BigDecimal hospitalExecutionRate;
    @TableLogic(value = "0", delval = "1") private Integer delFlag;
    @TableField(fill = FieldFill.INSERT) private Long createUser;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
}
