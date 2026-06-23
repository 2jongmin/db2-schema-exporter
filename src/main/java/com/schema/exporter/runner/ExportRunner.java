package com.schema.exporter.runner;

import com.schema.exporter.model.TableInfo;
import com.schema.exporter.service.ExcelService;
import com.schema.exporter.service.SchemaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 엑셀 내보내기 실행 컴포넌트
 * main() 에서 Spring Context 초기화 후 이 빈을 호출
 */
@Component
public class ExportRunner {

    private static final Logger log = LoggerFactory.getLogger(ExportRunner.class);

    private final SchemaService schemaService;
    private final ExcelService  excelService;

    @Autowired
    public ExportRunner(SchemaService schemaService, ExcelService excelService) {
        this.schemaService = schemaService;
        this.excelService  = excelService;
    }

    public void run() throws Exception {
        log.info("=== DB2 Schema Excel Exporter 시작 ===");

        // 1. DAO → Mapper(XML) → DB2 SYSCAT 뷰 조회
        List<TableInfo> tables = schemaService.extractTables();

        if (tables.isEmpty()) {
            log.warn("추출된 테이블이 없습니다. 설정(database.properties)을 확인하세요.");
            return;
        }

        log.info("총 {}개 테이블 추출 완료. 엑셀 생성 시작...", tables.size());

        // 2. 엑셀 생성
        String outputPath = excelService.export(tables);

        log.info("=== 완료: {} ===", outputPath);
        System.out.println("\n✅ 엑셀 생성 완료: " + outputPath);
    }
}
