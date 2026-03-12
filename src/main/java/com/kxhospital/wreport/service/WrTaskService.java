package com.kxhospital.wreport.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kxhospital.wreport.entity.WrTask;

import java.util.List;

public interface WrTaskService {
    IPage<WrTask> page(Page<WrTask> page, String taskName, Integer status);
    WrTask detail(Long id);
    Long add(WrTask task);
    void update(WrTask task);
    void updateStatus(Long id, Integer status);
    void delete(Long id);
    List<WrTask> activeList(Long orgId);
}
