package com.schema.exporter.service;

import com.schema.exporter.model.ColumnInfo;
import com.schema.exporter.model.TableInfo;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Apache POI 기반 엑셀 내보내기 서비스
 * - 목차(INDEX) 시트
 * - 테이블별 스키마 시트
 * - DDL 전체 모아보기 시트
 */
public class ExcelExportService {

    private static final Logger log = LoggerFactory.getLogger(ExcelExportService.class);

    // ===================== 색상 팔레트 =====================
    private static final byte[] COLOR_HEADER_BG    = hexToRgb("1F4E79"); // 진청색
    private static final byte[] COLOR_PK_BG        = hexToRgb("FFF2CC"); // 연노랑
    private static final byte[] COLOR_INDEX_BG     = hexToRgb("E2EFDA"); // 연녹색
    private static final byte[] COLOR_ODD_ROW_BG   = hexToRgb("F5F5F5"); // 연회색
    private static final byte[] COLOR_SECTION_BG   = hexToRgb("D6E4F0"); // 연파랑 (목차 헤더)
    private static final byte[] COLOR_WHITE         = hexToRgb("FFFFFF");
    private static final byte[] COLOR_BLACK         = hexToRgb("000000");

    // ===================== 컬럼 헤더 =====================
    private static final String[] SCHEMA_HEADERS = {
            "No", "컬럼명", "한글명", "데이터타입", "길이", "소수점", "NULL허용", "PK", "기본값", "인덱스명", "비고"
    };
    private static final int[] SCHEMA_COL_WIDTHS = {
            5, 25, 20, 18, 8, 8, 10, 6, 15, 20, 20
    };

    public void export(List<TableInfo> tables, String outputPath, String filenameBase, boolean includeDate) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // 시트 생성
            XSSFSheet indexSheet = wb.createSheet("INDEX");
            createIndexSheet(wb, indexSheet, tables);

            for (TableInfo table : tables) {
                String sheetName = sanitizeSheetName(table.getTableName());
                XSSFSheet sheet = wb.createSheet(sheetName);
                createTableSheet(wb, sheet, table);
            }

            XSSFSheet ddlSheet = wb.createSheet("DDL_ALL");
            createDdlSheet(wb, ddlSheet, tables);

            // 파일 저장
            String filename = buildFilename(outputPath, filenameBase, includeDate);
            new File(outputPath).mkdirs();
            try (FileOutputStream fos = new FileOutputStream(filename)) {
                wb.write(fos);
            }
            log.info("엑셀 파일 생성 완료: {}", filename);
            System.out.println("✅ 엑셀 파일 생성 완료: " + filename);
        }
    }

    // =====================================================================
    // INDEX 시트
    // =====================================================================
    private void createIndexSheet(XSSFWorkbook wb, XSSFSheet sheet, List<TableInfo> tables) {
        // 타이틀
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(30);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("DB2 테이블 스키마 정의서");
        titleCell.setCellStyle(buildTitleStyle(wb));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        // 생성일시
        Row dateRow = sheet.createRow(1);
        Cell dateCell = dateRow.createCell(0);
        dateCell.setCellValue("생성일시: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        dateCell.setCellStyle(buildDateStyle(wb));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

        // 빈 줄
        sheet.createRow(2);

        // 헤더
        String[] headers = {"No", "테이블명", "한글명 (설명)", "컬럼 수", "시트 이동"};
        int[] widths      = {6, 30, 40, 10, 12};

        Row headerRow = sheet.createRow(3);
        headerRow.setHeightInPoints(20);
        CellStyle hStyle = buildIndexHeaderStyle(wb);
        for (int i = 0; i < headers.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(hStyle);
            sheet.setColumnWidth(i, widths[i] * 256);
        }

        // 데이터 행
        CellStyle evenStyle = buildDataStyle(wb, false);
        CellStyle oddStyle  = buildDataStyle(wb, true);
        CellStyle linkStyle = buildLinkStyle(wb);

        for (int i = 0; i < tables.size(); i++) {
            TableInfo t = tables.get(i);
            Row row = sheet.createRow(4 + i);
            row.setHeightInPoints(18);
            CellStyle rowStyle = (i % 2 == 0) ? evenStyle : oddStyle;

            setCell(row, 0, String.valueOf(i + 1), rowStyle);
            setCell(row, 1, t.getTableName(), rowStyle);
            setCell(row, 2, t.getTableComment(), rowStyle);
            setCell(row, 3, String.valueOf(t.getColumns().size()), rowStyle);

            // 하이퍼링크
            Cell linkCell = row.createCell(4);
            linkCell.setCellValue("→ 이동");
            linkCell.setCellStyle(linkStyle);
            XSSFHyperlink link = (XSSFHyperlink) wb.getCreationHelper()
                    .createHyperlink(HyperlinkType.DOCUMENT);
            link.setAddress("'" + sanitizeSheetName(t.getTableName()) + "'!A1");
            linkCell.setHyperlink(link);
        }

        // 요약 행
        int summaryRow = 4 + tables.size() + 1;
        Row totalRow = sheet.createRow(summaryRow);
        CellStyle totalStyle = buildTotalStyle(wb);
        setCell(totalRow, 0, "합계", totalStyle);
        setCell(totalRow, 1, "", totalStyle);
        setCell(totalRow, 2, "총 " + tables.size() + "개 테이블", totalStyle);
        setCell(totalRow, 3, "=SUM(D5:D" + (4 + tables.size()) + ")", totalStyle);
        setCell(totalRow, 4, "", totalStyle);
    }

    // =====================================================================
    // 테이블 스키마 시트
    // =====================================================================
    private void createTableSheet(XSSFWorkbook wb, XSSFSheet sheet, TableInfo table) {
        // 테이블 정보 헤더 블록
        CellStyle labelStyle = buildLabelStyle(wb);
        CellStyle valueStyle = buildValueStyle(wb);
        CellStyle sectionStyle = buildSectionStyle(wb);

        // 섹션 타이틀
        Row secRow = sheet.createRow(0);
        secRow.setHeightInPoints(22);
        Cell secCell = secRow.createCell(0);
        secCell.setCellValue("[ " + table.getTableName() + " ] 테이블 스키마 정의");
        secCell.setCellStyle(sectionStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, SCHEMA_HEADERS.length - 1));

        // 테이블 기본 정보
        String[][] meta = {
                {"테이블명", table.getTableName()},
                {"한글명",   table.getTableComment()},
                {"스키마",   table.getSchemaName()},
                {"테이블스페이스", table.getTablespace() != null ? table.getTablespace() : ""}
        };
        for (int i = 0; i < meta.length; i++) {
            Row r = sheet.createRow(1 + i);
            r.setHeightInPoints(16);
            Cell lbl = r.createCell(0); lbl.setCellValue(meta[i][0]); lbl.setCellStyle(labelStyle);
            Cell val = r.createCell(1); val.setCellValue(meta[i][1]); val.setCellStyle(valueStyle);
            sheet.addMergedRegion(new CellRangeAddress(1 + i, 1 + i, 1, SCHEMA_HEADERS.length - 1));
        }

        // 빈 행
        sheet.createRow(5);

        // 컬럼 헤더
        Row hRow = sheet.createRow(6);
        hRow.setHeightInPoints(20);
        CellStyle hStyle = buildSchemaHeaderStyle(wb);
        for (int i = 0; i < SCHEMA_HEADERS.length; i++) {
            Cell c = hRow.createCell(i);
            c.setCellValue(SCHEMA_HEADERS[i]);
            c.setCellStyle(hStyle);
            sheet.setColumnWidth(i, SCHEMA_COL_WIDTHS[i] * 256);
        }

        // 컬럼 데이터
        CellStyle normalStyle  = buildColDataStyle(wb, false, false);
        CellStyle pkStyle      = buildColDataStyle(wb, true,  false);
        CellStyle indexStyle   = buildColDataStyle(wb, false, true);
        CellStyle oddNormal    = buildColDataStyle(wb, false, false, true);

        List<ColumnInfo> cols = table.getColumns();
        for (int i = 0; i < cols.size(); i++) {
            ColumnInfo col = cols.get(i);
            Row row = sheet.createRow(7 + i);
            row.setHeightInPoints(16);

            CellStyle rowStyle;
            if (col.isPrimaryKey())   rowStyle = pkStyle;
            else if (col.isIndexed()) rowStyle = indexStyle;
            else if (i % 2 != 0)     rowStyle = oddNormal;
            else                      rowStyle = normalStyle;

            setCell(row, 0,  String.valueOf(col.getOrdinalPosition()), rowStyle);
            setCell(row, 1,  col.getColumnName(),                       rowStyle);
            setCell(row, 2,  col.getColumnComment(),                    rowStyle);
            setCell(row, 3,  col.getFullDataType(),                     rowStyle);
            setCell(row, 4,  col.getColumnLength() > 0 ? String.valueOf(col.getColumnLength()) : "", rowStyle);
            setCell(row, 5,  col.getDecimalDigits() > 0 ? String.valueOf(col.getDecimalDigits()) : "", rowStyle);
            setCell(row, 6,  col.isNullable() ? "Y" : "N",             rowStyle);
            setCell(row, 7,  col.isPrimaryKey() ? "PK" : "",           rowStyle);
            setCell(row, 8,  col.getDefaultValue(),                     rowStyle);
            setCell(row, 9,  col.getIndexName() != null ? col.getIndexName() : "", rowStyle);
            setCell(row, 10, "",                                         rowStyle);
        }

        // INDEX 시트 이동 링크
        int backRow = 8 + cols.size();
        Row back = sheet.createRow(backRow);
        Cell backCell = back.createCell(0);
        backCell.setCellValue("← INDEX로 이동");
        backCell.setCellStyle(buildLinkStyle(wb));
        XSSFHyperlink backLink = (XSSFHyperlink) wb.getCreationHelper()
                .createHyperlink(HyperlinkType.DOCUMENT);
        backLink.setAddress("'INDEX'!A1");
        backCell.setHyperlink(backLink);

        // 시트 고정
        sheet.createFreezePane(0, 7);
    }

    // =====================================================================
    // DDL 전체 모아보기 시트
    // =====================================================================
    private void createDdlSheet(XSSFWorkbook wb, XSSFSheet ddlSheet, List<TableInfo> tables) {
        CellStyle titleStyle   = buildTitleStyle(wb);
        CellStyle ddlStyle     = buildDdlStyle(wb);
        CellStyle tableHStyle  = buildSectionStyle(wb);

        // 타이틀
        Row titleRow = ddlSheet.createRow(0);
        titleRow.setHeightInPoints(28);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("DDL 스크립트 전체 모음");
        titleCell.setCellStyle(titleStyle);
        ddlSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));
        ddlSheet.setColumnWidth(0, 12 * 256);
        ddlSheet.setColumnWidth(1, 80 * 256);

        int rowIdx = 2;
        for (TableInfo table : tables) {
            // 테이블 구분 헤더
            Row th = ddlSheet.createRow(rowIdx++);
            th.setHeightInPoints(18);
            Cell thCell = th.createCell(0);
            thCell.setCellValue(table.getTableName() + (table.getTableComment().isEmpty() ? "" : "  (" + table.getTableComment() + ")"));
            thCell.setCellStyle(tableHStyle);
            ddlSheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 2));

            // DDL 줄 단위로 출력
            String[] lines = table.getDdlScript().split("\n");
            for (String line : lines) {
                Row lineRow = ddlSheet.createRow(rowIdx++);
                lineRow.setHeightInPoints(15);
                Cell lineCell = lineRow.createCell(0);
                lineCell.setCellValue(line);
                lineCell.setCellStyle(ddlStyle);
                ddlSheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 2));
            }
            rowIdx++; // 빈 행
        }
    }

    // =====================================================================
    // 스타일 팩토리
    // =====================================================================
    private CellStyle buildTitleStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(COLOR_HEADER_BG, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)16);
        f.setColor(new XSSFColor(COLOR_WHITE, null));
        s.setFont(f);
        return s;
    }

    private CellStyle buildDateStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(hexToRgb("2E4053"), null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.RIGHT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = wb.createFont(); f.setFontHeightInPoints((short)10);
        f.setColor(new XSSFColor(hexToRgb("BFC9CA"), null));
        s.setFont(f);
        return s;
    }

    private CellStyle buildIndexHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(hexToRgb("2C3E50"), null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(s, BorderStyle.THIN);
        XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)11);
        f.setColor(new XSSFColor(COLOR_WHITE, null));
        s.setFont(f);
        return s;
    }

    private CellStyle buildSchemaHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(COLOR_HEADER_BG, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(s, BorderStyle.THIN);
        XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)10);
        f.setColor(new XSSFColor(COLOR_WHITE, null));
        s.setFont(f);
        return s;
    }

    private CellStyle buildDataStyle(XSSFWorkbook wb, boolean odd) {
        XSSFCellStyle s = wb.createCellStyle();
        if (odd) {
            s.setFillForegroundColor(new XSSFColor(COLOR_ODD_ROW_BG, null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(s, BorderStyle.THIN);
        XSSFFont f = wb.createFont(); f.setFontHeightInPoints((short)10);
        s.setFont(f);
        return s;
    }

    private CellStyle buildColDataStyle(XSSFWorkbook wb, boolean isPk, boolean isIndex) {
        return buildColDataStyle(wb, isPk, isIndex, false);
    }

    private CellStyle buildColDataStyle(XSSFWorkbook wb, boolean isPk, boolean isIndex, boolean odd) {
        XSSFCellStyle s = wb.createCellStyle();
        if (isPk) {
            s.setFillForegroundColor(new XSSFColor(COLOR_PK_BG, null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        } else if (isIndex) {
            s.setFillForegroundColor(new XSSFColor(COLOR_INDEX_BG, null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        } else if (odd) {
            s.setFillForegroundColor(new XSSFColor(COLOR_ODD_ROW_BG, null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setAlignment(HorizontalAlignment.LEFT);
        setBorder(s, BorderStyle.THIN);
        XSSFFont f = wb.createFont(); f.setFontHeightInPoints((short)10);
        f.setFontName("맑은 고딕");
        s.setFont(f);
        return s;
    }

    private CellStyle buildLabelStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(hexToRgb("EBF5FB"), null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(s, BorderStyle.THIN);
        XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)10);
        f.setFontName("맑은 고딕");
        s.setFont(f);
        return s;
    }

    private CellStyle buildValueStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(s, BorderStyle.THIN);
        XSSFFont f = wb.createFont(); f.setFontHeightInPoints((short)10);
        f.setFontName("맑은 고딕");
        s.setFont(f);
        return s;
    }

    private CellStyle buildSectionStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(COLOR_SECTION_BG, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(s, BorderStyle.MEDIUM);
        XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)11);
        f.setFontName("맑은 고딕");
        f.setColor(new XSSFColor(hexToRgb("1B4F72"), null));
        s.setFont(f);
        return s;
    }

    private CellStyle buildTotalStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(hexToRgb("D5DBDB"), null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(s, BorderStyle.MEDIUM);
        XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)10);
        s.setFont(f);
        return s;
    }

    private CellStyle buildLinkStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(s, BorderStyle.THIN);
        XSSFFont f = wb.createFont(); f.setUnderline(FontUnderline.SINGLE);
        f.setColor(IndexedColors.BLUE.getIndex());
        f.setFontHeightInPoints((short)10);
        s.setFont(f);
        return s;
    }

    private CellStyle buildDdlStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(false);
        XSSFFont f = wb.createFont();
        f.setFontName("Courier New");
        f.setFontHeightInPoints((short)9);
        s.setFont(f);
        return s;
    }

    // =====================================================================
    // 유틸리티
    // =====================================================================
    private void setCell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value : "");
        c.setCellStyle(style);
    }

    private void setBorder(XSSFCellStyle s, BorderStyle bs) {
        s.setBorderTop(bs); s.setBorderBottom(bs);
        s.setBorderLeft(bs); s.setBorderRight(bs);
    }

    private String sanitizeSheetName(String name) {
        String result = name.replaceAll("[\\\\/*?\\[\\]:]", "_");
        return result.length() > 31 ? result.substring(0, 31) : result;
    }

    private String buildFilename(String path, String base, boolean includeDate) {
        String sep = path.endsWith("/") || path.endsWith("\\") ? "" : File.separator;
        if (includeDate) {
            String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            return path + sep + base + "_" + date + ".xlsx";
        }
        return path + sep + base + ".xlsx";
    }

    private static byte[] hexToRgb(String hex) {
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return new byte[]{(byte)r, (byte)g, (byte)b};
    }
}
