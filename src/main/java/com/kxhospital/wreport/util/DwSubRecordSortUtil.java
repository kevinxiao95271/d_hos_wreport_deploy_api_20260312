package com.kxhospital.wreport.util;

import com.kxhospital.wreport.pojo.response.DwRecordDetailVO.DwDataAnalysisVO;
import com.kxhospital.wreport.pojo.response.DwRecordDetailVO.DwGuidanceVO;
import com.kxhospital.wreport.pojo.response.DwRecordDetailVO.DwMeetingVO;
import com.kxhospital.wreport.pojo.response.DwRecordDetailVO.DwSurveyVO;
import com.kxhospital.wreport.pojo.response.DwRecordDetailVO.DwTrainingVO;

import java.util.Comparator;
import java.util.List;

/**
 * 会议/培训/指导/调研等子记录：按活动开始时间从早到晚排序（无开始日期排最后）。
 */
public final class DwSubRecordSortUtil {

    private DwSubRecordSortUtil() {
    }

    public static void sortMeetings(List<DwMeetingVO> list) {
        if (list == null || list.size() < 2) {
            return;
        }
        list.sort(Comparator
                .comparing(DwMeetingVO::getMeetingStartDate, DwSubRecordSortUtil::compareDateStrings)
                .thenComparing(m -> halfOrder(m.getMeetingStartHalf()))
                .thenComparing(m -> m.getId() == null ? Long.MAX_VALUE : m.getId()));
    }

    public static void sortTrainings(List<DwTrainingVO> list) {
        if (list == null || list.size() < 2) {
            return;
        }
        list.sort(Comparator
                .comparing(DwTrainingVO::getTrainingStartDate, DwSubRecordSortUtil::compareDateStrings)
                .thenComparing(t -> halfOrder(t.getTrainingStartHalf()))
                .thenComparing(t -> t.getId() == null ? Long.MAX_VALUE : t.getId()));
    }

    public static void sortGuidances(List<DwGuidanceVO> list) {
        if (list == null || list.size() < 2) {
            return;
        }
        list.sort(Comparator
                .comparing(DwGuidanceVO::getGuidanceStartDate, DwSubRecordSortUtil::compareDateStrings)
                .thenComparing(g -> halfOrder(g.getGuidanceStartHalf()))
                .thenComparing(g -> g.getId() == null ? Long.MAX_VALUE : g.getId()));
    }

    public static void sortSurveys(List<DwSurveyVO> list) {
        if (list == null || list.size() < 2) {
            return;
        }
        list.sort(Comparator
                .comparing(DwSurveyVO::getSurveyStartDate, DwSubRecordSortUtil::compareDateStrings)
                .thenComparing(s -> halfOrder(s.getSurveyStartHalf()))
                .thenComparing(s -> s.getId() == null ? Long.MAX_VALUE : s.getId()));
    }

    public static void sortDataAnalysisReports(List<DwDataAnalysisVO> list) {
        if (list == null || list.size() < 2) {
            return;
        }
        list.sort(Comparator
                .comparing(DwDataAnalysisVO::getReportDate, DwSubRecordSortUtil::compareDateStrings)
                .thenComparing(d -> d.getId() == null ? Long.MAX_VALUE : d.getId()));
    }

    /** yyyy-MM-dd 字符串升序；null 视为最大（排最后） */
    private static int compareDateStrings(String a, String b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        return a.compareTo(b);
    }

    /** AM/上午=0，PM/下午=1 */
    private static int halfOrder(String half) {
        if (half == null || half.trim().isEmpty()) {
            return 0;
        }
        String trimmed = half.trim();
        if ("PM".equalsIgnoreCase(trimmed) || "下午".equals(trimmed)) {
            return 1;
        }
        return 0;
    }
}
