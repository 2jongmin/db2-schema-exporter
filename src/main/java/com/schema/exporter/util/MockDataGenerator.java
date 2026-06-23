package com.schema.exporter.util;

import com.schema.exporter.model.ColumnInfo;
import com.schema.exporter.model.TableInfo;
import com.schema.exporter.service.Db2SchemaService;
import com.schema.exporter.model.ExportConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * DB 접속 없이 테스트용 샘플 데이터를 생성하는 유틸리티
 * DB2 JDBC Driver 가 없는 환경에서 엑셀 출력을 확인할 때 사용
 */
public class MockDataGenerator {

    public static List<TableInfo> generateSampleTables(ExportConfig config) {
        Db2SchemaService svc = new Db2SchemaService(config);
        List<TableInfo> tables = new ArrayList<>();

        tables.add(buildUserTable(svc));
        tables.add(buildBoardTable(svc));
        tables.add(buildCommentTable(svc));
        tables.add(buildStockOrderTable(svc));
        tables.add(buildCoinOrderTable(svc));

        return tables;
    }

    private static TableInfo buildUserTable(Db2SchemaService svc) {
        List<ColumnInfo> cols = new ArrayList<>();
        cols.add(col(1,  "USER_ID",       "사용자ID",      "BIGINT",       "BIGINT",          0,  0, false, true,  ""));
        cols.add(col(2,  "EMAIL",          "이메일",         "VARCHAR",      "VARCHAR(150)",  150,  0, false, false, ""));
        cols.add(col(3,  "PASSWORD",       "비밀번호(암호화)","VARCHAR",     "VARCHAR(255)",  255,  0, true,  false, ""));
        cols.add(col(4,  "NICKNAME",       "닉네임",         "VARCHAR",      "VARCHAR(50)",    50,  0, false, false, ""));
        cols.add(col(5,  "OAUTH_PROVIDER", "OAuth제공자",    "VARCHAR",      "VARCHAR(20)",    20,  0, true,  false, "GOOGLE/NAVER/KAKAO"));
        cols.add(col(6,  "OAUTH_ID",       "OAuth식별자",    "VARCHAR",      "VARCHAR(100)",  100,  0, true,  false, ""));
        cols.add(col(7,  "ROLE",           "권한",           "VARCHAR",      "VARCHAR(20)",    20,  0, false, false, "USER"));
        cols.add(col(8,  "STATUS",         "상태코드",        "CHAR",         "CHAR(1)",         1,  0, false, false, "A"));
        cols.add(col(9,  "REFRESH_TOKEN",  "리프레시토큰",    "VARCHAR",      "VARCHAR(512)",  512,  0, true,  false, ""));
        cols.add(col(10, "CREATED_AT",     "생성일시",        "TIMESTAMP",    "TIMESTAMP",       0,  0, false, false, "CURRENT TIMESTAMP"));
        cols.add(col(11, "UPDATED_AT",     "수정일시",        "TIMESTAMP",    "TIMESTAMP",       0,  0, true,  false, ""));

        String ddl = svc.generateDdl("TB_USER", "회원 정보", cols);
        return TableInfo.builder()
                .schemaName("MYSCHEMA").tableName("TB_USER").tableComment("회원 정보")
                .columns(cols).ddlScript(ddl).tablespace("USERSPACE1").build();
    }

    private static TableInfo buildBoardTable(Db2SchemaService svc) {
        List<ColumnInfo> cols = new ArrayList<>();
        cols.add(col(1,  "BOARD_ID",    "게시글ID",   "BIGINT",   "BIGINT",        0, 0, false, true,  ""));
        cols.add(col(2,  "CATEGORY",    "카테고리",    "VARCHAR",  "VARCHAR(20)",  20, 0, false, false, ""));
        cols.add(col(3,  "TITLE",       "제목",        "VARCHAR",  "VARCHAR(200)", 200, 0, false, false, ""));
        cols.add(col(4,  "CONTENT",     "본문",        "CLOB",     "CLOB(1M)",      0, 0, true,  false, ""));
        cols.add(col(5,  "USER_ID",     "작성자ID",    "BIGINT",   "BIGINT",        0, 0, false, false, ""));
        cols.add(col(6,  "VIEW_COUNT",  "조회수",      "INTEGER",  "INTEGER",       0, 0, false, false, "0"));
        cols.add(col(7,  "LIKE_COUNT",  "좋아요수",    "INTEGER",  "INTEGER",       0, 0, false, false, "0"));
        cols.add(col(8,  "STATUS",      "상태",        "CHAR",     "CHAR(1)",       1, 0, false, false, "Y"));
        cols.add(col(9,  "CREATED_AT",  "등록일시",    "TIMESTAMP","TIMESTAMP",     0, 0, false, false, "CURRENT TIMESTAMP"));
        cols.add(col(10, "UPDATED_AT",  "수정일시",    "TIMESTAMP","TIMESTAMP",     0, 0, true,  false, ""));

        String ddl = svc.generateDdl("TB_BOARD", "게시판 게시글", cols);
        return TableInfo.builder()
                .schemaName("MYSCHEMA").tableName("TB_BOARD").tableComment("게시판 게시글")
                .columns(cols).ddlScript(ddl).tablespace("USERSPACE1").build();
    }

    private static TableInfo buildCommentTable(Db2SchemaService svc) {
        List<ColumnInfo> cols = new ArrayList<>();
        cols.add(col(1, "COMMENT_ID",  "댓글ID",   "BIGINT",  "BIGINT",       0, 0, false, true,  ""));
        cols.add(col(2, "BOARD_ID",    "게시글ID",  "BIGINT",  "BIGINT",       0, 0, false, false, ""));
        cols.add(col(3, "PARENT_ID",   "부모댓글ID","BIGINT",  "BIGINT",       0, 0, true,  false, ""));
        cols.add(col(4, "USER_ID",     "작성자ID",  "BIGINT",  "BIGINT",       0, 0, false, false, ""));
        cols.add(col(5, "CONTENT",     "내용",      "VARCHAR", "VARCHAR(2000)",2000,0,false, false, ""));
        cols.add(col(6, "STATUS",      "상태",      "CHAR",    "CHAR(1)",      1, 0, false, false, "Y"));
        cols.add(col(7, "CREATED_AT",  "등록일시",  "TIMESTAMP","TIMESTAMP",   0, 0, false, false, "CURRENT TIMESTAMP"));

        String ddl = svc.generateDdl("TB_COMMENT", "댓글", cols);
        return TableInfo.builder()
                .schemaName("MYSCHEMA").tableName("TB_COMMENT").tableComment("댓글")
                .columns(cols).ddlScript(ddl).tablespace("USERSPACE1").build();
    }

    private static TableInfo buildStockOrderTable(Db2SchemaService svc) {
        List<ColumnInfo> cols = new ArrayList<>();
        cols.add(col(1,  "ORDER_ID",     "주문ID",       "BIGINT",   "BIGINT",       0, 0, false, true,  ""));
        cols.add(col(2,  "USER_ID",      "사용자ID",      "BIGINT",   "BIGINT",       0, 0, false, false, ""));
        cols.add(col(3,  "STOCK_CODE",   "종목코드",      "VARCHAR",  "VARCHAR(20)",  20, 0, false, false, ""));
        cols.add(col(4,  "STOCK_NAME",   "종목명",        "VARCHAR",  "VARCHAR(100)", 100,0, false, false, ""));
        cols.add(col(5,  "ORDER_TYPE",   "주문유형",      "CHAR",     "CHAR(1)",       1, 0, false, false, ""));
        cols.add(col(6,  "ORDER_PRICE",  "주문가",        "DECIMAL",  "DECIMAL(18,4)",18, 4, false, false, ""));
        cols.add(col(7,  "ORDER_QTY",    "주문수량",      "INTEGER",  "INTEGER",       0, 0, false, false, ""));
        cols.add(col(8,  "EXEC_PRICE",   "체결가",        "DECIMAL",  "DECIMAL(18,4)",18, 4, true,  false, ""));
        cols.add(col(9,  "EXEC_QTY",     "체결수량",      "INTEGER",  "INTEGER",       0, 0, false, false, "0"));
        cols.add(col(10, "STATUS",       "주문상태",      "VARCHAR",  "VARCHAR(20)",  20, 0, false, false, "PENDING"));
        cols.add(col(11, "STRATEGY_NM",  "자동매매전략명", "VARCHAR",  "VARCHAR(100)", 100,0, true,  false, ""));
        cols.add(col(12, "CREATED_AT",   "주문일시",      "TIMESTAMP","TIMESTAMP",     0, 0, false, false, "CURRENT TIMESTAMP"));

        String ddl = svc.generateDdl("TB_STOCK_ORDER", "주식 자동매매 주문", cols);
        return TableInfo.builder()
                .schemaName("MYSCHEMA").tableName("TB_STOCK_ORDER").tableComment("주식 자동매매 주문")
                .columns(cols).ddlScript(ddl).tablespace("USERSPACE1").build();
    }

    private static TableInfo buildCoinOrderTable(Db2SchemaService svc) {
        List<ColumnInfo> cols = new ArrayList<>();
        cols.add(col(1,  "ORDER_ID",     "주문ID",        "BIGINT",  "BIGINT",        0, 0, false, true,  ""));
        cols.add(col(2,  "USER_ID",      "사용자ID",       "BIGINT",  "BIGINT",        0, 0, false, false, ""));
        cols.add(col(3,  "MARKET",       "마켓코드",       "VARCHAR", "VARCHAR(20)",   20, 0, false, false, ""));
        cols.add(col(4,  "COIN_SYMBOL",  "코인심볼",       "VARCHAR", "VARCHAR(20)",   20, 0, false, false, ""));
        cols.add(col(5,  "SIDE",         "매수/매도",      "VARCHAR", "VARCHAR(10)",   10, 0, false, false, ""));
        cols.add(col(6,  "ORDER_TYPE",   "주문유형",       "VARCHAR", "VARCHAR(20)",   20, 0, false, false, "LIMIT"));
        cols.add(col(7,  "PRICE",        "주문가격",       "DECIMAL", "DECIMAL(30,8)", 30, 8, true,  false, ""));
        cols.add(col(8,  "VOLUME",       "주문수량",       "DECIMAL", "DECIMAL(30,8)", 30, 8, false, false, ""));
        cols.add(col(9,  "EXEC_VOLUME",  "체결수량",       "DECIMAL", "DECIMAL(30,8)", 30, 8, false, false, "0"));
        cols.add(col(10, "EXCHANGE",     "거래소",         "VARCHAR", "VARCHAR(20)",   20, 0, false, false, "UPBIT"));
        cols.add(col(11, "STATUS",       "주문상태",       "VARCHAR", "VARCHAR(20)",   20, 0, false, false, "PENDING"));
        cols.add(col(12, "STRATEGY_NM",  "자동매매전략명",  "VARCHAR", "VARCHAR(100)", 100, 0, true,  false, ""));
        cols.add(col(13, "CREATED_AT",   "주문일시",       "TIMESTAMP","TIMESTAMP",     0, 0, false, false, "CURRENT TIMESTAMP"));

        String ddl = svc.generateDdl("TB_COIN_ORDER", "코인 자동매매 주문", cols);
        return TableInfo.builder()
                .schemaName("MYSCHEMA").tableName("TB_COIN_ORDER").tableComment("코인 자동매매 주문")
                .columns(cols).ddlScript(ddl).tablespace("USERSPACE1").build();
    }

    private static ColumnInfo col(int pos, String name, String comment, String type, String fullType,
                                   int len, int scale, boolean nullable, boolean pk, String defaultVal) {
        ColumnInfo c = ColumnInfo.builder()
                .ordinalPosition(pos).columnName(name).columnComment(comment)
                .dataType(type).fullDataType(fullType).columnLength(len).decimalDigits(scale)
                .nullable(nullable).primaryKey(pk).defaultValue(defaultVal).build();
        return c;
    }
}
