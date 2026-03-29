package com.kxhospital.wreport.cache;

import com.kxhospital.wreport.entity.DwRegion;
import com.kxhospital.wreport.mapper.DwRegionMapper;
import com.kxhospital.wreport.pojo.response.GuidanceRegionsVO;
import com.kxhospital.wreport.pojo.response.RegionNodeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 质控指导地区树缓存。
 * 应用启动后读取一次 dw_region 表，构建两棵树并常驻内存；
 * 运行期间不再访问数据库，也不自动刷新（行政区划变更概率极低）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DwRegionCache implements ApplicationRunner {

    private final DwRegionMapper regionMapper;

    private volatile GuidanceRegionsVO cachedRegions;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[DwRegionCache] 开始加载地区树数据...");
        cachedRegions = buildRegions();
        log.info("[DwRegionCache] 加载完成：cityTree={} 个节点，countyTree={} 个顶级节点",
                cachedRegions.getCityTree().size(),
                cachedRegions.getCountyTree().size());
    }

    /** 获取缓存数据（启动后始终不为 null） */
    public GuidanceRegionsVO get() {
        return cachedRegions;
    }

    // ─────────────────────────────────────────────────────────────────────────

    private GuidanceRegionsVO buildRegions() {
        GuidanceRegionsVO vo = new GuidanceRegionsVO();
        vo.setCityTree(buildCityTree());
        vo.setCountyTree(buildCountyTree());
        return vo;
    }

    /** city 树：所有节点都是叶节点，直接平铺 */
    private List<RegionNodeVO> buildCityTree() {
        List<DwRegion> rows = regionMapper.listByTreeType("city");
        List<RegionNodeVO> list = new ArrayList<>(rows.size());
        for (DwRegion r : rows) {
            RegionNodeVO node = new RegionNodeVO();
            node.setId(r.getId());
            node.setName(r.getName());
            list.add(node);
        }
        return list;
    }

    /** county 树：市节点(level=2) + 区县节点(level=3)，组装成两级树 */
    private List<RegionNodeVO> buildCountyTree() {
        List<DwRegion> rows = regionMapper.listByTreeType("county");

        // 先按顺序收集市节点
        Map<Integer, RegionNodeVO> cityMap = new LinkedHashMap<>();
        for (DwRegion r : rows) {
            if (r.getLevel() != null && r.getLevel() == 2) {
                RegionNodeVO node = new RegionNodeVO();
                node.setId(r.getId());
                node.setName(r.getName());
                node.setChildren(new ArrayList<>());
                cityMap.put(r.getId(), node);
            }
        }

        // 挂载区县子节点
        for (DwRegion r : rows) {
            if (r.getLevel() != null && r.getLevel() == 3 && r.getParentId() != null) {
                RegionNodeVO city = cityMap.get(r.getParentId());
                if (city != null) {
                    RegionNodeVO county = new RegionNodeVO();
                    county.setId(r.getId());
                    county.setName(r.getName());
                    city.getChildren().add(county);
                }
            }
        }

        return new ArrayList<>(cityMap.values());
    }
}
