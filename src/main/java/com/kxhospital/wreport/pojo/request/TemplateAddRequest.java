package com.kxhospital.wreport.pojo.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class TemplateAddRequest {
    @NotBlank(message = "模板名称不能为空")
    private String templateName;
    private String description;
    private List<TemplateItemRequest> items;
}
