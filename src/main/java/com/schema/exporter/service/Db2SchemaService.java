package com.schema.exporter.service;

import com.schema.exporter.model.ColumnInfo;
import com.schema.exporter.model.ExportConfig;
import com.schema.exporter.model.TableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * DB2 스키마 정보 조회 서비스
 * SYSCAT 뷰를 이용하여 테이블/컬럼/인덱스 정보를 조회합니다.
 */
public class Db2SchemaService {

    private static final Logger log = LoggerFactory.getLogger(Db2SchemaService.class);

    private final ExportConfig config;

    public Db2SchemaService(ExportConfig config) {
        this.config = config;
    }

    /**
     * DB2 연결 생성
     */
    public Connection getConnection() throws SQLException {
        try {
            Class.forName("com.ibm.db2.jcc.DB2Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("DB2 JDBC Driver not found. pom.xml에 db2 jcc 의존성을 추가하세요.", e);
        }
        return DriverManager.getConnection(config.getDbUrl(), config.getDbUsername(), config.getDbPassword());
    }

    /**
     * 전체 테이블 정보 추출
     */
    public List<TableInfo> extractAllTables(Connection conn) throws SQLException {
        List<String> tableNames = resolveTargetTables(conn);
        List<TableInfo> result = new ArrayList<>();

        for (String tableName : tableNames) {
            try {
                TableInfo tableInfo = extractTableInfo(conn, tableName);
                result.add(tableInfo);
                log.info("테이블 추출 완료: {}", tableName);
            } catch (Exception e) {
                log.error("테이블 추출 실패: {} - {}", tableName, e.getMessage());
            }
        }
        return result;
    }

    /**
     * 대상 테이블 목록 결정
     */
    private List<String> resolveTargetTables(Connection conn) throws SQLException {
        if (config.getTargetTables() != null && !config.getTargetTables().isEmpty()) {
            return config.getTargetTables();
        }
        return getAllTableNames(conn);
    }

    /**
     * 스키마 내 전체 테이블 목록 조회
     */
    public List<String> getAllTableNames(Connection conn) throws SQLException {
        List<String> tables = new ArrayList<>();
        String sql = """
                SELECT TABNAME
                FROM SYSCAT.TABLES
                WHERE TABSCHEMA = ?
                  AND TYPE = 'T'
                ORDER BY TABNAME
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, config.getSchema().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString("TABNAME"));
                }
            }
        }
        return tables;
    }

    /**
     * 단일 테이블 정보 추출
     */
    public TableInfo extractTableInfo(Connection conn, String tableName) throws SQLException {
        String tableComment = getTableComment(conn, tableName);
        List<ColumnInfo> columns = getColumnInfoList(conn, tableName);
        Set<String> pkColumns = getPrimaryKeyColumns(conn, tableName);
        Map<String, String> indexMap = getIndexMap(conn, tableName);

        // PK, 인덱스 정보 병합
        for (ColumnInfo col : columns) {
            if (pkColumns.contains(col.getColumnName())) {
                col.setPrimaryKey(true);
            }
            if (indexMap.containsKey(col.getColumnName())) {
                col.setIndexed(true);
                col.setIndexName(indexMap.get(col.getColumnName()));
            }
        }

        String ddl = generateDdl(tableName, tableComment, columns);

        return TableInfo.builder()
                .schemaName(config.getSchema())
                .tableName(tableName)
                .tableComment(tableComment)
                .columns(columns)
                .ddlScript(ddl)
                .tablespace(config.getTablespace())
                .build();
    }

    /**
     * 테이블 COMMENT 조회
     */
    private String getTableComment(Connection conn, String tableName) throws SQLException {
        String sql = """
                SELECT REMARKS
                FROM SYSCAT.TABLES
                WHERE TABSCHEMA = ?
                  AND TABNAME = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, config.getSchema().toUpperCase());
            ps.setString(2, tableName.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("REMARKS") != null ? rs.getString("REMARKS") : "";
                }
            }
        }
        return "";
    }

    /**
     * 컬럼 정보 목록 조회
     */
    private List<ColumnInfo> getColumnInfoList(Connection conn, String tableName) throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();
        String sql = """
                SELECT
                    C.COLNO           AS ORDINAL_POSITION,
                    C.COLNAME         AS COLUMN_NAME,
                    C.REMARKS         AS COLUMN_COMMENT,
                    C.TYPENAME        AS DATA_TYPE,
                    C.LENGTH          AS COLUMN_LENGTH,
                    C.SCALE           AS DECIMAL_DIGITS,
                    C.NULLS           AS IS_NULLABLE,
                    C.DEFAULT         AS COLUMN_DEFAULT
                FROM SYSCAT.COLUMNS C
                WHERE C.TABSCHEMA = ?
                  AND C.TABNAME   = ?
                ORDER BY C.COLNO
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, config.getSchema().toUpperCase());
            ps.setString(2, tableName.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String dataType   = rs.getString("DATA_TYPE");
                    int    length     = rs.getInt("COLUMN_LENGTH");
                    int    scale      = rs.getInt("DECIMAL_DIGITS");
                    String fullType   = buildFullDataType(dataType, length, scale);

                    ColumnInfo col = ColumnInfo.builder()
                            .ordinalPosition(rs.getInt("ORDINAL_POSITION") + 1)
                            .columnName(rs.getString("COLUMN_NAME"))
                            .columnComment(rs.getString("COLUMN_COMMENT") != null ? rs.getString("COLUMN_COMMENT") : "")
                            .dataType(dataType)
                            .fullDataType(fullType)
                            .columnLength(length)
                            .decimalDigits(scale)
                            .nullable("Y".equals(rs.getString("IS_NULLABLE")))
                            .defaultValue(rs.getString("COLUMN_DEFAULT") != null ? rs.getString("COLUMN_DEFAULT") : "")
                            .build();
                    columns.add(col);
                }
            }
        }
        return columns;
    }

    /**
     * 데이터 타입 + 길이 조합 문자열 생성
     */
    private String buildFullDataType(String dataType, int length, int scale) {
        return switch (dataType.toUpperCase()) {
            case "VARCHAR", "CHARACTER", "CHAR", "GRAPHIC", "VARGRAPHIC", "CLOB", "BLOB" ->
                    length > 0 ? dataType + "(" + length + ")" : dataType;
            case "DECIMAL", "NUMERIC" ->
                    scale > 0 ? dataType + "(" + length + "," + scale + ")" : dataType + "(" + length + ")";
            default -> dataType;
        };
    }

    /**
     * PK 컬럼 집합 조회
     */
    private Set<String> getPrimaryKeyColumns(Connection conn, String tableName) throws SQLException {
        Set<String> pkCols = new LinkedHashSet<>();
        String sql = """
                SELECT KC.COLNAME
                FROM SYSCAT.KEYCOLUSE KC
                JOIN SYSCAT.TABCONST TC
                  ON KC.CONSTNAME = TC.CONSTNAME
                 AND KC.TABSCHEMA = TC.TABSCHEMA
                 AND KC.TABNAME   = TC.TABNAME
                WHERE TC.TABSCHEMA = ?
                  AND TC.TABNAME   = ?
                  AND TC.TYPE      = 'P'
                ORDER BY KC.COLSEQ
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, config.getSchema().toUpperCase());
            ps.setString(2, tableName.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pkCols.add(rs.getString("COLNAME"));
                }
            }
        }
        return pkCols;
    }

    /**
     * 인덱스 컬럼 맵 조회 (컬럼명 → 인덱스명)
     */
    private Map<String, String> getIndexMap(Connection conn, String tableName) throws SQLException {
        Map<String, String> indexMap = new HashMap<>();
        String sql = """
                SELECT IC.COLNAME, I.INDNAME
                FROM SYSCAT.INDEXCOLUSE IC
                JOIN SYSCAT.INDEXES I
                  ON IC.INDSCHEMA = I.INDSCHEMA
                 AND IC.INDNAME   = I.INDNAME
                WHERE I.TABSCHEMA = ?
                  AND I.TABNAME   = ?
                  AND I.UNIQUERULE <> 'P'
                ORDER BY I.INDNAME, IC.COLSEQ
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, config.getSchema().toUpperCase());
            ps.setString(2, tableName.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    indexMap.putIfAbsent(rs.getString("COLNAME"), rs.getString("INDNAME"));
                }
            }
        }
        return indexMap;
    }

    /**
     * DDL 스크립트 생성
     */
    public String generateDdl(String tableName, String tableComment, List<ColumnInfo> columns) {
        StringBuilder sb = new StringBuilder();
        String schema = config.getSchema().toUpperCase();
        String fullTableName = schema + "." + tableName.toUpperCase();

        sb.append("-- ============================================================\n");
        if (!tableComment.isEmpty()) {
            sb.append("-- ").append(tableComment).append("\n");
        }
        sb.append("-- TABLE: ").append(fullTableName).append("\n");
        sb.append("-- ============================================================\n");
        sb.append("CREATE TABLE ").append(fullTableName).append(" (\n");

        List<String> pkCols = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            ColumnInfo col = columns.get(i);
            sb.append("    ").append(String.format("%-30s", col.getColumnName()))
              .append(String.format("%-25s", col.getFullDataType()));

            if (!col.getDefaultValue().isEmpty()) {
                sb.append("DEFAULT ").append(col.getDefaultValue()).append(" ");
            }

            sb.append(col.isNullable() ? "NULL" : "NOT NULL");

            if (i < columns.size() - 1 || config.isIncludePrimaryKey()) {
                sb.append(",");
            }
            if (!col.getColumnComment().isEmpty() && config.isIncludeComment()) {
                sb.append("  -- ").append(col.getColumnComment());
            }
            sb.append("\n");

            if (col.isPrimaryKey()) {
                pkCols.add(col.getColumnName());
            }
        }

        if (config.isIncludePrimaryKey() && !pkCols.isEmpty()) {
            sb.append("    CONSTRAINT PK_").append(tableName.toUpperCase())
              .append(" PRIMARY KEY (").append(String.join(", ", pkCols)).append(")\n");
        }

        sb.append(")");
        if (config.getTablespace() != null && !config.getTablespace().isEmpty()) {
            sb.append("\nIN ").append(config.getTablespace());
        }
        sb.append(";\n");

        if (config.isIncludeComment()) {
            if (!tableComment.isEmpty()) {
                sb.append("\nCOMMENT ON TABLE ").append(fullTableName)
                  .append(" IS '").append(tableComment.replace("'", "''")).append("';\n");
            }
            for (ColumnInfo col : columns) {
                if (!col.getColumnComment().isEmpty()) {
                    sb.append("COMMENT ON COLUMN ").append(fullTableName).append(".")
                      .append(col.getColumnName()).append(" IS '")
                      .append(col.getColumnComment().replace("'", "''")).append("';\n");
                }
            }
        }

        return sb.toString();
    }
}
