package com.schema.exporter;

import com.schema.exporter.model.ExportConfig;
import com.schema.exporter.model.TableInfo;
import com.schema.exporter.service.Db2SchemaService;
import com.schema.exporter.service.ExcelExportService;
import com.schema.exporter.util.ConfigLoader;
import com.schema.exporter.util.MockDataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.List;

/**
 * DB2 테이블 스키마 엑셀 내보내기 메인 클래스
 *
 * 실행 방법:
 *   - 실제 DB 연결: java -jar db2-schema-exporter.jar
 *   - 테스트 모드:  java -jar db2-schema-exporter.jar --mock
 */
public class Db2SchemaExporterApplication {

    private static final Logger log = LoggerFactory.getLogger(Db2SchemaExporterApplication.class);

    public static void main(String[] args) throws Exception {
        boolean mockMode = args.length > 0 && "--mock".equals(args[0]);

        log.info("=== DB2 Schema Excel Exporter 시작 ===");
        log.info("실행 모드: {}", mockMode ? "MOCK(테스트)" : "실제 DB 연결");

        // 설정 로드
        ExportConfig config = ConfigLoader.load();

        List<TableInfo> tables;

        if (mockMode) {
            // ── 테스트 모드: 샘플 데이터 사용 ──
            log.info("Mock 데이터로 엑셀 생성 중...");
            tables = MockDataGenerator.generateSampleTables(config);
        } else {
            // ── 실제 DB 모드 ──
            Db2SchemaService schemaService = new Db2SchemaService(config);
            log.info("DB 접속 중... URL: {}", config.getDbUrl());

            try (Connection conn = schemaService.getConnection()) {
                log.info("DB 접속 성공. 스키마 추출 시작: {}", config.getSchema());
                tables = schemaService.extractAllTables(conn);
            }
        }

        if (tables.isEmpty()) {
            log.warn("추출된 테이블이 없습니다. 설정을 확인하세요.");
            return;
        }

        log.info("총 {}개 테이블 추출 완료. 엑셀 생성 중...", tables.size());

        // 엑셀 생성
        ExcelExportService excelService = new ExcelExportService();
        excelService.export(tables, config.getOutputPath(), config.getOutputFilename(), config.isIncludeDate());

        log.info("=== 작업 완료 ===");
    }
}
