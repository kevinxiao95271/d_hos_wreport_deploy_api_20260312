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
import com.kxhospital.wreport.pojo.response.RecordDetailVO;
import com.kxhospital.wreport.service.WrRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@Tag(name = "上报管理")
@RestController
@RequestMapping("/wr/record")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class WrRecordController {

    private final WrRecordService recordService;

    @Operation(summary = "保存草稿（机构用户）- 幂等")
    @PostMapping("/save")
    public R<Long> save(@Valid @RequestBody RecordSaveRequest req) {
        LoginUser u = requireOrgUser();
        return R.ok(recordService.saveOrUpdate(req, u));
    }

    @Operation(summary = "提交上报（机构用户）")
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

    @Operation(summary = "分页查询所有上报（管理员）")
    @GetMapping("/admin/page")
    public R<IPage<WrRecord>> adminPage(@RequestParam(defaultValue = "1") int pageNum,
                                        @RequestParam(defaultValue = "20") int pageSize,
                                        @RequestParam(required = false) Long taskId,
                                        @RequestParam(required = false) String orgName,
                                        @RequestParam(required = false) Integer status) {
        requireAdmin();
        return R.ok(recordService.adminPage(new Page<>(pageNum, pageSize), taskId, orgName, status));
    }

    @Operation(summary = "上报详情（含数据值和附件）")
    @GetMapping("/detail/{recordId}")
    public R<RecordDetailVO> detail(@PathVariable Long recordId) {
        return R.ok(recordService.detail(recordId));
    }

    @Operation(summary = "审核（管理员）")
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
