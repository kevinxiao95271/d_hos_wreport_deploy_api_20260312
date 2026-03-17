package com.kxhospital.wreport.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kxhospital.wreport.common.LoginUser;
import com.kxhospital.wreport.common.R;
import com.kxhospital.wreport.common.UserContext;
import com.kxhospital.wreport.entity.WrRecord;
import com.kxhospital.wreport.pojo.request.RecordAuditRequest;
import com.kxhospital.wreport.pojo.request.RecordSaveRequest;
import com.kxhospital.wreport.pojo.request.RecordSubmitRequest;
import com.kxhospital.wreport.pojo.response.CrossViewVO;
import com.kxhospital.wreport.pojo.response.RecordAggregateResponse;
import com.kxhospital.wreport.pojo.response.RecordDetailVO;
import com.kxhospital.wreport.service.WrRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;

@Tag(name = "上报管理")
@RestController
@RequestMapping("/wr/record")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class WrRecordController {

    private final WrRecordService recordService;

    @Operation(summary = "保存草稿（机构用户）- 幂等", description = "保存草稿或覆盖草稿数据")
    @ApiResponse(responseCode = "200", description = "success", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":200,\"message\":\"success\",\"data\":2032129401981792257}")))
    @PostMapping("/save")
    public R<Long> save(@Valid @RequestBody RecordSaveRequest req) {
        LoginUser u = requireOrgUser();
        return R.ok(recordService.saveOrUpdate(req, u));
    }

    @Operation(summary = "提交上报（机构用户）", description = "提交草稿，进入待审核状态")
    @ApiResponse(responseCode = "200", description = "success", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":200,\"message\":\"success\",\"data\":null}")))
    @PostMapping("/submit")
    public R<Void> submit(@Valid @RequestBody RecordSubmitRequest req) {
        LoginUser u = requireOrgUser();
        recordService.submit(req, u);
        return R.ok();
    }

    @Operation(summary = "查询我的上报记录（机构用户）")
    @GetMapping("/my/{taskId}")
    public R<WrRecord> myRecord(@PathVariable Long taskId) {
        LoginUser u = requireOrgUser();
        return R.ok(recordService.myRecord(taskId, u));
    }

    @Operation(summary = "我的上报记录分页（机构用户）", description = "按机构过滤，支持 taskId/status 条件")
    @ApiResponse(responseCode = "200", description = "success", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":200,\"message\":\"success\",\"data\":{\"records\":[{\"id\":2032129401981792257,\"taskId\":2032128093065342977,\"orgId\":1986677412049780738,\"orgName\":\"某机构\",\"status\":1}],\"total\":1}}")))
    @GetMapping("/my/page")
    public R<IPage<WrRecord>> myPage(@RequestParam(defaultValue = "1") int pageNum,
                                    @RequestParam(defaultValue = "20") int pageSize,
                                    @RequestParam(required = false) Long taskId,
                                    @RequestParam(required = false) Integer status) {
        LoginUser u = requireOrgUser();
        return R.ok(recordService.myPage(new Page<>(pageNum, pageSize), u, taskId, status));
    }

    @Operation(summary = "分页查询所有上报（管理员）", description = "支持 taskId/orgName/status 条件")
    @ApiResponse(responseCode = "200", description = "success", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":200,\"message\":\"success\",\"data\":{\"records\":[{\"id\":2032129401981792257,\"orgName\":\"某机构\",\"status\":1}],\"total\":1}}")))
    @GetMapping("/admin/page")
    public R<IPage<WrRecord>> adminPage(@RequestParam(defaultValue = "1") int pageNum,
                                        @RequestParam(defaultValue = "20") int pageSize,
                                        @RequestParam(required = false) Long taskId,
                                        @RequestParam(required = false) String orgName,
                                        @RequestParam(required = false) Integer status) {
        requireAdmin();
        return R.ok(recordService.adminPage(new Page<>(pageNum, pageSize), taskId, orgName, status));
    }

    @Operation(summary = "上报统计汇总（管理员）", description = "按 taskId 统计各状态数量")
    @ApiResponse(responseCode = "200", description = "success", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":200,\"message\":\"success\",\"data\":{\"total\":10,\"draft\":2,\"submitted\":3,\"approved\":4,\"rejected\":1}}")))
    @GetMapping("/admin/aggregate")
    public R<RecordAggregateResponse> aggregate(@RequestParam(required = false) Long taskId) {
        requireAdmin();
        return R.ok(recordService.aggregate(taskId));
    }

    @Operation(summary = "跨机构横向视图（管理员）",
               description = "标准模板：列=选中字段，行=各机构；矩阵模板：列=各机构，行=地理层级行。" +
                             "itemIds 仅对标准模板生效（逗号分隔），不传则返回全部叶子列。")
    @GetMapping("/admin/crossview")
    public R<CrossViewVO> crossView(
            @RequestParam Long taskId,
            @RequestParam(required = false) List<Long> itemIds) {
        requireAdmin();
        return R.ok(recordService.crossView(taskId, itemIds));
    }

    @Operation(summary = "上报详情（含数据值和附件）", description = "返回详情及 statusLabel")
    @ApiResponse(responseCode = "200", description = "success", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":200,\"message\":\"success\",\"data\":{\"record\":{\"id\":2032129401981792257,\"status\":1},\"values\":[{\"itemId\":2032126966634672130,\"cellValue\":\"省级质控中心名称\"}],\"attachments\":[],\"statusLabel\":\"待审核\"}}")))
    @GetMapping("/detail/{recordId}")
    public R<RecordDetailVO> detail(@PathVariable Long recordId) {
        return R.ok(recordService.detail(recordId));
    }

    @Operation(summary = "审核（管理员）", description = "auditResult: 1=通过 2=驳回")
    @ApiResponse(responseCode = "200", description = "success", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":200,\"message\":\"success\",\"data\":null}")))
    @PostMapping("/audit")
    public R<Void> audit(@Valid @RequestBody RecordAuditRequest req) {
        LoginUser u = requireAdmin();
        recordService.audit(req, u);
        return R.ok();
    }

    @Operation(summary = "导出 Excel（管理员）")
    @GetMapping("/export/{taskId}")
    public void export(@PathVariable Long taskId, HttpServletResponse response) {
        requireAdmin();
        recordService.exportExcel(taskId, response);
    }

    private LoginUser requireAdmin() {
        LoginUser u = UserContext.get();
        if (u == null || !u.isAdmin()) throw new RuntimeException("权限不足，需要管理员角色");
        return u;
    }

    private LoginUser requireOrgUser() {
        LoginUser u = UserContext.get();
        if (u == null) throw new RuntimeException("未登录");
        if (!u.isOrgUser()) throw new RuntimeException("权限不足，需要机构用户角色");
        return u;
    }
}
