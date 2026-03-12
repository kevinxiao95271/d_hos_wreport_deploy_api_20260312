package com.kxhospital.wreport.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Long → String（防 JS 精度丢失） + LocalDateTime 格式化 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            // Long → String
            SimpleModule longMod = new SimpleModule();
            longMod.addSerializer(Long.class, ToStringSerializer.instance);
            longMod.addSerializer(Long.TYPE,  ToStringSerializer.instance);

            // LocalDateTime ↔ String
            JavaTimeModule timeMod = new JavaTimeModule();
            timeMod.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DT_FMT));
            timeMod.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DT_FMT));

            builder.modules(longMod, timeMod)
                   .simpleDateFormat("yyyy-MM-dd HH:mm:ss");
        };
    }
}
