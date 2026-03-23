package com.kxhospital.wreport.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kxhospital.wreport.common.LoginUser;
import com.kxhospital.wreport.common.R;
import com.kxhospital.wreport.common.UserContext;
import com.kxhospital.wreport.entity.WrTask;
import com.kxhospital.wreport.pojo.request.TaskAddRequest;
import com.kxhospital.wreport.pojo.request.TaskScopeRequest;
import com.kxhospital.wreport.pojo.response.TaskScopeResponse;
import com.kxhospital.wreport.service.WrTaskService;
import com.kxhospital.wreport.service.impl.WrTaskServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "任务管理")
@RestController
@RequestMapping("/wr/task")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class WrTaskController {

    private final WrTaskService taskService;
    private final WrTaskServiceImpl taskServiceImpl;

    @Operation(summary = "分页查询任务（管理员）")
    @GetMapping("/page")
    public R<IPage<WrTask>> page(@RequestParam(defaultValue = "1") int pageNum,
                                  @RequestParam(defaultValue = "20") int pageSize,
                                  @RequestParam(required = false) String taskName,
                                  @RequestParam(required = false) Integer status) {
        requireAdmin();
        return R.ok(taskService.page(new Page<>(pageNum, pageSize), taskName, status));
    }

    @Operation(summary = "获取进行中的任务列表（机构用户）")
    @GetMapping("/active")
    public R<List<WrTask>> activeList() {
        LoginUser u = UserContext.get();
        if (u == null) throw new RuntimeException("未登录");
        return R.ok(taskService.activeList(u.getOrgId()));
    }

    @Operation(summary = "查询任务范围（管理员）",
               description = "返回该任务已分配机构列表。orgs 含机构名称与当前填报状态（recordStatus: null=未开始, 0=草稿, 1=已提交, 2=已审核, 3=已驳回）。" +
                             "recordStatus>=1 的机构在前端应置灰 checkbox，提示[请先驳回后再移出]。")
    @GetMapping("/scope/{taskId}")
    public R<TaskScopeResponse> getScope(@PathVariable Long taskId) {
        requireAdmin();
        TaskScopeResponse resp = new TaskScopeResponse();
        resp.setTaskId(taskId);
        resp.setOrgs(taskServiceImpl.getScopeWithStatus(taskId));
        resp.setOrgIds(taskServiceImpl.getScopeOrgIds(taskId));
        return R.ok(resp);
    }

    @Operation(summary = "设置任务范围（管理员，空列表=全量）")
    @PostMapping("/scope/{taskId}")
    public R<Void> setScope(@PathVariable Long taskId, @RequestBody TaskScopeRequest req) {
        LoginUser u = requireAdmin();
        taskServiceImpl.replaceScope(taskId, req != null ? req.getOrgIds() : null, u);
        return R.ok();
    }

    @Operation(summary = "任务详情")
    @GetMapping("/detail/{id}")
    public R<WrTask> detail(@PathVariable Long id) {
        return R.ok(taskService.detail(id));
    }

    @Operation(summary = "新增任务（管理员）")
    @PostMapping("/add")
    public R<Long> add(@Valid @RequestBody TaskAddRequest req) {
        LoginUser u = requireAdmin();
        WrTask task = new WrTask();
        BeanUtils.copyProperties(req, task);
        Long taskId = taskService.add(task);
        taskServiceImpl.replaceScope(taskId, req.getOrgIds(), u);
        return R.ok(taskId);
    }

    @Operation(summary = "修改任务（管理员）")
    @PostMapping("/update")
    public R<Void> update(@RequestBody WrTask task) {
        requireAdmin();
        taskService.update(task);
        return R.ok();
    }

    @Operation(summary = "修改任务状态 (0=草稿 1=进行中 2=已关闭)")
    @PostMapping("/status/{id}/{status}")
    public R<Void> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        requireAdmin();
        taskService.updateStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "删除任务（管理员）")
    @PostMapping("/delete/{id}")
    public R<Void> delete(@PathVariable Long id) {
        requireAdmin();
        taskService.delete(id);
        return R.ok();
    }

    private LoginUser requireAdmin() {
        LoginUser u = UserContext.get();
        if (u == null || !u.isAdmin())
            throw new com.kxhospital.wreport.common.BusinessException(403, "权限不足，需要管理员角色");
        return u;
    }
}
