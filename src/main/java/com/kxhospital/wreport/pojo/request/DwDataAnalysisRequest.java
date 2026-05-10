package com.kxhospital.wreport.pojo.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DwDataAnalysisRequest {
    /** 非空时为更新，null 时为新增 */
    private Long      id;
    private Long      recordId;
    private String    reportName;
    private LocalDate reportDate;
}
