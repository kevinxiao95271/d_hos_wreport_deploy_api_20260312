package com.kxhospital.wreport.controller;

import com.kxhospital.wreport.cache.DwRegionCache;
import com.kxhospital.wreport.common.LoginUser;
import com.kxhospital.wreport.common.R;
import com.kxhospital.wreport.common.UserContext;
import com.kxhospital.wreport.entity.DwFieldConfig;
import com.kxhospital.wreport.entity.DwModuleConfig;
import com.kxhospital.wreport.pojo.response.DwModuleConfigVO;
import com.kxhospital.wreport.pojo.response.GuidanceRegionsVO;
import com.kxhospital.wreport.service.DwConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "日常工作模块 — 配置管理")
@RestController
@RequestMapping("/dw/config")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class DwConfigController {

    private final DwConfigService configService;
    private final DwRegionCache   regionCache;

    // ── 质控指导地区树（任何已登录用户均可调用） ───────────────────────────────────

    @Operation(summary = "获取质控指导地区树",
               description = "返回两棵树：\n" +
                             "- cityTree：省→市级质控中心（11 个平铺叶节点），用于勾选市级中心数量\n" +
                             "- countyTree：省→市→区县两级树，用于勾选县级中心数量\n\n" +
                             "数据启动时一次性加载，常驻内存，接口响应极快。\n\n" +
                             "前端计数：cityCenterCount = cityTree 中被勾选节点数；\n" +
                             "countyCenterCount = countyTree 中被勾选的叶节点（区县）数。")
    @GetMapping("/guidance/regions")
    public R<GuidanceRegionsVO> guidanceRegions() {
        user(); // 仅校验登录状态
        return R.ok(regionCache.get());
    }

    // ── 模块配置查询（管理员 + 机构均可调用，权限不同返回不同字段） ──────────────

    @Operation(summary = "获取所有模块配置",
               description = "管理员调用时返回 scoreRule（评分规则）；机构用户调用时该字段为 null。\n" +
                             "前端初始化填报页时调用一次，用于渲染模块名、上传提示语、动态扩展字段表单。")
    @GetMapping("/modules")
    public R<List<DwModuleConfigVO>> listModules() {
        LoginUser u = user();
        return R.ok(configService.listModules(u.isAdmin()));
    }

    // ── 模块配置修改（管理员） ──────────────────────────────────────────────────

    @Operation(summary = "更新模块配置（管理员）",
               description = "可修改字段：moduleName、scoreMax、scoreRule、isEnabled、sortOrder、uploadHint。\n" +
                             "仅传需要修改的字段，其余字段传 null 不会被覆盖（MyBatis-Plus 动态 UPDATE）。")
    @PostMapping("/module/update")
    public R<Void> updateModule(@RequestBody DwModuleConfig config) {
        requireAdmin();
        configService.updateModule(config);
        return R.ok();
    }

    // ── 扩展字段定义管理（管理员） ─────────────────────────────────────────────

    @Operation(summary = "新增扩展字段定义（管理员）",
               description = "fieldType: 'text' | 'number' | 'enum' | 'checkbox'\n" +
                             "enum/checkbox 时须填 fieldOptions（JSON 字符串数组，如 '[\"线上\",\"线下\"]'）")
    @PostMapping("/field/add")
    public R<DwFieldConfig> addField(@RequestBody DwFieldConfig config) {
        requireAdmin();
        return R.ok(configService.addField(config));
    }

    @Operation(summary = "更新扩展字段定义（管理员）",
               description = "仅传需要修改的字段；fieldKey 一旦创建不建议修改，否则历史值无法匹配")
    @PostMapping("/field/update")
    public R<Void> updateField(@RequestBody DwFieldConfig config) {
        requireAdmin();
        configService.updateField(config);
        return R.ok();
    }

    @Operation(summary = "禁用扩展字段（管理员，软删除）",
               description = "不物理删除，保留历史填报值，仅将 is_enabled 置为 false，前端不再展示该字段")
    @PostMapping("/field/delete/{id}")
    public R<Void> deleteField(@PathVariable Long id) {
        requireAdmin();
        configService.deleteField(id);
        return R.ok();
    }

    // ── 扩展字段值保存（机构填报时调用） ──────────────────────────────────────

    @Operation(summary = "保存扩展字段值（机构填报）",
               description = "支持批量 upsert。\n" +
                             "subRecordId：多条记录型模块（meeting/training/guidance/survey/bonus）传子记录ID；\n" +
                             "纯上传/单条型模块（annual_work/funding 等）传 null。\n\n" +
                             "values 示例（text类型）：{\"budget_amount\": \"3500\", \"memo\": \"备注内容\"}\n" +
                             "values 示例（checkbox类型）：{\"target_group\": \"[\\\"线上\\\",\\\"专科医院\\\"]\"}",
               requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                   content = @io.swagger.v3.oas.annotations.media.Content(
                       schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SaveFieldValuesRequest.class)
                   )
               ))
    @PostMapping("/field/values/save")
    public R<Void> saveFieldValues(@RequestBody SaveFieldValuesRequest req) {
        LoginUser u = user();
        configService.saveFieldValues(req.getRecordId(), req.getModuleKey(),
                req.getSubRecordId(), req.getValues(), u);
        return R.ok();
    }

    // ── 请求体 DTO ──────────────────────────────────────────────────────────────

    @Data
    public static class SaveFieldValuesRequest {
        private Long recordId;
        private String moduleKey;
        /** 多条记录型子记录ID，单条/纯上传型传 null */
        private Long subRecordId;
        /** { fieldKey -> fieldValue }，checkbox 类型 value 为 JSON 数组字符串 */
        private Map<String, String> values;
    }

    // ── 工具 ───────────────────────────────────────────────────────────────────

    private LoginUser user() {
        LoginUser u = UserContext.get();
        if (u == null) throw new com.kxhospital.wreport.common.BusinessException(401, "未登录");
        return u;
    }

    private LoginUser requireAdmin() {
        LoginUser u = user();
        if (!u.isAdmin()) throw new com.kxhospital.wreport.common.BusinessException(403, "权限不足，需要管理员角色");
        return u;
    }
}
