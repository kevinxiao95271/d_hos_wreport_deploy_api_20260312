package com.kxhospital.wreport.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

/**
 * 应用启动时执行 wr_module_ddl.sql，使用 IF NOT EXISTS 保证幂等
 * Order(1) 在 MinioInitializer(默认最低) 之前
 */
@Slf4j
@Order(1)
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            ClassPathResource res = new ClassPathResource("sql/wr_ddl_init.sql");
            if (!res.exists()) {
                log.warn("[DB] wr_module_ddl.sql not found, skipping DDL init");
                return;
            }
            String ddl = StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8);
            // Split on ; to execute each statement individually (skip empty)
            String[] stmts = ddl.split(";");
            int count = 0;
            for (String stmt : stmts) {
                String s = stmt.trim();
                if (s.isEmpty()) continue;
                // Skip pure-comment lines (SQL line comments)
                if (s.replaceAll("--[^\n]*\n?", "").trim().isEmpty()) continue;
                try {
                    jdbcTemplate.execute(s);
                    count++;
                } catch (Exception e) {
                    // IF NOT EXISTS handles most cases; log warnings for others
                    log.warn("[DB] DDL statement skipped ({}): {}", e.getMessage(), s.substring(0, Math.min(80, s.length())));
                }
            }
            log.info("[DB] DDL init done, executed {} statements", count);
        } catch (Exception e) {
            log.error("[DB] DDL init failed: {}", e.getMessage(), e);
        }
    }
}
