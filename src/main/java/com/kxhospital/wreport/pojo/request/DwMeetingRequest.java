package com.kxhospital.wreport.pojo.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DwMeetingRequest {
    private Long id;           // 新增时为 null，编辑时传入
    private Long recordId;
    private String meetingName;
    private LocalDate meetingTime;
    /** 'online' | 'offline' */
    private String meetingForm;
    private String meetingContent;
    private Integer attendeeCount;
    private BigDecimal attendanceRate;
}
