package com.kxhospital.wreport.pojo.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DwMeetingRequest {
    private Long id;           // 新增时为 null，编辑时传入
    private Long recordId;
    private String meetingName;
    private LocalDate meetingStartDate;
    /** AM/PM */
    private String meetingStartHalf;
    private LocalDate meetingEndDate;
    /** AM/PM */
    private String meetingEndHalf;
    /** 'online' | 'offline' */
    private String meetingForm;
    private String meetingContent;
    private Integer attendeeCount;
    private BigDecimal attendanceRate;
    /** 填报者自评分（可为空） */
    private BigDecimal selfScore;
}
