package com.kxhospital.wreport.pojo.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 模块配置聚合 VO（前端一次性拉取，用于渲染模块名/提示语/动态字段）
 * scoreDesc：公开版考核说明，机构端 + 管理员均可见。
 * scoreRule：完整评分规则（含分值），仅管理员可见，机构端返回 null。
 * scoreMax：满分值，仅管理员可见，机构端返回 null。
 */
@Data
public class DwModuleConfigVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String moduleKey;
    private String moduleName;
    /** 满分值，仅管理员可见 */
    private BigDecimal scoreMax;
    /** 公开版考核说明（去掉具体分值），机构端 + 管理员均可见 */
    private String scoreDesc;
    /** 完整评分规则（含分值），仅管理员可见；机构端为 null */
    private String scoreRule;
    private Boolean isEnabled;
    private Integer sortOrder;
    private String uploadHint;

    /**
     * 父模块 key（null=顶级或加分项）。
     * 前端据此将子项挂到对应大类下渲染。
     */
    private String parentModuleKey;

    /**
     * 是否为叶子节点（可填报/上传）。
     * false=大类容器，前端只渲染标题；true=实际填报项。
     */
    private Boolean isLeaf;

    /**
     * 是否为加分项。前端据此将其归入"加分项"分组独立展示。
     */
    private Boolean isBonus;

    /**
     * 该模块下已启用的扩展字段定义列表（子记录级动态表单字段）。
     * module_self_score 为模块级自评分，不在此列表中，其值通过
     * GET /dw/record/{id} 的 moduleSelfScores 字段单独返回。
     */
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
