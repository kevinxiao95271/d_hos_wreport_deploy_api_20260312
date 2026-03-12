package com.kxhospital.wreport.pojo.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.kxhospital.wreport.entity.WrRecord;
import com.kxhospital.wreport.entity.WrRecordValue;
import lombok.Data;

import java.util.List;

@Data
public class RecordDetailVO {
    private WrRecord record;
    private List<WrRecordValue> values;
    private List<AttachmentVO>  attachments;
}
