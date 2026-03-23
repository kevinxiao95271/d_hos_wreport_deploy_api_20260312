package com.kxhospital.wreport.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kxhospital.wreport.common.LoginUser;
import com.kxhospital.wreport.common.R;
import com.kxhospital.wreport.common.UserContext;
import com.kxhospital.wreport.entity.WrTemplate;
import com.kxhospital.wreport.entity.WrTemplateItem;
import com.kxhospital.wreport.entity.WrTemplateRow;
import com.kxhospital.wreport.pojo.request.TemplateAddRequest;
import com.kxhospital.wreport.pojo.request.TemplateItemRequest;
import com.kxhospital.wreport.pojo.response.TemplateDetailVO;
import com.kxhospital.wreport.service.WrTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

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

    @Operation(summary = "模板详情（基本信息）")
    @GetMapping("/detail/{id}")
    public R<WrTemplate> detail(@PathVariable Long id) {
        requireAdmin();
        return R.ok(templateService.detail(id));
    }

    @Operation(summary = "模板完整详情（含列定义+行定义）")
    @GetMapping("/detail/full/{id}")
    public R<TemplateDetailVO> detailFull(@PathVariable Long id) {
        return R.ok(templateService.detailFull(id));
    }

    @Operation(summary = "获取模板表头列表")
    @GetMapping("/items/{templateId}")
    public R<List<WrTemplateItem>> items(@PathVariable Long templateId) {
        return R.ok(templateService.items(templateId));
    }

    @Operation(summary = "覆盖保存模板表头",
               description = "全量覆盖模板表头项列表。每个节点必须带 id（已有节点填真实 DB id，新增节点填前端临时字符串），" +
                             "parentId 引用同批次某节点的 id。后端写前校验：无重复 id、无悬空 parentId、无环。")
    @PostMapping("/items/save/{templateId}")
    public R<Void> saveItems(@PathVariable Long templateId,
                             @org.springframework.web.bind.annotation.RequestBody List<TemplateItemRequest> items) {
        requireAdmin();
        templateService.replaceItems(templateId, items);
        return R.ok();
    }

    @Operation(summary = "模板列表(不分页，仅启用)", description = "仅返回 status=1 的模板")
    @ApiResponse(responseCode = "200", description = "success", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":200,\"message\":\"success\",\"data\":[{\"id\":2032126966508843010,\"templateName\":\"附件3：省市县质控中心设立情况统计表\",\"status\":1}]}")))
    @GetMapping("/list")
    public R<List<WrTemplate>> list() {
        requireAdmin();
        return R.ok(templateService.listActive());
    }

    @Operation(summary = "新增模板")
    @ApiResponse(responseCode = "200", description = "success", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":200,\"message\":\"success\",\"data\":2032126966508843010}")))
    @PostMapping("/add")
    public R<Long> add(@Valid @RequestBody TemplateAddRequest req) {
        requireAdmin();
        WrTemplate template = new WrTemplate();
        template.setTemplateName(req.getTemplateName());
        template.setDescription(req.getDescription());
        // templateType: 不传或 null 时默认 "form"；传 "score" 表示评分细则模板
        template.setTemplateType(req.getTemplateType() != null ? req.getTemplateType() : "form");
        // maxTotalChars: 0 或 null 均视为未启用；正整数表示启用并设置上限
        template.setMaxTotalChars(req.getMaxTotalChars() != null ? req.getMaxTotalChars() : 0);

        return R.ok(templateService.add(template, req.getItems()));
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

    @Operation(summary = "设置/清除表头项的字典绑定",
               description = "dictCode 传空串或不传 body → 解绑（切回普通输入框）；" +
                             "传有效 dictCode → 绑定字典（切为下拉框）。两个方向均安全，不影响已填报数据。")
    @PostMapping("/item/dict/{itemId}")
    public R<Void> updateItemDict(@PathVariable Long itemId,
                                  @org.springframework.web.bind.annotation.RequestBody(required = false)
                                  java.util.Map<String, String> body) {
        requireAdmin();
        String dictCode = (body != null) ? body.get("dictCode") : null;
        templateService.updateItemDict(itemId, dictCode);
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

    @Operation(summary = "查询模板行定义（矩阵类模板专用）")
    @GetMapping("/rows/{templateId}")
    public R<List<WrTemplateRow>> rows(@PathVariable Long templateId) {
        return R.ok(templateService.listRows(templateId));
    }

    @Operation(summary = "覆盖保存模板行定义（全量替换）")
    @PostMapping("/rows/save/{templateId}")
    public R<Void> saveRows(@PathVariable Long templateId,
                            @org.springframework.web.bind.annotation.RequestBody List<WrTemplateRow> rows) {
        requireAdmin();
        templateService.replaceRows(templateId, rows);
        return R.ok();
    }

    private void requireAdmin() {
        LoginUser u = UserContext.get();
        if (u == null || !u.isAdmin())
            throw new com.kxhospital.wreport.common.BusinessException(403, "权限不足，需要管理员角色");
    }
}
