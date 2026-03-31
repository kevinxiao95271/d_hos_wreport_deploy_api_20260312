package com.kxhospital.wreport.pojo.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DwFundingRequest {
    private Long recordId;
    /** 财政专项拨款（万元） */
    private BigDecimal fiscalAppropriationWan;
    private BigDecimal fiscalExecutionRate;
    /** 医院配套拨款（万元） */
    private BigDecimal hospitalAppropriationWan;
    private BigDecimal hospitalExecutionRate;
}
