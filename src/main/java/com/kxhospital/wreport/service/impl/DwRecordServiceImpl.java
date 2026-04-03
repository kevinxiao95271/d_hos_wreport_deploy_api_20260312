package com.kxhospital.wreport.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kxhospital.wreport.common.BusinessException;
import com.kxhospital.wreport.common.LoginUser;
import com.kxhospital.wreport.config.MinioProperties;
import com.kxhospital.wreport.config.MinioService;
import com.kxhospital.wreport.entity.*;
import com.kxhospital.wreport.mapper.*;
import com.kxhospital.wreport.pojo.request.*;
import com.kxhospital.wreport.cache.DwRegionCache;
import com.kxhospital.wreport.pojo.response.DwAdminOverviewVO;
import com.kxhospital.wreport.pojo.response.DwAttachmentVO;
import com.kxhospital.wreport.pojo.response.DwRecordDetailVO;
import com.kxhospital.wreport.pojo.response.DwRecordDetailVO.*;
import com.kxhospital.wreport.pojo.response.DwYearQuarterRecordVO;
import com.kxhospital.wreport.pojo.response.RegionNodeVO;
import com.kxhospital.wreport.pojo.response.TaskScopeOrgVO;
import com.kxhospital.wreport.service.DwRecordService;
import com.kxhospital.wreport.service.DwTaskModuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class DwRecordServiceImpl implements DwRecordService {

    private final WrTaskMapper            taskMapper;
    private final WrRecordMapper          recordMapper;
    private final WrTaskOrgScopeMapper    taskOrgScopeMapper;
    private final DwMeetingMapper    meetingMapper;
    private final DwTrainingMapper   trainingMapper;
    private final DwGuidanceMapper   guidanceMapper;
    private final DwSurveyMapper     surveyMapper;
    private final DwFundingMapper    fundingMapper;
    private final DwBonusMapper      bonusMapper;
    private final DwAttachmentMapper attachmentMapper;
    private final MinioService       minioService;
    private final MinioProperties    minioProps;
    private final com.kxhospital.wreport.service.DwConfigService configService;
    private final DwTaskModuleService dwTaskModuleService;
    private final DwRegionCache      regionCache;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ─────────────────────────────────────────────────────────────
    // 获取或初始化填报记录
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DwRecordDetailVO getOrInitRecord(Long taskId, LoginUser user) {
        WrTask task = taskMapper.selectById(taskId);
        if (task == null || task.getDelFlag() == 1)
            throw new BusinessException(404, "任务不存在");
        if (!"daily_work".equals(task.getTaskType()))
            throw new BusinessException(400, "该任务不是日常工作模块任务");
        if (task.getStatus() != 1)
            throw new BusinessException(400, "任务尚未开放，无法填报");
        // 检查机构是否在任务范围内（scope 为空代表全机构可见）
        boolean inScope = taskMapper.isTaskInScope(taskId, user.getOrgId()) > 0;
        if (!inScope)
            throw new BusinessException(403, "您的机构不在该任务的填报范围内");

        WrRecord record = recordMapper.selectOne(new LambdaQueryWrapper<WrRecord>()
                .eq(WrRecord::getTaskId, taskId)
                .eq(WrRecord::getOrgId, user.getOrgId())
                .eq(WrRecord::getDelFlag, 0));

        if (record == null) {
            record = new WrRecord();
            record.setTaskId(taskId);
            record.setOrgId(user.getOrgId());
            record.setOrgName(user.getOrgName());
            record.setStatus(0);
            recordMapper.insert(record);
        }
        return buildDetail(record, task, user, false);
    }

    @Override
    public DwRecordDetailVO detail(Long recordId, LoginUser user) {
        WrRecord record = requireRecord(recordId);
        if (!user.isAdmin() && !record.getOrgId().equals(user.getOrgId()))
            throw new BusinessException(403, "无权查看该记录");
        WrTask task = taskMapper.selectById(record.getTaskId());
        return buildDetail(record, task, user, false);
    }

    @Override
    public List<DwYearQuarterRecordVO> yearSummary(String statYear, Long orgId, Boolean approvedOnly, LoginUser user) {
        if (statYear == null || statYear.trim().isEmpty())
            throw new BusinessException(400, "statYear 不能为空");
        Long oid;
        if (user.isAdmin()) {
            if (orgId == null) throw new BusinessException(400, "管理端请指定 orgId");
            oid = orgId;
        } else {
            oid = user.getOrgId();
        }
        List<WrTask> tasks = taskMapper.selectDailyWorkByStatYear(statYear.trim());
        List<DwYearQuarterRecordVO> out = new ArrayList<>();
        boolean onlyApproved = Boolean.TRUE.equals(approvedOnly);
        for (WrTask t : tasks) {
            WrRecord rec = recordMapper.findByTaskAndOrg(t.getId(), oid);
            if (onlyApproved && (rec == null || rec.getStatus() == null || rec.getStatus() != 2)) continue;
            DwYearQuarterRecordVO row = new DwYearQuarterRecordVO();
            row.setTaskId(t.getId());
            row.setTaskName(t.getTaskName());
            row.setTaskStatus(t.getStatus());
            row.setStatYear(t.getStatYear());
            row.setStatQuarter(t.getStatQuarter());
            row.setReadOnly(true);
            row.setEnabledModuleKeys(new ArrayList<>(dwTaskModuleService.resolveEnabledModuleKeys(t.getId())));
            if (rec != null) {
                row.setRecordId(rec.getId());
                row.setRecordStatus(rec.getStatus());
                row.setDetail(buildDetail(rec, t, user, true));
            }
            out.add(row);
        }
        return out;
    }

    // ─────────────────────────────────────────────────────────────
    // 质控会议
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DwMeeting saveMeeting(DwMeetingRequest req, LoginUser user) {
        requireEditableRecord(req.getRecordId(), user);
        assertNotPlaceholderOnly("会议名称", req.getMeetingName());
        HalfDayRange meetingRange = normalizeHalfDayRange(
                req.getMeetingStartDate(), req.getMeetingStartHalf(),
                req.getMeetingEndDate(), req.getMeetingEndHalf(),
                "会议时间");
        DwMeeting entity;
        if (req.getId() != null) {
            entity = meetingMapper.selectById(req.getId());
            if (entity == null) throw new BusinessException(404, "会议记录不存在");
        } else {
            entity = new DwMeeting();
        }
        BeanUtils.copyProperties(req, entity, "id");
        entity.setMeetingStartDate(meetingRange.startDate);
        entity.setMeetingStartHalf(meetingRange.startHalf);
        entity.setMeetingEndDate(meetingRange.endDate);
        entity.setMeetingEndHalf(meetingRange.endHalf);
        if (req.getId() == null) meetingMapper.insert(entity);
        else meetingMapper.updateById(entity);
        return entity;
    }

    @Override
    @Transactional
    public void deleteMeeting(Long meetingId, LoginUser user) {
        DwMeeting entity = meetingMapper.selectById(meetingId);
        if (entity == null) return;
        requireEditableRecord(entity.getRecordId(), user);
        meetingMapper.deleteById(meetingId);
        // 级联软删附件
        attachmentMapper.delete(new LambdaQueryWrapper<DwAttachment>()
                .eq(DwAttachment::getSubRecordId, meetingId)
                .eq(DwAttachment::getModuleType, "meeting"));
    }

    // ─────────────────────────────────────────────────────────────
    // 质控培训
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DwTraining saveTraining(DwTrainingRequest req, LoginUser user) {
        requireEditableRecord(req.getRecordId(), user);
        HalfDayRange trainingRange = normalizeHalfDayRange(
                req.getTrainingStartDate(), req.getTrainingStartHalf(),
                req.getTrainingEndDate(), req.getTrainingEndHalf(),
                "培训时间");
        DwTraining entity;
        if (req.getId() != null) {
            entity = trainingMapper.selectById(req.getId());
            if (entity == null) throw new BusinessException(404, "培训记录不存在");
        } else {
            entity = new DwTraining();
        }
        BeanUtils.copyProperties(req, entity, "id");
        entity.setTrainingStartDate(trainingRange.startDate);
        entity.setTrainingStartHalf(trainingRange.startHalf);
        entity.setTrainingEndDate(trainingRange.endDate);
        entity.setTrainingEndHalf(trainingRange.endHalf);
        if (req.getId() == null) trainingMapper.insert(entity);
        else trainingMapper.updateById(entity);
        return entity;
    }

    @Override
    @Transactional
    public void deleteTraining(Long trainingId, LoginUser user) {
        DwTraining entity = trainingMapper.selectById(trainingId);
        if (entity == null) return;
        requireEditableRecord(entity.getRecordId(), user);
        trainingMapper.deleteById(trainingId);
        attachmentMapper.delete(new LambdaQueryWrapper<DwAttachment>()
                .eq(DwAttachment::getSubRecordId, trainingId)
                .eq(DwAttachment::getModuleType, "training"));
    }

    // ─────────────────────────────────────────────────────────────
    // 质控指导
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DwGuidance saveGuidance(DwGuidanceRequest req, LoginUser user) {
        requireEditableRecord(req.getRecordId(), user);
        HalfDayRange guidanceRange = normalizeHalfDayRange(
                req.getGuidanceStartDate(), req.getGuidanceStartHalf(),
                req.getGuidanceEndDate(), req.getGuidanceEndHalf(),
                "指导时间");
        int total = nvl(req.getCityCenterCount()) + nvl(req.getCountyCenterCount()) + nvl(req.getHospitalCount());
        if (total <= 0) throw new BusinessException(400, "市级中心数、县级中心数、医疗机构数不能全为 0");
        DwGuidance entity;
        if (req.getId() != null) {
            entity = guidanceMapper.selectById(req.getId());
            if (entity == null) throw new BusinessException(404, "指导记录不存在");
        } else {
            entity = new DwGuidance();
        }
        BeanUtils.copyProperties(req, entity, "id");
        entity.setGuidanceStartDate(guidanceRange.startDate);
        entity.setGuidanceStartHalf(guidanceRange.startHalf);
        entity.setGuidanceEndDate(guidanceRange.endDate);
        entity.setGuidanceEndHalf(guidanceRange.endHalf);
        if (req.getId() == null) guidanceMapper.insert(entity);
        else guidanceMapper.updateById(entity);
        return entity;
    }

    @Override
    @Transactional
    public void deleteGuidance(Long guidanceId, LoginUser user) {
        DwGuidance entity = guidanceMapper.selectById(guidanceId);
        if (entity == null) return;
        requireEditableRecord(entity.getRecordId(), user);
        guidanceMapper.deleteById(guidanceId);
        attachmentMapper.delete(new LambdaQueryWrapper<DwAttachment>()
                .eq(DwAttachment::getSubRecordId, guidanceId)
                .eq(DwAttachment::getModuleType, "guidance"));
    }

    // ─────────────────────────────────────────────────────────────
    // 质控调研
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DwSurvey saveSurvey(DwSurveyRequest req, LoginUser user) {
        requireEditableRecord(req.getRecordId(), user);
        HalfDayRange surveyRange = normalizeHalfDayRange(
                req.getSurveyStartDate(), req.getSurveyStartHalf(),
                req.getSurveyEndDate(), req.getSurveyEndHalf(),
                "调研时间");
        DwSurvey entity;
        if (req.getId() != null) {
            entity = surveyMapper.selectById(req.getId());
            if (entity == null) throw new BusinessException(404, "调研记录不存在");
        } else {
            entity = new DwSurvey();
        }
        BeanUtils.copyProperties(req, entity, "id");
        entity.setSurveyStartDate(surveyRange.startDate);
        entity.setSurveyStartHalf(surveyRange.startHalf);
        entity.setSurveyEndDate(surveyRange.endDate);
        entity.setSurveyEndHalf(surveyRange.endHalf);
        if (req.getId() == null) surveyMapper.insert(entity);
        else surveyMapper.updateById(entity);
        return entity;
    }

    @Override
    @Transactional
    public void deleteSurvey(Long surveyId, LoginUser user) {
        DwSurvey entity = surveyMapper.selectById(surveyId);
        if (entity == null) return;
        requireEditableRecord(entity.getRecordId(), user);
        surveyMapper.deleteById(surveyId);
        attachmentMapper.delete(new LambdaQueryWrapper<DwAttachment>()
                .eq(DwAttachment::getSubRecordId, surveyId)
                .eq(DwAttachment::getModuleType, "survey"));
    }

    // ─────────────────────────────────────────────────────────────
    // 经费执行
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DwFunding saveFunding(DwFundingRequest req, LoginUser user) {
        requireEditableRecord(req.getRecordId(), user);
        DwFunding existing = fundingMapper.findByRecord(req.getRecordId());
        DwFunding entity = existing != null ? existing : new DwFunding();
        BeanUtils.copyProperties(req, entity, "id");
        if (existing == null) fundingMapper.insert(entity);
        else fundingMapper.updateById(entity);
        return entity;
    }

    // ─────────────────────────────────────────────────────────────
    // 加分项
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DwBonus saveBonus(DwBonusRequest req, LoginUser user) {
        requireEditableRecord(req.getRecordId(), user);
        if ("publication".equals(req.getBonusType()) && req.getPubDate() == null) {
            throw new BusinessException(400, "出版日期不能为空");
        }
        // publication 只有 pubDate，comp 时间字段对其无意义，提前清零避免前端初始化值触发校验
        if ("publication".equals(req.getBonusType())) {
            req.setCompStartDate(null);
            req.setCompStartHalf(null);
            req.setCompEndDate(null);
            req.setCompEndHalf(null);
        }
        HalfDayRange compRange = normalizeHalfDayRange(
                req.getCompStartDate(), req.getCompStartHalf(),
                req.getCompEndDate(), req.getCompEndHalf(),
                "competition".equals(req.getBonusType()) ? "竞赛举办时间" : null);
        DwBonus existing = bonusMapper.selectOne(new LambdaQueryWrapper<DwBonus>()
                .eq(DwBonus::getRecordId, req.getRecordId())
                .eq(DwBonus::getBonusType, req.getBonusType())
                .eq(DwBonus::getDelFlag, 0));
        DwBonus entity = existing != null ? existing : new DwBonus();
        BeanUtils.copyProperties(req, entity, "id");
        if ("competition".equals(req.getBonusType())) {
            entity.setCompStartDate(compRange.startDate);
            entity.setCompStartHalf(compRange.startHalf);
            entity.setCompEndDate(compRange.endDate);
            entity.setCompEndHalf(compRange.endHalf);
        } else {
            entity.setCompStartDate(null);
            entity.setCompStartHalf(null);
            entity.setCompEndDate(null);
            entity.setCompEndHalf(null);
        }
        if (existing == null) bonusMapper.insert(entity);
        else bonusMapper.updateById(entity);
        return entity;
    }

    @Override
    @Transactional
    public void deleteBonus(Long bonusId, LoginUser user) {
        DwBonus entity = bonusMapper.selectById(bonusId);
        if (entity == null) return;
        requireEditableRecord(entity.getRecordId(), user);
        bonusMapper.deleteById(bonusId);
        attachmentMapper.delete(new LambdaQueryWrapper<DwAttachment>()
                .eq(DwAttachment::getSubRecordId, bonusId)
                .eq(DwAttachment::getModuleType, "bonus"));
    }

    // ─────────────────────────────────────────────────────────────
    // 附件上传 / 删除
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DwAttachmentVO uploadAttachment(Long recordId, String moduleType,
                                           Long subRecordId, String slot,
                                           MultipartFile file, LoginUser user) {
        requireEditableRecord(recordId, user);
        String prefix = "dw/" + recordId + "/" + moduleType;
        String url = minioService.uploadEvidence(file, prefix);

        DwAttachment att = new DwAttachment();
        att.setRecordId(recordId);
        att.setModuleType(moduleType);
        att.setSubRecordId(subRecordId);
        att.setSlot(slot);
        att.setFileName(file.getOriginalFilename());
        att.setFileUrl(url);
        att.setFileSize(file.getSize());
        att.setFileMime(file.getContentType());
        attachmentMapper.insert(att);

        DwAttachmentVO vo = new DwAttachmentVO();
        BeanUtils.copyProperties(att, vo);
        return vo;
    }

    @Override
    @Transactional
    public void deleteAttachment(Long attachmentId, LoginUser user) {
        DwAttachment att = attachmentMapper.selectById(attachmentId);
        if (att == null) return;
        requireEditableRecord(att.getRecordId(), user);
        if (att.getFileUrl() != null)
            minioService.deleteByUrl(minioProps.getBucketEvidence(), att.getFileUrl());
        attachmentMapper.deleteById(attachmentId);
    }

    // ─────────────────────────────────────────────────────────────
    // 提交 / 审核
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void submit(Long recordId, LoginUser user) {
        WrRecord record = requireRecord(recordId);
        if (!record.getOrgId().equals(user.getOrgId()))
            throw new BusinessException(403, "无权操作");
        if (record.getStatus() != 0 && record.getStatus() != 3)
            throw new BusinessException(400, "当前状态不允许提交");
        record.setStatus(1);
        record.setSubmitUser(user.getUserId());
        record.setSubmitTime(java.time.LocalDateTime.now());
        recordMapper.updateById(record);
    }

    @Override
    @Transactional
    public void audit(Long recordId, Integer result, String remark, LoginUser user) {
        if (!user.isAdmin()) throw new BusinessException(403, "无权审核");
        WrRecord record = requireRecord(recordId);
        if (record.getStatus() != 1) throw new BusinessException(400, "记录尚未提交，无法审核");
        record.setStatus(result == 1 ? 2 : 3);
        record.setAuditUser(user.getUserId());
        record.setAuditTime(java.time.LocalDateTime.now());
        record.setAuditResult(result);
        record.setAuditRemark(remark);
        recordMapper.updateById(record);
    }

    // ─────────────────────────────────────────────────────────────
    // 内部：聚合详情构建
    // ─────────────────────────────────────────────────────────────

    private DwRecordDetailVO buildDetail(WrRecord record, WrTask task, LoginUser user, boolean forceReadOnly) {
        Long rid = record.getId();
        DwRecordDetailVO vo = new DwRecordDetailVO();
        vo.setRecordId(rid);
        vo.setTaskId(record.getTaskId());
        vo.setTaskName(task != null ? task.getTaskName() : null);
        vo.setTaskType(task != null ? task.getTaskType() : null);
        vo.setStatYear(task != null ? task.getStatYear() : null);
        vo.setStatQuarter(task != null ? task.getStatQuarter() : null);
        vo.setOrgId(record.getOrgId());
        vo.setOrgName(record.getOrgName());
        vo.setStatus(record.getStatus());
        vo.setAuditRemark(record.getAuditRemark());
        vo.setReadOnly(computeReadOnly(record, task, user, forceReadOnly));
        vo.setEnabledModuleKeys(new ArrayList<>(dwTaskModuleService.resolveEnabledModuleKeys(record.getTaskId())));

        // 所有附件按 subRecordId + moduleType + slot 分组
        List<DwAttachment> allAtts = attachmentMapper.listByRecord(rid);
        Map<String, List<DwAttachment>> attMap = allAtts.stream()
                .collect(Collectors.groupingBy(a ->
                        a.getModuleType() + "|" + nvlId(a.getSubRecordId()) + "|" + a.getSlot()));

        // 所有扩展字段值，按 "moduleKey|subRecordId" 分组
        Map<String, Map<String, String>> extraMap = configService.loadAllValues(rid);
        vo.setModuleSelfScores(buildModuleSelfScores(extraMap));

        // 质控会议
        vo.setMeetings(meetingMapper.listByRecord(rid).stream().map(m -> {
            DwMeetingVO mv = new DwMeetingVO();
            BeanUtils.copyProperties(m, mv);
            mv.setMeetingStartDate(formatDate(m.getMeetingStartDate()));
            mv.setMeetingEndDate(formatDate(m.getMeetingEndDate()));
            fillStartQuarter(mv, m.getMeetingStartDate());
            mv.setMinutes(toVOList(attMap.get("meeting|" + m.getId() + "|minutes")));
            mv.setPhotos(toVOList(attMap.get("meeting|" + m.getId() + "|photo")));
            mv.setSignins(toVOList(attMap.get("meeting|" + m.getId() + "|signin")));
            mv.setExtraValues(extraMap.getOrDefault("meeting|" + m.getId(), Collections.emptyMap()));
            return mv;
        }).collect(Collectors.toList()));

        // 质控培训
        vo.setTrainings(trainingMapper.listByRecord(rid).stream().map(t -> {
            DwTrainingVO tv = new DwTrainingVO();
            BeanUtils.copyProperties(t, tv);
            tv.setTrainingStartDate(formatDate(t.getTrainingStartDate()));
            tv.setTrainingEndDate(formatDate(t.getTrainingEndDate()));
            fillStartQuarter(tv, t.getTrainingStartDate());
            tv.setMaterials(toVOList(attMap.get("training|" + t.getId() + "|material")));
            tv.setPhotos(toVOList(attMap.get("training|" + t.getId() + "|photo")));
            tv.setExtraValues(extraMap.getOrDefault("training|" + t.getId(), Collections.emptyMap()));
            return tv;
        }).collect(Collectors.toList()));

        // 质控指导（含市/县质控中心名称反查及分组，数据来自内存缓存）
        Map<Integer, String> regionMap = buildRegionMap();
        vo.setGuidances(guidanceMapper.listByRecord(rid).stream().map(g -> {
            DwGuidanceVO gv = new DwGuidanceVO();
            BeanUtils.copyProperties(g, gv);
            gv.setGuidanceStartDate(formatDate(g.getGuidanceStartDate()));
            gv.setGuidanceEndDate(formatDate(g.getGuidanceEndDate()));
            fillStartQuarter(gv, g.getGuidanceStartDate());
            gv.setCityCenterNames(resolveRegionNames(g.getCityCenterIds(), regionMap));
            gv.setCountyCenterNames(resolveRegionNames(g.getCountyCenterIds(), regionMap));
            gv.setCountyCenterGroups(groupCountyCenters(g.getCountyCenterIds(), regionMap));
            gv.setEvidences(toVOList(attMap.get("guidance|" + g.getId() + "|evidence")));
            gv.setExtraValues(extraMap.getOrDefault("guidance|" + g.getId(), Collections.emptyMap()));
            return gv;
        }).collect(Collectors.toList()));

        // 质控调研
        vo.setSurveys(surveyMapper.listByRecord(rid).stream().map(s -> {
            DwSurveyVO sv = new DwSurveyVO();
            BeanUtils.copyProperties(s, sv);
            sv.setSurveyStartDate(formatDate(s.getSurveyStartDate()));
            sv.setSurveyEndDate(formatDate(s.getSurveyEndDate()));
            fillStartQuarter(sv, s.getSurveyStartDate());
            sv.setReports(toVOList(attMap.get("survey|" + s.getId() + "|report")));
            sv.setPhotos(toVOList(attMap.get("survey|" + s.getId() + "|photo")));
            sv.setExtraValues(extraMap.getOrDefault("survey|" + s.getId(), Collections.emptyMap()));
            return sv;
        }).collect(Collectors.toList()));

        // 纯上传模块（含 record 级扩展字段）
        vo.setAnnualWorkFiles(toVOList(attMap.get("annual_work|null|evidence")));
        vo.setAnnualWorkExtra(extraMap.getOrDefault("annual_work|null", Collections.emptyMap()));

        vo.setItConstructionFiles(toVOList(attMap.get("it_construction|null|evidence")));
        vo.setItConstructionExtra(extraMap.getOrDefault("it_construction|null", Collections.emptyMap()));

        Map<String, List<DwAttachmentVO>> workPlanMap = new LinkedHashMap<>();
        workPlanMap.put("plan",    toVOList(attMap.get("work_plan|null|plan")));
        workPlanMap.put("summary", toVOList(attMap.get("work_plan|null|summary")));
        vo.setWorkPlanFiles(workPlanMap);
        vo.setWorkPlanExtra(extraMap.getOrDefault("work_plan|null", Collections.emptyMap()));

        vo.setAdminResponseFiles(toVOList(attMap.get("admin_response|null|evidence")));
        vo.setAdminResponseExtra(extraMap.getOrDefault("admin_response|null", Collections.emptyMap()));

        Map<String, List<DwAttachmentVO>> activityMap = new LinkedHashMap<>();
        activityMap.put("pre_report",  toVOList(attMap.get("activity_report|null|pre_report")));
        activityMap.put("post_report", toVOList(attMap.get("activity_report|null|post_report")));
        vo.setActivityReportFiles(activityMap);
        vo.setActivityReportExtra(extraMap.getOrDefault("activity_report|null", Collections.emptyMap()));

        // 经费执行
        vo.setFunding(fundingMapper.findByRecord(rid));
        vo.setFundingExtra(extraMap.getOrDefault("funding|null", Collections.emptyMap()));

        // 加分项
        vo.setBonuses(bonusMapper.listByRecord(rid).stream().map(b -> {
            DwBonusVO bv = new DwBonusVO();
            BeanUtils.copyProperties(b, bv);
            bv.setPubDate(b.getPubDate() != null ? b.getPubDate().format(DATE_FMT) : null);
            bv.setCompStartDate(formatDate(b.getCompStartDate()));
            bv.setCompEndDate(formatDate(b.getCompEndDate()));
            bv.setEvidences(toVOList(attMap.get("bonus|" + b.getId() + "|evidence")));
            bv.setExtraValues(extraMap.getOrDefault("bonus|" + b.getId(), Collections.emptyMap()));
            return bv;
        }).collect(Collectors.toList()));

        return vo;
    }

    // ─────────────────────────────────────────────────────────────
    // 内部工具方法
    // ─────────────────────────────────────────────────────────────

    private WrRecord requireRecord(Long recordId) {
        WrRecord r = recordMapper.selectById(recordId);
        if (r == null || r.getDelFlag() == 1)
            throw new BusinessException(404, "填报记录不存在");
        return r;
    }

    private void requireEditableRecord(Long recordId, LoginUser user) {
        WrRecord r = requireRecord(recordId);
        if (!user.isAdmin() && !r.getOrgId().equals(user.getOrgId()))
            throw new BusinessException(403, "无权操作");
        if (!user.isAdmin() && r.getStatus() != 0 && r.getStatus() != 3)
            throw new BusinessException(400, "记录已提交，不可修改");
    }

    private List<DwAttachmentVO> toVOList(List<DwAttachment> list) {
        if (list == null) return Collections.emptyList();
        return list.stream().map(a -> {
            DwAttachmentVO v = new DwAttachmentVO();
            BeanUtils.copyProperties(a, v);
            return v;
        }).collect(Collectors.toList());
    }

    private int nvl(Integer v) { return v == null ? 0 : v; }
    private String nvlId(Long v) { return v == null ? "null" : String.valueOf(v); }
    private String formatDate(LocalDate date) { return date != null ? date.format(DATE_FMT) : null; }

    /** 自然季度：Q1=1–3 月，…，Q4=10–12 月；按开始日期计算，供列表分季度配色 */
    private void fillStartQuarter(DwMeetingVO vo, LocalDate start) {
        QuarterHint h = QuarterHint.of(start);
        vo.setStartYearQuarter(h.startYearQuarter);
        vo.setQuarterIndex(h.quarterIndex);
    }

    private void fillStartQuarter(DwTrainingVO vo, LocalDate start) {
        QuarterHint h = QuarterHint.of(start);
        vo.setStartYearQuarter(h.startYearQuarter);
        vo.setQuarterIndex(h.quarterIndex);
    }

    private void fillStartQuarter(DwGuidanceVO vo, LocalDate start) {
        QuarterHint h = QuarterHint.of(start);
        vo.setStartYearQuarter(h.startYearQuarter);
        vo.setQuarterIndex(h.quarterIndex);
    }

    private void fillStartQuarter(DwSurveyVO vo, LocalDate start) {
        QuarterHint h = QuarterHint.of(start);
        vo.setStartYearQuarter(h.startYearQuarter);
        vo.setQuarterIndex(h.quarterIndex);
    }

    private static final class QuarterHint {
        final String startYearQuarter;
        final Integer quarterIndex;

        private QuarterHint(String startYearQuarter, Integer quarterIndex) {
            this.startYearQuarter = startYearQuarter;
            this.quarterIndex = quarterIndex;
        }

        static QuarterHint of(LocalDate d) {
            if (d == null) {
                return new QuarterHint(null, null);
            }
            int q = (d.getMonthValue() - 1) / 3 + 1;
            return new QuarterHint(d.getYear() + "-Q" + q, q);
        }
    }
    private java.math.BigDecimal parseDecimal(String v) {
        if (v == null || v.trim().isEmpty()) return null;
        try {
            return new java.math.BigDecimal(v.trim());
        } catch (Exception ignore) {
            return null;
        }
    }

    private Map<String, java.math.BigDecimal> buildModuleSelfScores(Map<String, Map<String, String>> extraMap) {
        List<String> modules = Arrays.asList(
                "meeting", "training", "guidance", "survey",
                "annual_work", "it_construction", "work_plan",
                "admin_response", "activity_report", "funding",
                "bonus_pub", "bonus_comp"
        );
        Map<String, java.math.BigDecimal> map = new LinkedHashMap<>();
        for (String module : modules) {
            Map<String, String> values = extraMap.getOrDefault(module + "|null", Collections.emptyMap());
            map.put(module, parseDecimal(values.get("module_self_score")));
        }
        return map;
    }

    private HalfDayRange normalizeHalfDayRange(
            LocalDate startDate, String startHalf,
            LocalDate endDate, String endHalf,
            String label) {
        boolean required = notBlank(label);
        if (!required && startDate == null && endDate == null && !notBlank(startHalf) && !notBlank(endHalf)) {
            return new HalfDayRange(null, null, null, null);
        }
        if (startDate == null || endDate == null || !notBlank(startHalf) || !notBlank(endHalf)) {
            throw new BusinessException(400, (required ? label : "时间区间") + "需同时提供开始日期/时段和结束日期/时段");
        }
        String sh = normalizeHalf(startHalf, label);
        String eh = normalizeHalf(endHalf, label);
        String prefix = notBlank(label) ? label : "时间区间";
        if (compareHalfDay(startDate, sh, endDate, eh) > 0) {
            throw new BusinessException(400, prefix + "开始时间不能晚于结束时间");
        }
        return new HalfDayRange(startDate, sh, endDate, eh);
    }

    private String normalizeHalf(String half, String label) {
        String v = half == null ? null : half.trim().toUpperCase(Locale.ROOT);
        if (!"AM".equals(v) && !"PM".equals(v)) {
            String prefix = notBlank(label) ? label : "时间区间";
            throw new BusinessException(400, prefix + "时段仅支持 AM/PM");
        }
        return v;
    }

    private int compareHalfDay(LocalDate startDate, String startHalf, LocalDate endDate, String endHalf) {
        int dateCmp = startDate.compareTo(endDate);
        if (dateCmp != 0) return dateCmp;
        return Integer.compare(halfOrder(startHalf), halfOrder(endHalf));
    }

    private boolean computeReadOnly(WrRecord record, WrTask task, LoginUser user, boolean forceReadOnly) {
        if (forceReadOnly) return true;
        if (user == null) return true;
        if (user.isAdmin()) return false;
        if (task != null && task.getStatus() != null && task.getStatus() != 1) return true;
        Integer st = record.getStatus();
        return st != null && st != 0 && st != 3;
    }

    private static void assertNotPlaceholderOnly(String fieldLabel, String value) {
        if (value == null || value.trim().isEmpty())
            throw new BusinessException(400, fieldLabel + "不能为空");
        boolean onlyGarbage = value.trim().chars()
                .allMatch(c -> c == '?' || c == '\uFFFD' || Character.isWhitespace(c));
        if (onlyGarbage)
            throw new BusinessException(400, fieldLabel + "无效，请填写真实文字");
    }

    private int halfOrder(String half) { return "AM".equals(half) ? 0 : 1; }
    private boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }

    private static class HalfDayRange {
        private final LocalDate startDate;
        private final String startHalf;
        private final LocalDate endDate;
        private final String endHalf;

        private HalfDayRange(LocalDate startDate, String startHalf, LocalDate endDate, String endHalf) {
            this.startDate = startDate;
            this.startHalf = startHalf;
            this.endDate = endDate;
            this.endHalf = endHalf;
        }
    }

    /**
     * 从内存缓存构建 regionId → name 全量查找表。
     * 每次 buildDetail 调用一次，整个方法内复用，无 DB 访问。
     */
    private Map<Integer, String> buildRegionMap() {
        Map<Integer, String> map = new HashMap<>();
        com.kxhospital.wreport.pojo.response.GuidanceRegionsVO regions = regionCache.get();
        if (regions == null) return map;
        if (regions.getCityTree() != null) {
            for (RegionNodeVO node : regions.getCityTree()) {
                map.put(node.getId(), node.getName());
            }
        }
        if (regions.getCountyTree() != null) {
            for (RegionNodeVO city : regions.getCountyTree()) {
                map.put(city.getId(), city.getName());
                if (city.getChildren() != null) {
                    for (RegionNodeVO county : city.getChildren()) {
                        map.put(county.getId(), county.getName());
                    }
                }
            }
        }
        return map;
    }

    /**
     * 将县级中心 ID 列表按所属市分组。
     * ID 规则：区县 ID ÷ 100 = 所属市节点 ID（如 20101 ÷ 100 = 201 = 杭州市）。
     * 分组顺序依照原始 ID 首次出现的市顺序，组内顺序保持原始顺序。
     */
    private List<DwRecordDetailVO.CountyCenterGroupVO> groupCountyCenters(
            String jsonIds, Map<Integer, String> regionMap) {
        if (jsonIds == null || jsonIds.trim().isEmpty()) return Collections.emptyList();
        String trimmed = jsonIds.trim().replaceAll("[\\[\\]\\s]", "");
        if (trimmed.isEmpty() || "null".equals(trimmed)) return Collections.emptyList();

        // 按市节点 ID 有序分组（LinkedHashMap 保持首次出现顺序）
        Map<Integer, List<String>> grouped = new LinkedHashMap<>();
        for (String s : trimmed.split(",")) {
            if (s.isEmpty()) continue;
            try {
                int countyId = Integer.parseInt(s);
                int cityId   = countyId / 100;          // 20101 → 201
                String countyName = regionMap.getOrDefault(countyId, s);
                grouped.computeIfAbsent(cityId, k -> new ArrayList<>()).add(countyName);
            } catch (NumberFormatException ignored) { }
        }

        List<DwRecordDetailVO.CountyCenterGroupVO> result = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> entry : grouped.entrySet()) {
            DwRecordDetailVO.CountyCenterGroupVO g = new DwRecordDetailVO.CountyCenterGroupVO();
            g.setCityName(regionMap.getOrDefault(entry.getKey(), String.valueOf(entry.getKey())));
            g.setCounties(entry.getValue());
            result.add(g);
        }
        return result;
    }

    /**
     * 将 JSON 整数数组字符串（如 "[101,103]"）解析并映射为名称列表。
     * 若某 ID 不在 regionMap 中则原样保留 ID 字符串，保证数据不丢失。
     */
    private List<String> resolveRegionNames(String jsonIds, Map<Integer, String> regionMap) {
        if (jsonIds == null || jsonIds.trim().isEmpty()) return Collections.emptyList();
        String trimmed = jsonIds.trim();
        if ("[]".equals(trimmed) || "null".equals(trimmed)) return Collections.emptyList();
        trimmed = trimmed.replaceAll("[\\[\\]\\s]", "");
        if (trimmed.isEmpty()) return Collections.emptyList();
        return Arrays.stream(trimmed.split(","))
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return regionMap.getOrDefault(Integer.parseInt(s), s);
                    } catch (NumberFormatException e) {
                        return s;
                    }
                })
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    // 管理端汇总视图
    // ─────────────────────────────────────────────────────────────

    @Override
    public DwAdminOverviewVO adminOverview(Long taskId) {
        // 1. 获取任务范围内所有机构（含尚未填报的），并附带当前记录状态
        List<TaskScopeOrgVO> scopeList = taskOrgScopeMapper.selectScopeWithStatus(taskId);

        // 2. 批量获取已存在的 wr_record
        List<WrRecord> records = recordMapper.selectByTaskId(taskId);
        Map<Long, WrRecord> recordByOrg = records.stream()
                .collect(Collectors.toMap(WrRecord::getOrgId, r -> r, (a, b) -> a));

        // 3. 批量查各模块计数（5 条 SQL 代替 N×5 条）
        List<Long> recordIds = records.stream().map(WrRecord::getId).collect(Collectors.toList());
        Map<Long, Integer> meetingCounts  = Collections.emptyMap();
        Map<Long, Integer> trainingCounts = Collections.emptyMap();
        Map<Long, Integer> guidanceCounts = Collections.emptyMap();
        Map<Long, Integer> surveyCounts   = Collections.emptyMap();
        Map<Long, Integer> bonusCounts    = Collections.emptyMap();
        java.util.Set<Long> fundingSet    = Collections.emptySet();

        if (!recordIds.isEmpty()) {
            meetingCounts  = toCountMap(meetingMapper.countByRecordIds(recordIds));
            trainingCounts = toCountMap(trainingMapper.countByRecordIds(recordIds));
            guidanceCounts = toCountMap(guidanceMapper.countByRecordIds(recordIds));
            surveyCounts   = toCountMap(surveyMapper.countByRecordIds(recordIds));
            bonusCounts    = toCountMap(bonusMapper.countByRecordIds(recordIds));
            fundingSet     = new java.util.HashSet<>(fundingMapper.existingRecordIds(recordIds));
        }

        // 4. 组装结果
        long notStarted = 0, draft = 0, submitted = 0, approved = 0, rejected = 0;
        List<DwAdminOverviewVO.OrgRow> orgRows = new ArrayList<>();

        for (TaskScopeOrgVO scope : scopeList) {
            DwAdminOverviewVO.OrgRow row = new DwAdminOverviewVO.OrgRow();
            row.setOrgId(scope.getOrgId());
            row.setOrgName(scope.getOrgName());

            WrRecord rec = recordByOrg.get(scope.getOrgId());
            if (rec == null) {
                row.setRecordId(null);
                row.setStatus(null);
                row.setStatusLabel("未开始");
                notStarted++;
            } else {
                Long rid = rec.getId();
                row.setRecordId(rid);
                row.setStatus(rec.getStatus());
                row.setStatusLabel(dwStatusLabel(rec.getStatus()));
                switch (rec.getStatus()) {
                    case 0: draft++;     break;
                    case 1: submitted++; break;
                    case 2: approved++;  break;
                    case 3: rejected++;  break;
                    default: break;
                }
                row.setMeetingCount(meetingCounts.getOrDefault(rid, 0));
                row.setTrainingCount(trainingCounts.getOrDefault(rid, 0));
                row.setGuidanceCount(guidanceCounts.getOrDefault(rid, 0));
                row.setSurveyCount(surveyCounts.getOrDefault(rid, 0));
                row.setHasFunding(fundingSet.contains(rid));
                row.setBonusCount(bonusCounts.getOrDefault(rid, 0));
            }
            orgRows.add(row);
        }

        DwAdminOverviewVO vo = new DwAdminOverviewVO();
        vo.setTotal(scopeList.size());
        vo.setNotStarted(notStarted);
        vo.setDraft(draft);
        vo.setSubmitted(submitted);
        vo.setApproved(approved);
        vo.setRejected(rejected);
        vo.setOrgRows(orgRows);
        return vo;
    }

    /**
     * 将 [{recordid: x, cnt: n}, ...] 转为 Map<recordId, count>。
     * PostgreSQL 默认返回小写列名，兼容大小写查找。
     */
    private Map<Long, Integer> toCountMap(List<Map<String, Object>> rows) {
        Map<Long, Integer> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            // PostgreSQL 列别名统一小写；MyBatis 传回的 key 可能是 recordid 或 recordId
            Object rid = row.get("rid");
            Object cnt = row.get("cnt");
            if (rid != null && cnt != null) {
                map.put(((Number) rid).longValue(), ((Number) cnt).intValue());
            }
        }
        return map;
    }

    private String dwStatusLabel(Integer status) {
        if (status == null) return "未开始";
        switch (status) {
            case 0: return "草稿";
            case 1: return "已提交";
            case 2: return "已通过";
            case 3: return "已驳回";
            default: return String.valueOf(status);
        }
    }
}
