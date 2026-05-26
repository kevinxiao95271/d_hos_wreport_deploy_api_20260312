package com.kxhospital.wreport.controller;

import com.kxhospital.wreport.common.QcOrgAssignExclusions;
import com.kxhospital.wreport.common.R;
import com.kxhospital.wreport.mapper.HrOrganizationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 机构列表接口。
 * <p>供管理员在"分配机构"弹窗中加载可选机构列表。</p>
 * <p>数据取自 hr_organization：organization_type='2' 且 org_category 为 1（质控中心）或 2（技术指导中心）。</p>
 */
@Tag(name = "机构管理")
@RestController
@RequestMapping("/wr/org")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class WrOrgController {

    private final HrOrganizationMapper hrOrganizationMapper;

    /**
     * 返回可分配给任务的机构列表（质控中心 + 技术指导中心，共约 68 家）。
     * <p>前端在任务"分配机构"弹窗中调用此接口渲染可选机构复选框，保存时使用 orgId。</p>
     * <p>返回字段：orgId、orgName、orgCategory（1=质控中心，2=技术指导中心）</p>
     */
    @Operation(summary = "可分配机构列表（管理员）",
               description = "返回 hr_organization 中 organization_type=2 且 org_category 为 1/2 的机构，供任务分配机构弹窗使用。")
    @ApiResponse(responseCode = "200", description = "success",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"code\":200,\"message\":\"success\",\"data\":[" +
                            "{\"orgId\":\"7437930037585641473\"," +
                            "\"orgName\":\"省神经外科技术指导中心\",\"orgCategory\":\"2\"}]}")))
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list() {
        return R.ok(QcOrgAssignExclusions.filterAssignableOrgs(hrOrganizationMapper.listQcOrgs(null)));
    }
}
