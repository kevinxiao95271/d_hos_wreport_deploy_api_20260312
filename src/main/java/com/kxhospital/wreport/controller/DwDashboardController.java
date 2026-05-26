package com.kxhospital.wreport.controller;

import com.kxhospital.wreport.common.LoginUser;
import com.kxhospital.wreport.common.R;
import com.kxhospital.wreport.common.UserContext;
import com.kxhospital.wreport.pojo.response.DwDashboardModuleStatsVO;
import com.kxhospital.wreport.service.DwRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 日常工作 — 数据看板
 */
@Tag(name = "日常工作 — 数据看板")
@RestController
@RequestMapping("/dw/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class DwDashboardController {

    private final DwRecordService dwRecordService;

    @Operation(summary = "模块统计看板",
            description = "按任务与机构维度（质控中心/技术指导中心/所有）统计各启用模块的填报数量，供柱状图展示")
    @GetMapping("/module-stats")
    public R<DwDashboardModuleStatsVO> moduleStats(@RequestParam Long taskId,
                                                   @RequestParam(required = false) Integer orgCategory) {
        requireAdmin();
        return R.ok(dwRecordService.dashboardModuleStats(taskId, orgCategory));
    }

    private LoginUser requireAdmin() {
        LoginUser u = UserContext.get();
        if (u == null) {
            throw new com.kxhospital.wreport.common.BusinessException(401, "未登录");
        }
        if (!u.isAdmin()) {
            throw new com.kxhospital.wreport.common.BusinessException(403, "无权限");
        }
        return u;
    }
}
