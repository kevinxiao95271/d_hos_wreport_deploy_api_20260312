package com.kxhospital.wreport.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kxhospital.wreport.entity.WrTask;
import com.kxhospital.wreport.mapper.WrTaskMapper;
import com.kxhospital.wreport.service.WrTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WrTaskServiceImpl implements WrTaskService {

    private final WrTaskMapper taskMapper;

    @Override
    public IPage<WrTask> page(Page<WrTask> page, String taskName, Integer status) {
        return taskMapper.selectPage(page, taskName, status);
    }

    @Override
    public WrTask detail(Long id) {
        return taskMapper.selectById(id);
    }

    @Override
    public Long add(WrTask task) {
        task.setStatus(0);
        taskMapper.insert(task);
        return task.getId();
    }

    @Override
    public void update(WrTask task) {
        taskMapper.updateById(task);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        LambdaUpdateWrapper<WrTask> w = new LambdaUpdateWrapper<>();
        w.eq(WrTask::getId, id).set(WrTask::getStatus, status);
        taskMapper.update(null, w);
    }

    @Override
    public void delete(Long id) {
        taskMapper.deleteById(id);
    }

    @Override
    public List<WrTask> activeList() {
        return taskMapper.selectActiveTasks();
    }
}
