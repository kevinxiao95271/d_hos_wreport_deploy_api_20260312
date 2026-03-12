package com.kxhospital.wreport.pojo.response;

import com.kxhospital.wreport.entity.WrRecord;
import com.kxhospital.wreport.entity.WrRecordValue;
import com.kxhospital.wreport.entity.WrTemplateRow;
import lombok.Data;

import java.util.List;

@Data
public class RecordDetailVO {
    private WrRecord            record;
    private List<WrRecordValue> values;
    private List<AttachmentVO>  attachments;
    private String              statusLabel;
    /** 模板行定义（矩阵类模板有值，标准类为空列表） */
    private List<WrTemplateRow> rows;
}
