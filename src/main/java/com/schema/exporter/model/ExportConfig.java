package com.schema.exporter.model;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 내보내기 설정 - Spring @Value 로 database.properties 주입
 */
@Data
@Component
public class ExportConfig {

    @Value("${db.schema}")
    private String schema;

    @Value("${export.tables:}")
    private String tablesRaw;

    @Value("${excel.output.path:./output}")
    private String outputPath;

    @Value("${excel.output.filename:DB2_Schema_Export}")
    private String outputFilename;

    @Value("${excel.include.date:true}")
    private boolean includeDate;

    @Value("${ddl.include.primary.key:true}")
    private boolean includePrimaryKey;

    @Value("${ddl.include.index:true}")
    private boolean includeIndex;

    @Value("${ddl.include.comment:true}")
    private boolean includeComment;

    @Value("${ddl.tablespace:USERSPACE1}")
    private String tablespace;

    public List<String> getTargetTables() {
        if (tablesRaw == null || tablesRaw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(tablesRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
