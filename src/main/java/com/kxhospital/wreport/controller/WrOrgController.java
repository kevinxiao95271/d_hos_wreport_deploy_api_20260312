package com.kxhospital.wreport.controller;

import com.kxhospital.wreport.common.R;
import com.kxhospital.wreport.mapper.SysUserMapper;
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
 * <p>供管理员在"分配机构"弹窗中加载可选机构列表，替代已下线的调试接口 GET /api/auth/users。</p>
 * <p>数据链路：sys_user → hr_person → hr_organization（不使用 sys_user.org_id）。</p>
 */
@Tag(name = "机构管理")
@RestController
@RequestMapping("/wr/org")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class WrOrgController {

    private final SysUserMapper sysUserMapper;

    /**
     * 返回所有可分配给任务的机构用户列表（角色 qcUser，启用且未删除）。
     * <p>前端在任务"分配机构"弹窗中调用此接口渲染可选机构复选框。</p>
     * <p>返回字段：</p>
     * <ul>
     *   <li>userId  — sys_user.user_id（String，防精度丢失）</li>
     *   <li>account — 登录账号</li>
     *   <li>realName — 用户真实姓名</li>
     *   <li>orgId   — hr_person.org_id（String）</li>
     *   <li>orgName — hr_organization.org_name</li>
     * </ul>
     */
    @Operation(summary = "可分配机构列表（管理员）",
               description = "返回所有角色为 qcUser 的机构用户，供任务分配机构弹窗使用。" +
                             "数据通过 sys_user.person_id → hr_person → hr_organization 获取，保证机构名准确。")
    @ApiResponse(responseCode = "200", description = "success",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"code\":200,\"message\":\"success\",\"data\":[" +
                            "{\"userId\":\"7437930037937963010\",\"account\":\"qc_001\"," +
                            "\"realName\":\"省神经外科技术指导中心\",\"orgId\":\"7437930037585641473\"," +
                            "\"orgName\":\"省神经外科技术指导中心\"}]}")))
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list() {
        return R.ok(sysUserMapper.listOrgUsers());
    }
}
