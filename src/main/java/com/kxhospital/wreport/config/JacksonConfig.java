package com.kxhospital.wreport.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

/** Long → String（防 JS 精度丢失） + LocalDateTime 格式化（兼容 ISO 8601 和空格格式） */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter OUT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 反序列化：兼容 "yyyy-MM-dd HH:mm:ss" 和 ISO 8601 "yyyy-MM-dd'T'HH:mm:ss" */
    private static final DateTimeFormatter IN_FMT = new DateTimeFormatterBuilder()
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"))
            .toFormatter();

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            SimpleModule longMod = new SimpleModule();
            longMod.addSerializer(Long.class, ToStringSerializer.instance);
            longMod.addSerializer(Long.TYPE,  ToStringSerializer.instance);

            JavaTimeModule timeMod = new JavaTimeModule();
            timeMod.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(OUT_FMT));
            timeMod.addDeserializer(LocalDateTime.class, new StdDeserializer<LocalDateTime>(LocalDateTime.class) {
                @Override
                public LocalDateTime deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
                    String text = p.getText().trim();
                    if (text.isEmpty()) return null;
                    return LocalDateTime.parse(text, IN_FMT);
                }
            });

            builder.modules(longMod, timeMod)
                   .simpleDateFormat("yyyy-MM-dd HH:mm:ss");
        };
    }
}
