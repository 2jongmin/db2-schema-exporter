package com.schema.exporter.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DB2 테이블 정보 모델
 */
@Data
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

    /** 생성 DDL */
    private String ddlScript;

    /** 테이블스페이스 */
    private String tablespace;

    /** 레코드 건수 (선택) */
    private Long rowCount;
}
