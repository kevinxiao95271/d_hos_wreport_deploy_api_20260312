package com.kxhospital.wreport.pojo.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DwFundingRequest {
    private Long recordId;
    private Boolean fiscalHasFund;
    private BigDecimal fiscalExecutionRate;
    private Boolean hospitalHasFund;
    private BigDecimal hospitalExecutionRate;
}
