package com.schema.exporter.model;

import lombok.Builder;
import lombok.Data;

/**
 * DB2 테이블 컬럼 정보 모델
 */
@Data
@Builder
public class ColumnInfo {

    /** 순번 */
    private int ordinalPosition;

    /** 컬럼명 */
    private String columnName;

    /** 한글명 (컬럼 COMMENT) */
    private String columnComment;

    /** 데이터 타입 */
    private String dataType;

    /** 전체 타입 (길이 포함 e.g. VARCHAR(100)) */
    private String fullDataType;

    /** 길이/크기 */
    private Integer columnLength;

    /** 소수점 자리수 */
    private Integer decimalDigits;

    /** NOT NULL 여부 */
    private boolean nullable;

    /** PK 여부 */
    private boolean primaryKey;

    /** PK 순번 */
    private int pkPosition;

    /** 기본값 */
    private String defaultValue;

    /** 인덱스 여부 */
    private boolean indexed;

    /** 인덱스명 */
    private String indexName;
}
