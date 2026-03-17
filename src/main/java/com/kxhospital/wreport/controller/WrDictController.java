package com.kxhospital.wreport.controller;

import com.kxhospital.wreport.common.R;
import com.kxhospital.wreport.entity.WrDictItem;
import com.kxhospital.wreport.entity.WrDictType;
import com.kxhospital.wreport.service.WrDictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "字典管理")
@RestController
@RequestMapping("/wr/dict")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class WrDictController {

    private final WrDictService dictService;

    @Operation(summary = "字典类型列表")
    @GetMapping("/types")
    public R<List<WrDictType>> types() {
        return R.ok(dictService.listTypes());
    }

    @Operation(summary = "新增或修改字典类型")
    @PostMapping("/type/save")
    public R<Void> saveType(@RequestBody WrDictType dictType) {
        dictService.saveType(dictType);
        return R.ok();
    }

    @Operation(summary = "删除字典类型（级联软删条目）")
    @PostMapping("/type/delete/{id}")
    public R<Void> deleteType(@PathVariable Long id) {
        dictService.deleteType(id);
        return R.ok();
    }

    @Operation(summary = "获取字典条目列表",
               description = "按 dictCode 查询，前端填报时调用此接口渲染下拉选项")
    @GetMapping("/items/{dictCode}")
    public R<List<WrDictItem>> items(@PathVariable String dictCode) {
        return R.ok(dictService.listItems(dictCode));
    }

    @Operation(summary = "全量覆盖字典条目")
    @PostMapping("/items/save/{dictTypeId}")
    public R<Void> saveItems(@PathVariable Long dictTypeId,
                             @RequestBody List<WrDictItem> items) {
        dictService.replaceItems(dictTypeId, items);
        return R.ok();
    }
}
