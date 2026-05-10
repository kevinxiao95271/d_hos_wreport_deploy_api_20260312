package com.kxhospital.wreport.service;

import com.kxhospital.wreport.common.LoginUser;
import com.kxhospital.wreport.entity.*;
import com.kxhospital.wreport.pojo.request.*;
import com.kxhospital.wreport.pojo.response.DwAttachmentVO;
import com.kxhospital.wreport.pojo.response.DwAdminOverviewVO;
import com.kxhospital.wreport.pojo.response.DwRecordDetailVO;
import com.kxhospital.wreport.pojo.response.DwYearQuarterRecordVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** 日常工作模块 — 填报服务 */
public interface DwRecordService {

    /** 获取或初始化填报记录（机构用户访问任务时自动创建草稿） */
    DwRecordDetailVO getOrInitRecord(Long taskId, LoginUser user);

    /** 查询填报详情（管理员或本机构用户） */
    DwRecordDetailVO detail(Long recordId, LoginUser user);

    // ── 质控会议 ──────────────────────────────────────
    DwMeeting saveMeeting(DwMeetingRequest req, LoginUser user);
    void deleteMeeting(Long meetingId, LoginUser user);

    // ── 质控培训 ──────────────────────────────────────
    DwTraining saveTraining(DwTrainingRequest req, LoginUser user);
    void deleteTraining(Long trainingId, LoginUser user);

    // ── 质控指导 ──────────────────────────────────────
    DwGuidance saveGuidance(DwGuidanceRequest req, LoginUser user);
    void deleteGuidance(Long guidanceId, LoginUser user);

    // ── 质控调研 ──────────────────────────────────────
    DwSurvey saveSurvey(DwSurveyRequest req, LoginUser user);
    void deleteSurvey(Long surveyId, LoginUser user);

    // ── 质控数据分析报告 ───────────────────────────────
    DwDataAnalysis saveDataAnalysis(DwDataAnalysisRequest req, LoginUser user);
    void deleteDataAnalysis(Long dataAnalysisId, LoginUser user);

    // ── 三级质控网络完善 ───────────────────────────────
    DwNetworkBuild saveNetworkBuild(DwNetworkBuildRequest req, LoginUser user);

    // ── 经费执行 ──────────────────────────────────────
    DwFunding saveFunding(DwFundingRequest req, LoginUser user);

    // ── 加分项 ────────────────────────────────────────
    DwBonus saveBonus(DwBonusRequest req, LoginUser user);
    void deleteBonus(Long bonusId, LoginUser user);

    // ── 附件上传/删除 ─────────────────────────────────
    DwAttachmentVO uploadAttachment(Long recordId, String moduleType,
                                   Long subRecordId, String slot,
                                   MultipartFile file, LoginUser user);
    void deleteAttachment(Long attachmentId, LoginUser user);

    // ── 提交/审核 ─────────────────────────────────────
    void submit(Long recordId, LoginUser user);
    void audit(Long recordId, Integer result, String remark, LoginUser user);

    // ── 管理端汇总视图 ────────────────────────────────
    /** 跨机构汇总视图：各机构填报状态及各模块数据量 */
    DwAdminOverviewVO adminOverview(Long taskId);

    /**
     * 年度汇总：按 statYear 查该年度下所有 daily_work 季度任务及指定机构的填报快照（只读）。
     * 管理端在查看年度任务时用于展示季度参考数据。
     * approvedOnly=true 时仅返回已审核通过（recordStatus=2）的季度记录。
     */
    List<DwYearQuarterRecordVO> yearSummary(String statYear, Long orgId, Boolean approvedOnly, LoginUser user);
}
