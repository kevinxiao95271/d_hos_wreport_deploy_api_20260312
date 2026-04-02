package com.kxhospital.wreport.controller;

import com.kxhospital.wreport.common.LoginUser;
import com.kxhospital.wreport.common.R;
import com.kxhospital.wreport.common.UserContext;
import com.kxhospital.wreport.entity.*;
import com.kxhospital.wreport.pojo.request.*;
import com.kxhospital.wreport.pojo.response.DwAdminOverviewVO;
import com.kxhospital.wreport.pojo.response.DwAttachmentVO;
import com.kxhospital.wreport.pojo.response.DwRecordDetailVO;
import com.kxhospital.wreport.pojo.response.DwYearQuarterRecordVO;
import com.kxhospital.wreport.service.DwRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "日常工作模块 — 填报")
@RestController
@RequestMapping("/dw/record")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class DwRecordController {

    private final DwRecordService service;

    // ── 记录 ─────────────────────────────────────────────────────

    @Operation(summary = "获取或初始化填报记录（机构用户）",
               description = "机构用户进入日常工作任务时调用，自动创建草稿记录并返回完整详情")
    @GetMapping("/init/{taskId}")
    public R<DwRecordDetailVO> initRecord(@PathVariable Long taskId) {
        return R.ok(service.getOrInitRecord(taskId, user()));
    }

    @Operation(summary = "跨机构汇总视图（管理员）",
               description = "返回该日常工作任务下所有机构的填报状态及各模块数据量，替代 normal 任务的 crossview")
    @GetMapping("/admin/overview")
    public R<DwAdminOverviewVO> adminOverview(@RequestParam Long taskId) {
        requireAdmin();
        return R.ok(service.adminOverview(taskId));
    }

    @Operation(summary = "年度汇总（管理端查看年度任务时展示各季度填报参考）",
               description = "按 statYear 汇总该年度下所有 daily_work 季度任务，返回指定机构的填报快照（只读）。\n" +
                             "管理端须传 orgId；机构端返回本机构数据。\n" +
                             "approvedOnly=true 时仅返回已审核通过（recordStatus=2）的季度，可用于年度任务页仅展示已通过数据。")
    @GetMapping("/year-summary")
    public R<List<DwYearQuarterRecordVO>> yearSummary(
            @RequestParam String statYear,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false, defaultValue = "false") Boolean approvedOnly) {
        return R.ok(service.yearSummary(statYear, orgId, approvedOnly, user()));
    }

    @Operation(summary = "查看填报详情",
               description = "管理员可查看任意记录，机构用户只能查看本机构记录")
    @GetMapping("/{recordId}")
    public R<DwRecordDetailVO> detail(@PathVariable Long recordId) {
        return R.ok(service.detail(recordId, user()));
    }

    @Operation(summary = "提交填报记录")
    @PostMapping("/submit/{recordId}")
    public R<Void> submit(@PathVariable Long recordId) {
        service.submit(recordId, user());
        return R.ok();
    }

    @Operation(summary = "审核填报记录（管理员）",
               description = "result: 1=通过  0=驳回")
    @PostMapping("/audit/{recordId}")
    public R<Void> audit(@PathVariable Long recordId,
                         @RequestParam Integer result,
                         @RequestParam(required = false) String remark) {
        service.audit(recordId, result, remark, user());
        return R.ok();
    }

    // ── 质控会议 ─────────────────────────────────────────────────

    @Operation(summary = "新增/编辑质控会议记录",
               description = "id 为 null 时新增，有值时更新")
    @PostMapping("/meeting/save")
    public R<DwMeeting> saveMeeting(@RequestBody DwMeetingRequest req) {
        return R.ok(service.saveMeeting(req, user()));
    }

    @Operation(summary = "删除质控会议记录（含附件）")
    @PostMapping("/meeting/delete/{id}")
    public R<Void> deleteMeeting(@PathVariable Long id) {
        service.deleteMeeting(id, user());
        return R.ok();
    }

    // ── 质控培训 ─────────────────────────────────────────────────

    @Operation(summary = "新增/编辑质控培训记录")
    @PostMapping("/training/save")
    public R<DwTraining> saveTraining(@RequestBody DwTrainingRequest req) {
        return R.ok(service.saveTraining(req, user()));
    }

    @Operation(summary = "删除质控培训记录（含附件）")
    @PostMapping("/training/delete/{id}")
    public R<Void> deleteTraining(@PathVariable Long id) {
        service.deleteTraining(id, user());
        return R.ok();
    }

    // ── 质控指导 ─────────────────────────────────────────────────

    @Operation(summary = "新增/编辑质控指导记录",
               description = "cityCenterCount/countyCenterCount 由前端树勾选自动统计后传入，hospitalCount 手动填写，三者之和不能为 0")
    @PostMapping("/guidance/save")
    public R<DwGuidance> saveGuidance(@RequestBody DwGuidanceRequest req) {
        return R.ok(service.saveGuidance(req, user()));
    }

    @Operation(summary = "删除质控指导记录（含附件）")
    @PostMapping("/guidance/delete/{id}")
    public R<Void> deleteGuidance(@PathVariable Long id) {
        service.deleteGuidance(id, user());
        return R.ok();
    }

    // ── 质控调研 ─────────────────────────────────────────────────

    @Operation(summary = "新增/编辑质控调研记录")
    @PostMapping("/survey/save")
    public R<DwSurvey> saveSurvey(@RequestBody DwSurveyRequest req) {
        return R.ok(service.saveSurvey(req, user()));
    }

    @Operation(summary = "删除质控调研记录（含附件）")
    @PostMapping("/survey/delete/{id}")
    public R<Void> deleteSurvey(@PathVariable Long id) {
        service.deleteSurvey(id, user());
        return R.ok();
    }

    // ── 经费执行 ─────────────────────────────────────────────────

    @Operation(summary = "保存经费执行数据（每条记录只有一份，重复调用则覆盖）")
    @PostMapping("/funding/save")
    public R<DwFunding> saveFunding(@RequestBody DwFundingRequest req) {
        return R.ok(service.saveFunding(req, user()));
    }

    // ── 加分项 ───────────────────────────────────────────────────

    @Operation(summary = "保存加分项（同类型重复调用则覆盖）",
               description = "bonusType: 'publication'=丛书/指南  'competition'=技能竞赛")
    @PostMapping("/bonus/save")
    public R<DwBonus> saveBonus(@RequestBody DwBonusRequest req) {
        return R.ok(service.saveBonus(req, user()));
    }

    @Operation(summary = "删除加分项（含附件）")
    @PostMapping("/bonus/delete/{id}")
    public R<Void> deleteBonus(@PathVariable Long id) {
        service.deleteBonus(id, user());
        return R.ok();
    }

    // ── 附件 ─────────────────────────────────────────────────────

    @Operation(summary = "上传附件",
               description = "moduleType: 模块标识（meeting/training/guidance/survey/annual_work/it_construction/work_plan/admin_response/activity_report/bonus）\n" +
                             "subRecordId: 多条记录型模块的子记录ID（meeting/training/guidance/survey/bonus），纯上传模块传 null\n" +
                             "slot: 附件槽位（meeting→minutes|photo|signin；training→material|photo；guidance→evidence；survey→report|photo；work_plan→plan|summary；activity_report→pre_report|post_report；其余→evidence）")
    @PostMapping("/attachment/upload")
    public R<DwAttachmentVO> upload(@RequestParam Long recordId,
                                    @RequestParam String moduleType,
                                    @RequestParam(required = false) Long subRecordId,
                                    @RequestParam String slot,
                                    @RequestParam("file") MultipartFile file) {
        return R.ok(service.uploadAttachment(recordId, moduleType, subRecordId, slot, file, user()));
    }

    @Operation(summary = "删除附件（同时从 MinIO 删除文件）")
    @PostMapping("/attachment/delete/{id}")
    public R<Void> deleteAttachment(@PathVariable Long id) {
        service.deleteAttachment(id, user());
        return R.ok();
    }

    // ── 工具 ─────────────────────────────────────────────────────

    private LoginUser user() {
        LoginUser u = UserContext.get();
        if (u == null) throw new com.kxhospital.wreport.common.BusinessException(401, "未登录");
        return u;
    }

    private LoginUser requireAdmin() {
        LoginUser u = user();
        if (!u.isAdmin()) throw new com.kxhospital.wreport.common.BusinessException(403, "无权限");
        return u;
    }
}
