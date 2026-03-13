package com.kxhospital.wreport.pojo.response;

import com.kxhospital.wreport.entity.WrRecord;
import com.kxhospital.wreport.entity.WrRecordValue;
import com.kxhospital.wreport.entity.WrTemplateItem;
import com.kxhospital.wreport.entity.WrTemplateRow;
import lombok.Data;

import java.util.List;

@Data
public class RecordDetailVO {
    private WrRecord             record;
    private List<WrRecordValue>  values;
    private List<AttachmentVO>   attachments;
    private String               statusLabel;
    /**
     * 本条记录实际涉及的列定义（由 values 中出现的 itemId 反推）：
     * - 附件2（标准表头）：返回全部 48 个叶子列定义（含层级，前端可构建三级表头）
     * - 附件3（矩阵勾选）：只返回本机构对应的那一列，其余 26 列不返回
     * 前端无需再单独请求 /wr/template/items，也无需过滤"哪列是自己的"
     */
    private List<WrTemplateItem> items;
    /**
     * 行定义（矩阵类模板有值，标准类为空列表）
     * 前端无需再单独请求 /wr/template/rows
     */
    private List<WrTemplateRow>  rows;
}
