package com.schema.exporter.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 내보내기 설정 모델
 */
@Data
@Builder
public class ExportConfig {

    /** DB 접속 URL */
    private String dbUrl;

    /** DB 사용자명 */
    private String dbUsername;

    /** DB 비밀번호 */
    private String dbPassword;

    /** 스키마명 */
    private String schema;

    /** 추출 대상 테이블 목록 (비어 있으면 전체) */
    private List<String> targetTables;

    /** 엑셀 출력 경로 */
    private String outputPath;

    /** 엑셀 파일명 (확장자 제외) */
    private String outputFilename;

    /** 날짜 포함 여부 */
    private boolean includeDate;

    /** PK 제약조건 포함 여부 */
    private boolean includePrimaryKey;

    /** 인덱스 포함 여부 */
    private boolean includeIndex;

    /** COMMENT 포함 여부 */
    private boolean includeComment;

    /** 기본 테이블스페이스 */
    private String tablespace;
}
