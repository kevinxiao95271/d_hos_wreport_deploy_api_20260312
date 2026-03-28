package com.kxhospital.wreport.pojo.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 模块配置聚合 VO（前端一次性拉取，用于渲染模块名/提示语/动态字段）
 * score_rule 字段仅在管理员请求时返回，机构端该字段为 null。
 */
@Data
public class DwModuleConfigVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String moduleKey;
    private String moduleName;
    private BigDecimal scoreMax;
    /** 评分规则，仅管理员可见；机构端接口不返回此字段（置 null） */
    private String scoreRule;
    private Boolean isEnabled;
    private Integer sortOrder;
    private String uploadHint;
    /** 该模块下已启用的扩展字段定义列表 */
    private List<DwFieldConfigVO> extraFields;

    @Data
    public static class DwFieldConfigVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String fieldKey;
        private String fieldName;
        /** 'text' | 'number' | 'enum' | 'checkbox' */
        private String fieldType;
        /** enum/checkbox 时为 JSON 字符串数组，其余为 null */
        private String fieldOptions;
        private Boolean isRequired;
        private Integer sortOrder;
        private String placeholder;
    }
}
