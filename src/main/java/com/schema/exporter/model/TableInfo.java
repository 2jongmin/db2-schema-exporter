package com.schema.exporter.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * DB2 테이블 정보 모델
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableInfo {

    /** 스키마명 */
    private String schemaName;

    /** 테이블명 */
    private String tableName;

    /** 테이블 한글명 (COMMENT) */
    private String tableComment;

    /** 컬럼 목록 */
    private List<ColumnInfo> columns;

    /** 생성 DDL 스크립트 */
    private String ddlScript;

    /** 테이블스페이스 */
    private String tablespace;
}
