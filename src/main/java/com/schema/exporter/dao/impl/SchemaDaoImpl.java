package com.schema.exporter.dao.impl;

import com.schema.exporter.dao.SchemaDao;
import com.schema.exporter.mapper.SchemaMapper;
import com.schema.exporter.model.ColumnInfo;
import com.schema.exporter.model.TableInfo;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SchemaDao MyBatis 구현체
 * SqlSessionTemplate 을 통해 SchemaMapper XML 쿼리 실행
 */
@Repository("schemaDao")
public class SchemaDaoImpl implements SchemaDao {

    private static final Logger log = LoggerFactory.getLogger(SchemaDaoImpl.class);

    private final SqlSessionTemplate sqlSessionTemplate;

    @Autowired
    public SchemaDaoImpl(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    /**
     * Mapper 인스턴스 반환 헬퍼
     */
    private SchemaMapper mapper() {
        return sqlSessionTemplate.getMapper(SchemaMapper.class);
    }

    @Override
    public List<TableInfo> findAllTables(String schema) {
        log.debug("전체 테이블 목록 조회 - schema: {}", schema);
        return mapper().selectTableList(schema);
    }

    @Override
    public List<TableInfo> findTablesByNames(String schema, List<String> tableNames) {
        log.debug("테이블 목록 조회 - schema: {}, tables: {}", schema, tableNames);
        return mapper().selectTableListByNames(schema, tableNames);
    }

    @Override
    public TableInfo findTable(String schema, String tableName) {
        log.debug("단일 테이블 조회 - schema: {}, table: {}", schema, tableName);
        return mapper().selectTable(schema, tableName);
    }

    @Override
    public List<ColumnInfo> findColumns(String schema, String tableName) {
        log.debug("컬럼 조회 - {}.{}", schema, tableName);
        return mapper().selectColumnList(schema, tableName);
    }

    @Override
    public List<ColumnInfo> findPrimaryKeyColumns(String schema, String tableName) {
        log.debug("PK 컬럼 조회 - {}.{}", schema, tableName);
        return mapper().selectPrimaryKeyColumns(schema, tableName);
    }

    @Override
    public List<ColumnInfo> findIndexColumns(String schema, String tableName) {
        log.debug("인덱스 컬럼 조회 - {}.{}", schema, tableName);
        return mapper().selectIndexColumns(schema, tableName);
    }
}
