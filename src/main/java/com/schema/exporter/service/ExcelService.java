package com.schema.exporter.service;

import com.schema.exporter.model.TableInfo;

import java.io.IOException;
import java.util.List;

/**
 * 엑셀 내보내기 서비스 인터페이스
 */
public interface ExcelService {

    /**
     * 테이블 목록을 엑셀 파일로 내보내기
     */
    String export(List<TableInfo> tables) throws IOException;
}
