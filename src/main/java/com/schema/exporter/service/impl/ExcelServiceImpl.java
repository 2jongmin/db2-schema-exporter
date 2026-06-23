package com.schema.exporter.service.impl;

import com.schema.exporter.model.ColumnInfo;
import com.schema.exporter.model.ExportConfig;
import com.schema.exporter.model.TableInfo;
import com.schema.exporter.service.ExcelService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Apache POI 기반 엑셀 생성 서비스 구현체
 * - INDEX 시트 (목차 + 하이퍼링크)
 * - 테이블별 스키마 시트 (PK=노랑, 인덱스=연녹색)
 * - DDL_ALL 시트 (전체 DDL 스크립트)
 */
@Service
public class ExcelServiceImpl implements ExcelService {

    private static final Logger log = LoggerFactory.getLogger(ExcelServiceImpl.class);

    // ── 색상 팔레트 ──
    private static final byte[] C_HDR    = rgb("1F4E79");
    private static final byte[] C_SEC    = rgb("D6E4F0");
    private static final byte[] C_PK     = rgb("FFF2CC");
    private static final byte[] C_IDX    = rgb("E2EFDA");
    private static final byte[] C_ODD    = rgb("F5F5F5");
    private static final byte[] C_WHITE  = rgb("FFFFFF");
    private static final byte[] C_DARK   = rgb("2C3E50");
    private static final byte[] C_LBL    = rgb("EBF5FB");
    private static final byte[] C_TOTAL  = rgb("D5DBDB");
    private static final byte[] C_DATE_B = rgb("2E4053");
    private static final byte[] C_DATE_F = rgb("BFC9CA");
    private static final byte[] C_BLUE   = rgb("1A5276");

    private static final String[] COL_HDRS = {
            "No","컬럼명","한글명","데이터타입","길이","소수점","NULL","PK","기본값","인덱스명","비고"
    };
    private static final int[] COL_WIDTHS = {
            5, 25, 20, 18, 8, 8, 10, 6, 15, 20, 20
    };

    private final ExportConfig config;

    @Autowired
    public ExcelServiceImpl(ExportConfig config) {
        this.config = config;
    }

    @Override
    public String export(List<TableInfo> tables) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            buildIndexSheet(wb, tables);
            tables.forEach(t -> buildTableSheet(wb, t));
            buildDdlSheet(wb, tables);

            String path = saveFile(wb);
            log.info("엑셀 생성 완료: {}", path);
            return path;
        }
    }

    // ═══════════════════════════════════════════════════
    // INDEX 시트
    // ═══════════════════════════════════════════════════
    private void buildIndexSheet(XSSFWorkbook wb, List<TableInfo> tables) {
        XSSFSheet ws = wb.createSheet("INDEX");

        // 타이틀
        ws.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        Row r0 = ws.createRow(0); r0.setHeightInPoints(32);
        cell(r0, 0, "DB2 테이블 스키마 정의서", titleStyle(wb));

        // 생성일시
        ws.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));
        Row r1 = ws.createRow(1); r1.setHeightInPoints(15);
        cell(r1, 0, "생성일시: " + now(), dateStyle(wb));

        ws.createRow(2).setHeightInPoints(8);

        // 헤더
        String[] hdrs = {"No","테이블명","한글명 (설명)","컬럼 수","시트 이동"};
        int[]    wids = {6, 28, 38, 10, 12};
        Row hr = ws.createRow(3); hr.setHeightInPoints(22);
        CellStyle hs = indexHeaderStyle(wb);
        for (int i = 0; i < hdrs.length; i++) {
            cell(hr, i, hdrs[i], hs);
            ws.setColumnWidth(i, wids[i] * 256);
        }

        // 데이터
        CellStyle even = dataStyle(wb, false);
        CellStyle odd  = dataStyle(wb, true);
        CellStyle link = linkStyle(wb);

        for (int i = 0; i < tables.size(); i++) {
            TableInfo t = tables.get(i);
            Row row = ws.createRow(4 + i); row.setHeightInPoints(18);
            CellStyle rs = (i % 2 == 0) ? even : odd;
            cell(row, 0, String.valueOf(i + 1), rs);
            cell(row, 1, t.getTableName(), rs);
            cell(row, 2, nvl(t.getTableComment()), rs);
            cell(row, 3, String.valueOf(t.getColumns().size()), rs);
            // 하이퍼링크
            Cell lc = row.createCell(4);
            lc.setCellValue("→ 이동"); lc.setCellStyle(link);
            XSSFHyperlink hl = (XSSFHyperlink) wb.getCreationHelper()
                    .createHyperlink(HyperlinkType.DOCUMENT);
            hl.setAddress("'" + safe(t.getTableName()) + "'!A1");
            lc.setHyperlink(hl);
        }

        // 합계
        int tr = 4 + tables.size() + 1;
        Row tot = ws.createRow(tr); tot.setHeightInPoints(18);
        CellStyle ts = totalStyle(wb);
        cell(tot, 0, "합계", ts);
        cell(tot, 1, "", ts);
        cell(tot, 2, "총 " + tables.size() + "개 테이블", ts);
        Cell sumC = tot.createCell(3);
        sumC.setCellFormula("SUM(D5:D" + (4 + tables.size()) + ")");
        sumC.setCellStyle(ts);
        cell(tot, 4, "", ts);
    }

    // ═══════════════════════════════════════════════════
    // 테이블 스키마 시트
    // ═══════════════════════════════════════════════════
    private void buildTableSheet(XSSFWorkbook wb, TableInfo table) {
        XSSFSheet ws = wb.createSheet(safe(table.getTableName()));
        int colCnt = COL_HDRS.length;

        // 섹션 타이틀
        ws.addMergedRegion(new CellRangeAddress(0, 0, 0, colCnt - 1));
        Row r0 = ws.createRow(0); r0.setHeightInPoints(24);
        cell(r0, 0, "[ " + table.getTableName() + " ] 테이블 스키마 정의", sectionStyle(wb));

        // 메타 정보 (4행)
        String[][] meta = {
                {"테이블명",      table.getTableName()},
                {"한글명",        nvl(table.getTableComment())},
                {"스키마",        nvl(table.getSchemaName())},
                {"테이블스페이스", nvl(table.getTablespace())}
        };
        CellStyle ls = labelStyle(wb);
        CellStyle vs = valueStyle(wb);
        for (int i = 0; i < meta.length; i++) {
            Row r = ws.createRow(1 + i); r.setHeightInPoints(16);
            cell(r, 0, meta[i][0], ls);
            cell(r, 1, meta[i][1], vs);
            ws.addMergedRegion(new CellRangeAddress(1 + i, 1 + i, 1, colCnt - 1));
        }

        ws.createRow(5).setHeightInPoints(8);

        // 컬럼 헤더
        Row hr = ws.createRow(6); hr.setHeightInPoints(22);
        CellStyle hs = schemaHeaderStyle(wb);
        for (int i = 0; i < COL_HDRS.length; i++) {
            cell(hr, i, COL_HDRS[i], hs);
            ws.setColumnWidth(i, COL_WIDTHS[i] * 256);
        }

        // 컬럼 데이터
        CellStyle normal  = colStyle(wb, false, false, false);
        CellStyle pkSt    = colStyle(wb, true,  false, false);
        CellStyle idxSt   = colStyle(wb, false, true,  false);
        CellStyle oddSt   = colStyle(wb, false, false, true);

        List<ColumnInfo> cols = table.getColumns();
        for (int i = 0; i < cols.size(); i++) {
            ColumnInfo c = cols.get(i);
            Row row = ws.createRow(7 + i); row.setHeightInPoints(16);

            CellStyle rs = c.isPrimaryKey() ? pkSt
                         : c.isIndexed()    ? idxSt
                         : (i % 2 != 0)     ? oddSt
                         :                    normal;

            cell(row, 0,  String.valueOf(c.getOrdinalPosition()), rs);
            cell(row, 1,  c.getColumnName(), rs);
            cell(row, 2,  nvl(c.getColumnComment()), rs);
            cell(row, 3,  nvl(c.getFullDataType()), rs);
            cell(row, 4,  c.getColumnLength() > 0 ? String.valueOf(c.getColumnLength()) : "", rs);
            cell(row, 5,  c.getDecimalDigits() > 0 ? String.valueOf(c.getDecimalDigits()) : "", rs);
            cell(row, 6,  c.isNullable() ? "Y" : "N", rs);
            cell(row, 7,  c.isPrimaryKey() ? "PK" : "", rs);
            cell(row, 8,  nvl(c.getDefaultValue()), rs);
            cell(row, 9,  c.isIndexed() ? nvl(c.getIndexName()) : "", rs);
            cell(row, 10, "", rs);
        }

        // INDEX 복귀 링크
        Row back = ws.createRow(9 + cols.size()); back.setHeightInPoints(16);
        Cell bc = back.createCell(0);
        bc.setCellValue("← INDEX로 이동"); bc.setCellStyle(linkStyle(wb));
        XSSFHyperlink bl = (XSSFHyperlink) wb.getCreationHelper()
                .createHyperlink(HyperlinkType.DOCUMENT);
        bl.setAddress("'INDEX'!A1");
        bc.setHyperlink(bl);

        ws.createFreezePane(0, 7);
    }

    // ═══════════════════════════════════════════════════
    // DDL_ALL 시트
    // ═══════════════════════════════════════════════════
    private void buildDdlSheet(XSSFWorkbook wb, List<TableInfo> tables) {
        XSSFSheet ws = wb.createSheet("DDL_ALL");
        ws.setColumnWidth(0, 100 * 256);

        Row r0 = ws.createRow(0); r0.setHeightInPoints(28);
        ws.addMergedRegion(new CellRangeAddress(0, 0, 0, 0));
        cell(r0, 0, "DDL 스크립트 전체 모음", titleStyle(wb));

        CellStyle secSt = sectionStyle(wb);
        CellStyle ddlSt = ddlStyle(wb);
        int ri = 2;

        for (TableInfo t : tables) {
            Row sh = ws.createRow(ri++); sh.setHeightInPoints(18);
            ws.addMergedRegion(new CellRangeAddress(ri - 1, ri - 1, 0, 0));
            String label = t.getTableName()
                    + (nvl(t.getTableComment()).isBlank() ? "" : "  (" + t.getTableComment() + ")");
            cell(sh, 0, label, secSt);

            for (String line : t.getDdlScript().split("\n")) {
                Row lr = ws.createRow(ri++); lr.setHeightInPoints(14);
                ws.addMergedRegion(new CellRangeAddress(ri - 1, ri - 1, 0, 0));
                cell(lr, 0, line, ddlSt);
            }
            ri++;
        }
    }

    // ═══════════════════════════════════════════════════
    // 파일 저장
    // ═══════════════════════════════════════════════════
    private String saveFile(XSSFWorkbook wb) throws IOException {
        String dir  = config.getOutputPath();
        String base = config.getOutputFilename();
        String ts   = config.isIncludeDate()
                ? "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                : "";
        new File(dir).mkdirs();
        String path = dir + File.separator + base + ts + ".xlsx";
        try (FileOutputStream fos = new FileOutputStream(path)) {
            wb.write(fos);
        }
        return path;
    }

    // ═══════════════════════════════════════════════════
    // 스타일 팩토리
    // ═══════════════════════════════════════════════════
    private CellStyle titleStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(C_HDR, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = wb.createFont(); f.setBold(true);
        f.setFontHeightInPoints((short)16); f.setColor(new XSSFColor(C_WHITE, null));
        f.setFontName("맑은 고딕"); s.setFont(f);
        return s;
    }

    private CellStyle dateStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(C_DATE_B, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.RIGHT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = wb.createFont(); f.setFontHeightInPoints((short)9);
        f.setColor(new XSSFColor(C_DATE_F, null)); f.setFontName("맑은 고딕"); s.setFont(f);
        return s;
    }

    private CellStyle indexHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(C_DARK, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        border(s, BorderStyle.THIN);
        XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)11);
        f.setColor(new XSSFColor(C_WHITE, null)); f.setFontName("맑은 고딕"); s.setFont(f);
        return s;
    }

    private CellStyle schemaHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(C_HDR, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        border(s, BorderStyle.THIN);
        XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)10);
        f.setColor(new XSSFColor(C_WHITE, null)); f.setFontName("맑은 고딕"); s.setFont(f);
        return s;
    }

    private CellStyle sectionStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(C_SEC, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        border(s, BorderStyle.MEDIUM);
        XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)11);
        f.setColor(new XSSFColor(C_BLUE, null)); f.setFontName("맑은 고딕"); s.setFont(f);
        return s;
    }

    private CellStyle labelStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(C_LBL, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        border(s, BorderStyle.THIN);
        XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)10);
        f.setFontName("맑은 고딕"); s.setFont(f);
        return s;
    }

    private CellStyle valueStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        border(s, BorderStyle.THIN);
        XSSFFont f = wb.createFont(); f.setFontHeightInPoints((short)10);
        f.setFontName("맑은 고딕"); s.setFont(f);
        return s;
    }

    private CellStyle dataStyle(XSSFWorkbook wb, boolean odd) {
        XSSFCellStyle s = wb.createCellStyle();
        if (odd) { s.setFillForegroundColor(new XSSFColor(C_ODD, null));
                   s.setFillPattern(FillPatternType.SOLID_FOREGROUND); }
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        border(s, BorderStyle.THIN);
        XSSFFont f = wb.createFont(); f.setFontHeightInPoints((short)10);
        f.setFontName("맑은 고딕"); s.setFont(f);
        return s;
    }

    private CellStyle colStyle(XSSFWorkbook wb, boolean pk, boolean idx, boolean odd) {
        XSSFCellStyle s = wb.createCellStyle();
        byte[] bg = pk ? C_PK : idx ? C_IDX : odd ? C_ODD : C_WHITE;
        s.setFillForegroundColor(new XSSFColor(bg, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setAlignment(HorizontalAlignment.LEFT);
        border(s, BorderStyle.THIN);
        XSSFFont f = wb.createFont(); f.setFontHeightInPoints((short)10);
        f.setBold(pk); f.setFontName("맑은 고딕"); s.setFont(f);
        return s;
    }

    private CellStyle totalStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(C_TOTAL, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        border(s, BorderStyle.MEDIUM);
        XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)10);
        f.setFontName("맑은 고딕"); s.setFont(f);
        return s;
    }

    private CellStyle linkStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        border(s, BorderStyle.THIN);
        XSSFFont f = wb.createFont();
        f.setUnderline(FontUnderline.SINGLE);
        f.setColor(IndexedColors.BLUE.getIndex());
        f.setFontHeightInPoints((short)10); f.setFontName("맑은 고딕"); s.setFont(f);
        return s;
    }

    private CellStyle ddlStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = wb.createFont();
        f.setFontName("Courier New"); f.setFontHeightInPoints((short)9); s.setFont(f);
        return s;
    }

    // ── 유틸 ──
    private void cell(Row row, int col, String val, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(val != null ? val : "");
        c.setCellStyle(style);
    }

    private void border(XSSFCellStyle s, BorderStyle bs) {
        s.setBorderTop(bs); s.setBorderBottom(bs);
        s.setBorderLeft(bs); s.setBorderRight(bs);
    }

    private String safe(String name) {
        String r = name.replaceAll("[\\\\/*?\\[\\]:]", "_");
        return r.length() > 31 ? r.substring(0, 31) : r;
    }

    private String nvl(String v) { return v != null ? v : ""; }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private static byte[] rgb(String hex) {
        return new byte[]{
            (byte) Integer.parseInt(hex.substring(0, 2), 16),
            (byte) Integer.parseInt(hex.substring(2, 4), 16),
            (byte) Integer.parseInt(hex.substring(4, 6), 16)
        };
    }
}
