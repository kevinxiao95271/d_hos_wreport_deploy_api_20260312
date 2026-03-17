package com.kxhospital.wreport.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kxhospital.wreport.entity.WrDictItem;
import com.kxhospital.wreport.entity.WrDictType;
import com.kxhospital.wreport.mapper.WrDictItemMapper;
import com.kxhospital.wreport.mapper.WrDictMapper;
import com.kxhospital.wreport.service.WrDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WrDictServiceImpl implements WrDictService {

    private final WrDictMapper     dictMapper;
    private final WrDictItemMapper dictItemMapper;

    @Override
    public List<WrDictType> listTypes() {
        return dictMapper.selectAllTypes();
    }

    @Override
    public WrDictType getTypeByCode(String dictCode) {
        return dictMapper.selectOne(new LambdaQueryWrapper<WrDictType>()
                .eq(WrDictType::getDictCode, dictCode)
                .eq(WrDictType::getDelFlag, 0));
    }

    @Override
    public void saveType(WrDictType dictType) {
        if (dictType.getId() == null) {
            dictMapper.insert(dictType);
        } else {
            dictMapper.updateById(dictType);
        }
    }

    @Override
    public void deleteType(Long id) {
        LambdaUpdateWrapper<WrDictType> uw = new LambdaUpdateWrapper<>();
        uw.eq(WrDictType::getId, id).set(WrDictType::getDelFlag, 1);
        dictMapper.update(null, uw);
        // 同步软删条目
        LambdaUpdateWrapper<WrDictItem> iuw = new LambdaUpdateWrapper<>();
        iuw.eq(WrDictItem::getDictTypeId, id).set(WrDictItem::getDelFlag, 1);
        dictItemMapper.update(null, iuw);
    }

    @Override
    public List<WrDictItem> listItems(String dictCode) {
        return dictMapper.selectItemsByCode(dictCode);
    }

    @Override
    @Transactional
    public void replaceItems(Long dictTypeId, List<WrDictItem> items) {
        // 软删旧条目
        LambdaUpdateWrapper<WrDictItem> uw = new LambdaUpdateWrapper<>();
        uw.eq(WrDictItem::getDictTypeId, dictTypeId).set(WrDictItem::getDelFlag, 1);
        dictItemMapper.update(null, uw);
        // 插入新条目
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                WrDictItem item = items.get(i);
                item.setDictTypeId(dictTypeId);
                item.setSortNum(i + 1);
                item.setDelFlag(0);
                item.setStatus(1);
                dictItemMapper.insert(item);
            }
        }
    }
}
