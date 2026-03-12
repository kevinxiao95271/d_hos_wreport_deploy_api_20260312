package com.kxhospital.wreport.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kxhospital.wreport.common.LoginUser;
import com.kxhospital.wreport.common.R;
import com.kxhospital.wreport.common.UserContext;
import com.kxhospital.wreport.entity.WrTemplate;
import com.kxhospital.wreport.entity.WrTemplateItem;
import com.kxhospital.wreport.pojo.request.TemplateAddRequest;
import com.kxhospital.wreport.pojo.request.TemplateItemRequest;
import com.kxhospital.wreport.service.WrTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "模板管理（Admin）")
@RestController
@RequestMapping("/wr/template")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class WrTemplateController {

    private final WrTemplateService templateService;

    @Operation(summary = "分页查询模板")
    @GetMapping("/page")
    public R<IPage<WrTemplate>> page(@RequestParam(defaultValue = "1") int pageNum,
                                     @RequestParam(defaultValue = "20") int pageSize,
                                     @RequestParam(required = false) String templateName,
                                     @RequestParam(required = false) Integer status) {
        requireAdmin();
        return R.ok(templateService.page(new Page<>(pageNum, pageSize), templateName, status));
    }

    @Operation(summary = "模板详情")
    @GetMapping("/detail/{id}")
    public R<WrTemplate> detail(@PathVariable Long id) {
        requireAdmin();
        return R.ok(templateService.detail(id));
    }

    @Operation(summary = "获取模板表头列表")
    @GetMapping("/items/{templateId}")
    public R<List<WrTemplateItem>> items(@PathVariable Long templateId) {
        return R.ok(templateService.items(templateId));
    }

    @Operation(summary = "新增模板")
    @PostMapping("/add")
    public R<Long> add(@Valid @RequestBody TemplateAddRequest req) {
        requireAdmin();
        WrTemplate template = new WrTemplate();
        template.setTemplateName(req.getTemplateName());
        template.setDescription(req.getDescription());

        List<WrTemplateItem> items = new ArrayList<>();
        if (req.getItems() != null) {
            for (TemplateItemRequest ir : req.getItems()) {
                WrTemplateItem item = new WrTemplateItem();
                BeanUtils.copyProperties(ir, item);
                items.add(item);
            }
        }
        return R.ok(templateService.add(template, items));
    }

    @Operation(summary = "修改模板基本信息")
    @PostMapping("/update")
    public R<Void> update(@RequestBody WrTemplate template) {
        requireAdmin();
        templateService.update(template);
        return R.ok();
    }

    @Operation(summary = "修改模板状态 (0=禁用 1=启用)")
    @PostMapping("/status/{id}/{status}")
    public R<Void> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        requireAdmin();
        templateService.updateStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "删除模板")
    @PostMapping("/delete/{id}")
    public R<Void> delete(@PathVariable Long id) {
        requireAdmin();
        templateService.delete(id);
        return R.ok();
    }

    @Operation(summary = "上传格式模板文件（管理员绑定到表头项）")
    @PostMapping("/item/upload-format/{itemId}")
    public R<String> uploadFormat(@PathVariable Long itemId,
                                  @RequestParam("file") MultipartFile file) {
        requireAdmin();
        String url = templateService.uploadFormatTemplate(itemId, file);
        return R.ok(url);
    }

    @Operation(summary = "删除格式模板文件")
    @PostMapping("/item/delete-format/{itemId}")
    public R<Void> deleteFormat(@PathVariable Long itemId) {
        requireAdmin();
        templateService.deleteFormatTemplate(itemId);
        return R.ok();
    }

    private void requireAdmin() {
        LoginUser u = UserContext.get();
        if (u == null || !u.isAdmin()) throw new RuntimeException("权限不足，需要管理员角色");
    }
}
