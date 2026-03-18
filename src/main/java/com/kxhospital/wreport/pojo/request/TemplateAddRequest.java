package com.kxhospital.wreport.pojo.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class TemplateAddRequest {
    @NotBlank(message = "模板名称不能为空")
    private String templateName;
    private String description;
    /**
     * 填报总字数上限：0 或 null = 不限制；正整数 = 启用限制。
     * 参见 WrTemplate.maxTotalChars 字段说明。
     */
    private Integer maxTotalChars;
    private List<TemplateItemRequest> items;
}
