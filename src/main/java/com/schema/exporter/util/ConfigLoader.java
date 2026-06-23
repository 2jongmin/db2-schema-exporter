package com.schema.exporter.util;

import com.schema.exporter.model.ExportConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * config.properties 로딩 유틸리티
 */
public class ConfigLoader {

    public static ExportConfig load() throws IOException {
        Properties props = new Properties();
        try (InputStream is = ConfigLoader.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (is == null) throw new IOException("config.properties 파일을 찾을 수 없습니다.");
            props.load(is);
        }

        List<String> targetTables = new ArrayList<>();
        String tablesVal = props.getProperty("db.tables", "").trim();
        if (!tablesVal.isEmpty()) {
            Arrays.stream(tablesVal.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(targetTables::add);
        }

        return ExportConfig.builder()
                .dbUrl(props.getProperty("db.url"))
                .dbUsername(props.getProperty("db.username"))
                .dbPassword(props.getProperty("db.password"))
                .schema(props.getProperty("db.schema", ""))
                .targetTables(targetTables)
                .outputPath(props.getProperty("excel.output.path", "./output"))
                .outputFilename(props.getProperty("excel.output.filename", "DB2_Schema_Export"))
                .includeDate(Boolean.parseBoolean(props.getProperty("excel.include.date", "true")))
                .includePrimaryKey(Boolean.parseBoolean(props.getProperty("ddl.include.primary.key", "true")))
                .includeIndex(Boolean.parseBoolean(props.getProperty("ddl.include.index", "true")))
                .includeComment(Boolean.parseBoolean(props.getProperty("ddl.include.comment", "true")))
                .tablespace(props.getProperty("ddl.tablespace", "USERSPACE1"))
                .build();
    }
}
