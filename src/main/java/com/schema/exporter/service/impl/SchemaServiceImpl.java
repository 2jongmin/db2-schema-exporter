package com.schema.exporter.service.impl;

import com.schema.exporter.dao.SchemaDao;
import com.schema.exporter.model.ColumnInfo;
import com.schema.exporter.model.ExportConfig;
import com.schema.exporter.model.TableInfo;
import com.schema.exporter.service.SchemaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * SchemaService 구현체
 * DAO → Mapper(XML) → DB2 SYSCAT 뷰 순서로 데이터 조회
 */
@Service
public class SchemaServiceImpl implements SchemaService {

    private static final Logger log = LoggerFactory.getLogger(SchemaServiceImpl.class);

    private final SchemaDao  schemaDao;
    private final ExportConfig config;

    @Autowired
    public SchemaServiceImpl(SchemaDao schemaDao, ExportConfig config) {
        this.schemaDao = schemaDao;
        this.config    = config;
    }

    /**
     * 대상 테이블 추출 (전체 or 지정)
     */
    @Override
    @Transactional(readOnly = true)
    public List<TableInfo> extractTables() {
        String schema = config.getSchema().toUpperCase();
        List<String> targets = config.getTargetTables();

        // 1. 테이블 기본 정보 목록 조회
        List<TableInfo> tables = targets.isEmpty()
                ? schemaDao.findAllTables(schema)
                : schemaDao.findTablesByNames(schema, targets);

        if (tables.isEmpty()) {
            log.warn("추출 대상 테이블이 없습니다. 스키마: {}", schema);
            return Collections.emptyList();
        }

        log.info("테이블 {}개 기본 정보 조회 완료", tables.size());

        // 2. 각 테이블 컬럼/PK/인덱스 상세 정보 조회
        List<TableInfo> result = new ArrayList<>();
        for (TableInfo table : tables) {
            try {
                TableInfo enriched = enrichTableInfo(schema, table);
                result.add(enriched);
                log.info("  └─ {} ({}) - 컬럼 {}개",
                        table.getTableName(),
                        table.getTableComment(),
                        enriched.getColumns().size());
            } catch (Exception e) {
                log.error("테이블 상세 조회 실패: {} - {}", table.getTableName(), e.getMessage());
            }
        }
        return result;
    }

    /**
     * 테이블에 컬럼/PK/인덱스 정보 병합
     */
    private TableInfo enrichTableInfo(String schema, TableInfo table) {
        String tableName = table.getTableName();

        // 컬럼 목록
        List<ColumnInfo> columns = schemaDao.findColumns(schema, tableName);

        // PK 컬럼 Set
        Map<String, Integer> pkMap = new LinkedHashMap<>();
        schemaDao.findPrimaryKeyColumns(schema, tableName)
                .forEach(pk -> pkMap.put(pk.getColumnName(), pk.getPkPosition()));

        // 인덱스 컬럼 Map (컬럼명 → 인덱스명)
        Map<String, String> indexMap = new HashMap<>();
        schemaDao.findIndexColumns(schema, tableName)
                .forEach(idx -> indexMap.putIfAbsent(idx.getColumnName(), idx.getIndexName()));

        // 컬럼별 fullDataType 조합 + PK/인덱스 플래그 적용
        columns.forEach(col -> {
            col.setFullDataType(buildFullDataType(col));
            if (pkMap.containsKey(col.getColumnName())) {
                col.setPrimaryKey(true);
                col.setPkPosition(pkMap.get(col.getColumnName()));
            }
            if (indexMap.containsKey(col.getColumnName())) {
                col.setIndexed(true);
                col.setIndexName(indexMap.get(col.getColumnName()));
            }
        });

        // DDL 생성
        String ddl = generateDdl(TableInfo.builder()
                .schemaName(schema)
                .tableName(tableName)
                .tableComment(table.getTableComment())
                .columns(columns)
                .tablespace(table.getTablespace())
                .build());

        return TableInfo.builder()
                .schemaName(schema)
                .tableName(tableName)
                .tableComment(nvl(table.getTableComment()))
                .columns(columns)
                .ddlScript(ddl)
                .tablespace(nvl(table.getTablespace(), config.getTablespace()))
                .build();
    }

    /**
     * 데이터 타입 + 길이/스케일 조합
     */
    private String buildFullDataType(ColumnInfo col) {
        String type  = col.getDataType();
        int    len   = col.getColumnLength();
        int    scale = col.getDecimalDigits();

        return switch (type.toUpperCase()) {
            case "VARCHAR", "CHARACTER", "CHAR",
                 "GRAPHIC", "VARGRAPHIC", "CLOB", "BLOB", "DBCLOB" ->
                    len > 0 ? type + "(" + len + ")" : type;
            case "DECIMAL", "NUMERIC" ->
                    scale > 0
                            ? type + "(" + len + "," + scale + ")"
                            : type + "(" + len + ")";
            default -> type;
        };
    }

    /**
     * DDL 스크립트 생성
     */
    @Override
    public String generateDdl(TableInfo table) {
        StringBuilder sb  = new StringBuilder();
        String schema     = table.getSchemaName().toUpperCase();
        String tableName  = table.getTableName().toUpperCase();
        String fullName   = schema + "." + tableName;
        String ts         = nvl(table.getTablespace(), config.getTablespace());
        List<ColumnInfo> cols = table.getColumns();

        sb.append("-- ============================================================\n");
        if (!table.getTableComment().isBlank()) {
            sb.append("-- ").append(table.getTableComment()).append("\n");
        }
        sb.append("-- TABLE : ").append(fullName).append("\n");
        sb.append("-- ============================================================\n");
        sb.append("CREATE TABLE ").append(fullName).append(" (\n");

        List<String> pkCols = cols.stream()
                .filter(ColumnInfo::isPrimaryKey)
                .sorted(Comparator.comparingInt(ColumnInfo::getPkPosition))
                .map(ColumnInfo::getColumnName)
                .toList();

        boolean hasPk = config.isIncludePrimaryKey() && !pkCols.isEmpty();

        for (int i = 0; i < cols.size(); i++) {
            ColumnInfo col   = cols.get(i);
            boolean    isLast = (i == cols.size() - 1) && !hasPk;

            sb.append("    ")
              .append(String.format("%-30s", col.getColumnName()))
              .append(String.format("%-25s", col.getFullDataType()));

            if (col.getDefaultValue() != null && !col.getDefaultValue().isBlank()) {
                sb.append("DEFAULT ").append(col.getDefaultValue()).append(" ");
            }
            sb.append(col.isNullable() ? "NULL" : "NOT NULL");
            if (!isLast) sb.append(",");

            if (config.isIncludeComment() && !col.getColumnComment().isBlank()) {
                sb.append("  -- ").append(col.getColumnComment());
            }
            sb.append("\n");
        }

        if (hasPk) {
            sb.append("    CONSTRAINT PK_").append(tableName)
              .append(" PRIMARY KEY (").append(String.join(", ", pkCols)).append(")\n");
        }

        sb.append(")");
        if (!ts.isBlank()) sb.append("\nIN ").append(ts);
        sb.append(";\n");

        // COMMENT 절
        if (config.isIncludeComment()) {
            if (!table.getTableComment().isBlank()) {
                sb.append("\nCOMMENT ON TABLE ").append(fullName)
                  .append(" IS '").append(escape(table.getTableComment())).append("';\n");
            }
            cols.stream()
                .filter(c -> !c.getColumnComment().isBlank())
                .forEach(c ->
                    sb.append("COMMENT ON COLUMN ").append(fullName).append(".")
                      .append(c.getColumnName())
                      .append(" IS '").append(escape(c.getColumnComment())).append("';\n")
                );
        }
        return sb.toString();
    }

    // ── 유틸 ──
    private String nvl(String val) { return val != null ? val : ""; }
    private String nvl(String val, String def) { return (val != null && !val.isBlank()) ? val : def; }
    private String escape(String s) { return s.replace("'", "''"); }
}
