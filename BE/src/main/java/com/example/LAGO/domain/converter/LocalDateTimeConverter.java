package com.example.LAGO.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Converter
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, String> {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter POSTGRES_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
    private static final DateTimeFormatter SIMPLE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String convertToDatabaseColumn(LocalDateTime attribute) {
        return attribute == null ? null : attribute.format(ISO_FORMATTER);
    }

    @Override
    public LocalDateTime convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        
        try {
            // Try ISO format first (e.g., "2025-08-13T09:05:43")
            return LocalDateTime.parse(dbData, ISO_FORMATTER);
        } catch (DateTimeParseException e1) {
            try {
                // Try PostgreSQL timestamp with microseconds (e.g., "2025-08-12 15:19:01.315081")
                return LocalDateTime.parse(dbData, POSTGRES_FORMATTER);
            } catch (DateTimeParseException e2) {
                try {
                    // Try simple timestamp without microseconds (e.g., "2025-08-12 15:19:01")
                    return LocalDateTime.parse(dbData, SIMPLE_FORMATTER);
                } catch (DateTimeParseException e3) {
                    throw new IllegalArgumentException("Unable to parse LocalDateTime from: " + dbData, e3);
                }
            }
        }
    }
}
