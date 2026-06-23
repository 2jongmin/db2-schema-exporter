package com.schema.exporter.dao;

import com.schema.exporter.model.ColumnInfo;
import com.schema.exporter.model.TableInfo;

import java.util.List;

/**
 * DB2 스키마 조회 DAO 인터페이스
 * Service 레이어는 이 인터페이스에만 의존 (DIP 원칙)
 */
public interface SchemaDao {

    /**
     * 전체 테이블 목록 조회
     */
    List<TableInfo> findAllTables(String schema);

    /**
     * 지정 테이블명 목록 조회
     */
    List<TableInfo> findTablesByNames(String schema, List<String> tableNames);

    /**
     * 단일 테이블 조회
     */
    TableInfo findTable(String schema, String tableName);

    /**
     * 테이블 컬럼 목록 조회
     */
    List<ColumnInfo> findColumns(String schema, String tableName);

    /**
     * PK 컬럼 조회
     */
    List<ColumnInfo> findPrimaryKeyColumns(String schema, String tableName);

    /**
     * 인덱스 컬럼 조회
     */
    List<ColumnInfo> findIndexColumns(String schema, String tableName);
}
