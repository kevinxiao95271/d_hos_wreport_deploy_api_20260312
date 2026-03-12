package com.kxhospital.wreport.pojo.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttachmentVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long   id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long   recordId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long   itemId;
    private String attachName;
    private String attachPath;
    private Long   attachSize;
    private String attachType;
    private LocalDateTime createTime;
}
