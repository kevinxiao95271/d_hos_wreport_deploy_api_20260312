package com.kxhospital.wreport.pojo.response;

import com.kxhospital.wreport.entity.WrTemplateItem;
import com.kxhospital.wreport.entity.WrTemplateRow;
import lombok.Data;

import java.util.List;

/**
 * 跨机构横向视图（crossview）响应结构。
 *
 * templateType = "standard"（附件2）时：
 *   使用 items + orgRows
 *   渲染方式：列=模板字段，行=每个机构一行
 *
 * templateType = "matrix"（附件3）时：
 *   使用 rows + numberItems + orgCols + matrixValues + numberValues
 *   渲染方式：列=各机构，行=地理层级行（含联动树）+ number 项追加行
 */
@Data
public class CrossViewVO {

    /** "standard" | "matrix" */
    private String templateType;

    // ═══════════════════════════════════════════════════
    //  标准模板（附件2）
    // ═══════════════════════════════════════════════════

    /** 选中展示的列定义（含 headerPath / dictCode），前端可用于渲染多级表头 */
    private List<WrTemplateItem> items;

    /** 每个机构一行 */
    private List<OrgRow> orgRows;

    // ═══════════════════════════════════════════════════
    //  矩阵模板（附件3）
    // ═══════════════════════════════════════════════════

    /** 行定义（含 parentRowIndex，供前端构建联动树和缩进渲染） */
    private List<WrTemplateRow> rows;

    /** number 类型的独立填报项（市级成立个数 / 县级成立个数） */
    private List<WrTemplateItem> numberItems;

    /** 每个机构一列（列头信息） */
    private List<OrgCol> orgCols;

    /** checkbox 格值：(orgId, rowIndex) → "1"/"0" */
    private List<MatrixCell> matrixValues;

    /** number 项值：(orgId, itemId) → 数字字符串 */
    private List<NumberCell> numberValues;

    // ─── 内部类 ────────────────────────────────────────

    @Data
    public static class OrgRow {
        private Long    orgId;
        private String  orgName;
        private Long    recordId;
        private Integer status;
        private String  statusLabel;
        /** itemId → { cellValue, cellLabel } */
        private List<Cell> cells;
    }

    @Data
    public static class OrgCol {
        private Long    orgId;
        private String  orgName;
        private Long    recordId;
        private Integer status;
        private String  statusLabel;
    }

    @Data
    public static class Cell {
        private Long   itemId;
        private String cellValue;
        /** 字典翻译后的显示文字，无字典时与 cellValue 相同 */
        private String cellLabel;
    }

    @Data
    public static class MatrixCell {
        private Long    orgId;
        private Integer rowIndex;
        private String  cellValue;
    }

    @Data
    public static class NumberCell {
        private Long   orgId;
        private Long   itemId;
        private String cellValue;
    }
}
