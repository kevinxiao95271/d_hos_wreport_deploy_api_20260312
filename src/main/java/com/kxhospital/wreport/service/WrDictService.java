package com.kxhospital.wreport.service;

import com.kxhospital.wreport.entity.WrDictItem;
import com.kxhospital.wreport.entity.WrDictType;

import java.util.List;

public interface WrDictService {

    List<WrDictType> listTypes();

    WrDictType getTypeByCode(String dictCode);

    void saveType(WrDictType dictType);

    void deleteType(Long id);

    List<WrDictItem> listItems(String dictCode);

    /** 全量覆盖某字典的条目 */
    void replaceItems(Long dictTypeId, List<WrDictItem> items);
}
