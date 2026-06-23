package com.schema.exporter.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 내보내기 설정 - Spring @Value 로 database.properties 주입
 */
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

    // ── Getters ──
    public String  getSchema()         { return schema; }
    public String  getTablesRaw()      { return tablesRaw; }
    public String  getOutputPath()     { return outputPath; }
    public String  getOutputFilename() { return outputFilename; }
    public boolean isIncludeDate()     { return includeDate; }
    public boolean isIncludePrimaryKey() { return includePrimaryKey; }
    public boolean isIncludeIndex()    { return includeIndex; }
    public boolean isIncludeComment()  { return includeComment; }
    public String  getTablespace()     { return tablespace; }

    // ── Setters ──
    public void setSchema(String schema)                   { this.schema = schema; }
    public void setTablesRaw(String tablesRaw)             { this.tablesRaw = tablesRaw; }
    public void setOutputPath(String outputPath)           { this.outputPath = outputPath; }
    public void setOutputFilename(String outputFilename)   { this.outputFilename = outputFilename; }
    public void setIncludeDate(boolean includeDate)        { this.includeDate = includeDate; }
    public void setIncludePrimaryKey(boolean v)            { this.includePrimaryKey = v; }
    public void setIncludeIndex(boolean includeIndex)      { this.includeIndex = includeIndex; }
    public void setIncludeComment(boolean includeComment)  { this.includeComment = includeComment; }
    public void setTablespace(String tablespace)           { this.tablespace = tablespace; }

    /**
     * export.tables 값을 파싱하여 테이블 목록 반환
     * 비어 있으면 빈 리스트 반환 → 전체 테이블 추출
     */
    public List<String> getTargetTables() {
        if (tablesRaw == null || tablesRaw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(tablesRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
