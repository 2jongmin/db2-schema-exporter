package com.schema.exporter;

import com.schema.exporter.runner.ExportRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * DB2 Schema Excel Exporter - Spring Framework 기반 진입점
 *
 * 실행 방법:
 *   mvn package
 *   java -jar target/db2-schema-exporter-1.0.0-jar-with-dependencies.jar
 */
public class Db2SchemaExporterApplication {

    public static void main(String[] args) throws Exception {
        // Spring ApplicationContext 초기화 (applicationContext.xml)
        ApplicationContext ctx =
                new ClassPathXmlApplicationContext("applicationContext.xml");

        // ExportRunner 빈 취득 후 실행
        ExportRunner runner = ctx.getBean(ExportRunner.class);
        runner.run();

        // Context 정상 종료
        ((ClassPathXmlApplicationContext) ctx).close();
    }
}
