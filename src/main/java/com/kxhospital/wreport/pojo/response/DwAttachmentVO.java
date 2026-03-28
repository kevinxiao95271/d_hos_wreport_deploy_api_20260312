package com.kxhospital.wreport.pojo.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DwAttachmentVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String moduleType;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long subRecordId;
    private String slot;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String fileMime;
    private LocalDateTime createTime;
}
