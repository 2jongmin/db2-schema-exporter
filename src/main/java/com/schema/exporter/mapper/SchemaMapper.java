package com.schema.exporter.mapper;

import com.schema.exporter.model.ColumnInfo;
import com.schema.exporter.model.TableInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * DB2 SYSCAT 조회 MyBatis Mapper 인터페이스
 * SQL 정의: resources/mapper/SchemaMapper.xml
 */
public interface SchemaMapper {

    /**
     * 스키마 내 전체 테이블 목록 조회
     */
    List<TableInfo> selectTableList(@Param("schema") String schema);

    /**
     * 지정된 테이블명 목록으로 조회
     */
    List<TableInfo> selectTableListByNames(
            @Param("schema") String schema,
            @Param("tableNames") List<String> tableNames);

    /**
     * 단일 테이블 조회
     */
    TableInfo selectTable(
            @Param("schema") String schema,
            @Param("tableName") String tableName);

    /**
     * 테이블 컬럼 목록 조회
     */
    List<ColumnInfo> selectColumnList(
            @Param("schema") String schema,
            @Param("tableName") String tableName);

    /**
     * PK 컬럼 조회
     */
    List<ColumnInfo> selectPrimaryKeyColumns(
            @Param("schema") String schema,
            @Param("tableName") String tableName);

    /**
     * 인덱스 컬럼 조회
     */
    List<ColumnInfo> selectIndexColumns(
            @Param("schema") String schema,
            @Param("tableName") String tableName);
}
