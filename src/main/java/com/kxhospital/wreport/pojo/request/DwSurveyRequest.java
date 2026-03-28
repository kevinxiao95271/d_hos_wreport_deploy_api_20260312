package com.kxhospital.wreport.pojo.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DwSurveyRequest {
    private Long id;
    private Long recordId;
    private LocalDate surveyTime;
    private String surveyTarget;
    /** 'baseline' | 'special' */
    private String surveyType;
    /** 'online' | 'offline' */
    private String surveyForm;
    private String surveyContent;
}
