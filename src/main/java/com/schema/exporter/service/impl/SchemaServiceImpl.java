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
import java.util.stream.Collectors;

/**
 * SchemaService 구현체
 * DAO → Mapper(XML) → DB2 SYSCAT 뷰 순서로 데이터 조회
 */
@Service
public class SchemaServiceImpl implements SchemaService {

    private static final Logger log = LoggerFactory.getLogger(SchemaServiceImpl.class);

    private final SchemaDao    schemaDao;
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
        String       schema  = config.getSchema().toUpperCase();
        List<String> targets = config.getTargetTables();

        List<TableInfo> tables = targets.isEmpty()
                ? schemaDao.findAllTables(schema)
                : schemaDao.findTablesByNames(schema, targets);

        if (tables.isEmpty()) {
            log.warn("추출 대상 테이블이 없습니다. 스키마: {}", schema);
            return Collections.emptyList();
        }

        log.info("테이블 {}개 기본 정보 조회 완료", tables.size());

        List<TableInfo> result = new ArrayList<TableInfo>();
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

        List<ColumnInfo> columns = schemaDao.findColumns(schema, tableName);

        // PK 컬럼 Map (컬럼명 → 순번)
        Map<String, Integer> pkMap = new LinkedHashMap<String, Integer>();
        for (ColumnInfo pk : schemaDao.findPrimaryKeyColumns(schema, tableName)) {
            pkMap.put(pk.getColumnName(), pk.getPkPosition());
        }

        // 인덱스 컬럼 Map (컬럼명 → 인덱스명)
        Map<String, String> indexMap = new HashMap<String, String>();
        for (ColumnInfo idx : schemaDao.findIndexColumns(schema, tableName)) {
            if (!indexMap.containsKey(idx.getColumnName())) {
                indexMap.put(idx.getColumnName(), idx.getIndexName());
            }
        }

        // fullDataType 조합 + PK/인덱스 플래그
        for (ColumnInfo col : columns) {
            col.setFullDataType(buildFullDataType(col));
            if (pkMap.containsKey(col.getColumnName())) {
                col.setPrimaryKey(true);
                col.setPkPosition(pkMap.get(col.getColumnName()));
            }
            if (indexMap.containsKey(col.getColumnName())) {
                col.setIndexed(true);
                col.setIndexName(indexMap.get(col.getColumnName()));
            }
        }

        TableInfo base = TableInfo.builder()
                .schemaName(schema)
                .tableName(tableName)
                .tableComment(nvl(table.getTableComment()))
                .columns(columns)
                .tablespace(nvl(table.getTablespace(), config.getTablespace()))
                .build();

        String ddl = generateDdl(base);
        base.setDdlScript(ddl);
        return base;
    }

    /**
     * 데이터 타입 + 길이/스케일 조합 (Java 8 호환 if-else)
     */
    private String buildFullDataType(ColumnInfo col) {
        String type  = col.getDataType().toUpperCase();
        int    len   = col.getColumnLength();
        int    scale = col.getDecimalDigits();

        if ("VARCHAR".equals(type) || "CHARACTER".equals(type) || "CHAR".equals(type)
                || "GRAPHIC".equals(type) || "VARGRAPHIC".equals(type)
                || "CLOB".equals(type)    || "BLOB".equals(type)
                || "DBCLOB".equals(type)) {
            return len > 0 ? type + "(" + len + ")" : type;
        } else if ("DECIMAL".equals(type) || "NUMERIC".equals(type)) {
            return scale > 0 ? type + "(" + len + "," + scale + ")" : type + "(" + len + ")";
        } else {
            return type;
        }
    }

    /**
     * DDL 스크립트 생성
     */
    @Override
    public String generateDdl(TableInfo table) {
        StringBuilder sb       = new StringBuilder();
        String        schema   = table.getSchemaName().toUpperCase();
        String        tblName  = table.getTableName().toUpperCase();
        String        fullName = schema + "." + tblName;
        String        ts       = nvl(table.getTablespace(), config.getTablespace());
        List<ColumnInfo> cols  = table.getColumns();

        sb.append("-- ============================================================\n");
        if (!isEmpty(table.getTableComment())) {
            sb.append("-- ").append(table.getTableComment()).append("\n");
        }
        sb.append("-- TABLE : ").append(fullName).append("\n");
        sb.append("-- ============================================================\n");
        sb.append("CREATE TABLE ").append(fullName).append(" (\n");

        // PK 컬럼 순서 정렬
        List<ColumnInfo> pkList = new ArrayList<ColumnInfo>();
        for (ColumnInfo c : cols) {
            if (c.isPrimaryKey()) pkList.add(c);
        }
        Collections.sort(pkList, new Comparator<ColumnInfo>() {
            @Override
            public int compare(ColumnInfo a, ColumnInfo b) {
                return Integer.compare(a.getPkPosition(), b.getPkPosition());
            }
        });
        List<String> pkCols = new ArrayList<String>();
        for (ColumnInfo c : pkList) {
            pkCols.add(c.getColumnName());
        }

        boolean hasPk = config.isIncludePrimaryKey() && !pkCols.isEmpty();

        for (int i = 0; i < cols.size(); i++) {
            ColumnInfo col    = cols.get(i);
            boolean    isLast = (i == cols.size() - 1) && !hasPk;

            sb.append("    ")
              .append(String.format("%-30s", col.getColumnName()))
              .append(String.format("%-25s", col.getFullDataType()));

            if (!isEmpty(col.getDefaultValue())) {
                sb.append("DEFAULT ").append(col.getDefaultValue()).append(" ");
            }
            sb.append(col.isNullable() ? "NULL" : "NOT NULL");
            if (!isLast) sb.append(",");

            if (config.isIncludeComment() && !isEmpty(col.getColumnComment())) {
                sb.append("  -- ").append(col.getColumnComment());
            }
            sb.append("\n");
        }

        if (hasPk) {
            sb.append("    CONSTRAINT PK_").append(tblName)
              .append(" PRIMARY KEY (")
              .append(join(pkCols, ", "))
              .append(")\n");
        }

        sb.append(")");
        if (!isEmpty(ts)) sb.append("\nIN ").append(ts);
        sb.append(";\n");

        // COMMENT 절
        if (config.isIncludeComment()) {
            if (!isEmpty(table.getTableComment())) {
                sb.append("\nCOMMENT ON TABLE ").append(fullName)
                  .append(" IS '").append(escape(table.getTableComment())).append("';\n");
            }
            for (ColumnInfo c : cols) {
                if (!isEmpty(c.getColumnComment())) {
                    sb.append("COMMENT ON COLUMN ").append(fullName).append(".")
                      .append(c.getColumnName())
                      .append(" IS '").append(escape(c.getColumnComment())).append("';\n");
                }
            }
        }
        return sb.toString();
    }

    // ── 유틸 (Java 8 호환) ──
    private boolean isEmpty(String s)              { return s == null || s.trim().isEmpty(); }
    private String  nvl(String val)                { return val != null ? val : ""; }
    private String  nvl(String val, String def)    { return (val != null && !val.trim().isEmpty()) ? val : def; }
    private String  escape(String s)               { return s.replace("'", "''"); }
    private String  join(List<String> list, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(sep);
            sb.append(list.get(i));
        }
        return sb.toString();
    }
}
