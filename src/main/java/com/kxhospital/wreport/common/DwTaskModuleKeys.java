package com.kxhospital.wreport.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** 日常工作任务标准模块范围（与前端 dwTaskModules.js / wr_api_guide 保持一致） */
public final class DwTaskModuleKeys {

    private DwTaskModuleKeys() {}

    public static final List<String> QUARTER_MODULES = Collections.unmodifiableList(Arrays.asList(
            "meeting", "training", "guidance", "survey"
    ));

    public static final List<String> ANNUAL_MODULES = Collections.unmodifiableList(Arrays.asList(
            "meeting", "training", "guidance", "survey",
            "work_plan", "annual_work",
            "national_report", "prov_report",
            "activity_report", "funding",
            "bonus_pub", "bonus_comp", "bonus_admin"
    ));

    /** 年度任务不再启用的模块（兼容旧 scope / 自动补齐） */
    public static final List<String> ANNUAL_EXCLUDED_MODULES = Collections.unmodifiableList(Arrays.asList(
            "indicator_db", "indicator_monitor", "network_build"
    ));
}
