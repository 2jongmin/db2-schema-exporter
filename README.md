# db2-schema-exporter

DB2 테이블 스키마 및 DDL 스크립트를 엑셀로 내보내는 도구

## 기술 스택

| 구분 | 기술 |
|------|------|
| Framework | Spring Framework 5.3 |
| ORM | MyBatis 3.5 + XML Mapper |
| DB | IBM DB2 (SYSCAT 뷰) |
| Connection Pool | HikariCP |
| Excel | Apache POI 5.2 |
| Build | Maven |

## 프로젝트 구조

```
src/main/java/com/schema/exporter/
├── Db2SchemaExporterApplication.java   # 진입점 (Spring Context 초기화)
├── config/
├── model/
│   ├── ColumnInfo.java                 # 컬럼 정보 모델
│   ├── TableInfo.java                  # 테이블 정보 모델
│   └── ExportConfig.java              # 설정 (@Value 주입)
├── mapper/
│   └── SchemaMapper.java              # MyBatis Mapper 인터페이스
├── dao/
│   ├── SchemaDao.java                 # DAO 인터페이스
│   └── impl/SchemaDaoImpl.java        # SqlSessionTemplate 구현체
├── service/
│   ├── SchemaService.java             # 스키마 추출 서비스 인터페이스
│   ├── ExcelService.java              # 엑셀 생성 서비스 인터페이스
│   └── impl/
│       ├── SchemaServiceImpl.java     # DAO 호출 → DDL 생성
│       └── ExcelServiceImpl.java      # Apache POI 엑셀 생성
└── runner/
    └── ExportRunner.java              # 실행 컴포넌트

src/main/resources/
├── applicationContext.xml             # Spring Bean 설정 (DataSource, MyBatis)
├── mybatis-config.xml                 # MyBatis 전역 설정
├── database.properties                # DB 접속 및 출력 설정
└── mapper/
    └── SchemaMapper.xml               # SQL 쿼리 (SYSCAT 뷰)
```

## 실행 방법

### 1. DB2 JDBC 드라이버 설정
`pom.xml`의 주석 해제 후 드라이버 경로 지정:
```xml
<dependency>
    <groupId>com.ibm.db2</groupId>
    <artifactId>jcc</artifactId>
    <version>11.5.9.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/db2jcc4.jar</systemPath>
</dependency>
```

### 2. database.properties 설정
```properties
db.url=jdbc:db2://localhost:50000/SAMPLEDB
db.username=db2admin
db.password=password
db.schema=MYSCHEMA

# 특정 테이블만 추출 (비우면 전체)
export.tables=TB_USER,TB_BOARD
```

### 3. 빌드 및 실행
```bash
mvn package
java -jar target/db2-schema-exporter-1.0.0-jar-with-dependencies.jar
```

## 엑셀 출력 구성

| 시트 | 내용 |
|------|------|
| INDEX | 전체 테이블 목차 + 하이퍼링크 |
| [테이블명] | 컬럼 스키마 (PK=노랑, 인덱스=연녹색) |
| DDL_ALL | 전체 CREATE TABLE + COMMENT 스크립트 |

## 아키텍처 흐름

```
main()
  └─ Spring ApplicationContext 로드 (applicationContext.xml)
       └─ ExportRunner.run()
            ├─ SchemaService.extractTables()
            │    └─ SchemaDao (interface)
            │         └─ SchemaDaoImpl (SqlSessionTemplate)
            │              └─ SchemaMapper.xml (SYSCAT 뷰 SQL)
            │                   └─ DB2
            └─ ExcelService.export()
                 └─ ExcelServiceImpl (Apache POI)
                      └─ output/*.xlsx
```
