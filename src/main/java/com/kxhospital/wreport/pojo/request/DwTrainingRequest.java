package com.kxhospital.wreport.pojo.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DwTrainingRequest {
    private Long id;
    private Long recordId;
    private String trainingName;
    private LocalDate trainingStartDate;
    /** AM/PM */
    private String trainingStartHalf;
    private LocalDate trainingEndDate;
    /** AM/PM */
    private String trainingEndHalf;
    /** 'online' | 'offline' */
    private String trainingForm;
    private String trainingContent;
    private Integer attendeeCount;
    private BigDecimal coverageRate;
}
