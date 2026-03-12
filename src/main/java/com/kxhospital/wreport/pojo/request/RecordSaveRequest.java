package com.kxhospital.wreport.pojo.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class RecordSaveRequest {
    @NotNull(message = "任务ID不能为空")
    private Long taskId;
    /** 表单型（非表格），直接传 values */
    private List<CellValue> values;
    /** 表格型（多行），传 rows */
    private List<RowData> rows;

    @Data
    public static class CellValue {
        @NotNull private Long   itemId;
        private String value;
    }

    @Data
    public static class RowData {
        @NotNull private Integer rowIndex;
        private List<CellValue> cells;
    }
}
