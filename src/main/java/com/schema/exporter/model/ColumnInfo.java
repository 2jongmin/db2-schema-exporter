package com.schema.exporter.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * DB2 컬럼 정보 모델
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColumnInfo {

    /** 순번 */
    private int ordinalPosition;

    /** 컬럼명 */
    private String columnName;

    /** 한글명 (COMMENT) */
    private String columnComment;

    /** 데이터 타입 (e.g. VARCHAR) */
    private String dataType;

    /** 전체 타입 (e.g. VARCHAR(100)) - 서비스에서 조합 */
    private String fullDataType;

    /** 길이 */
    private int columnLength;

    /** 소수점 자리수 */
    private int decimalDigits;

    /** NULL 허용 여부 */
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
