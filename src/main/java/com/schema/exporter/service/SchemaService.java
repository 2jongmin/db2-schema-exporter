package com.schema.exporter.service;

import com.schema.exporter.model.TableInfo;

import java.util.List;

/**
 * 스키마 추출 서비스 인터페이스
 */
public interface SchemaService {

    /**
     * 설정 기반으로 대상 테이블 전체 추출
     * (export.tables 설정에 따라 전체 또는 지정 테이블)
     */
    List<TableInfo> extractTables();

    /**
     * DDL 스크립트 생성
     */
    String generateDdl(TableInfo tableInfo);
}
