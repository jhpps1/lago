package com.example.LAGO.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * DB 메타데이터 ↔ JPA 엔티티 메타모델 정합성 점검 서비스
 *
 * 점검 항목:
 *  - 테이블/뷰 존재 여부 (VIEW: STOCK_DAY/WEEK/MONTH/YEAR 읽기전용)
 *  - 컬럼(이름/타입/NULL) 요약
 *  - PK/FK 요약
 *
 * 성능:
 *  - Java 21 가상 스레드(virtual thread)로 엔티티별 병렬 점검
 *
 * 중요:
 *  - 실제 테이블/컬럼명은 EC2 DB 기준이어야 함 (명세 변경 금지)
 *  - ACCOUNTS.created_at 은 text 타입(스키마 기준)임
 */
@Service
public class SchemaValidationService {

    private final EntityManager em;
    private final DataSource dataSource;

    public SchemaValidationService(EntityManager em, DataSource dataSource) {
        this.em = em;
        this.dataSource = dataSource;
    }

    /** 스키마 전체 점검 실행 */
    public Map<String, Object> validateAll() throws Exception {
        Set<EntityType<?>> entities = em.getMetamodel().getEntities();
        Map<String, TableMeta> dbMeta = loadDatabaseMeta();

        try (ExecutorService es = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<Map<String, Object>>> tasks = new ArrayList<>();
            for (EntityType<?> et : entities) tasks.add(() -> validateEntity(et, dbMeta));

            List<Future<Map<String, Object>>> futures = es.invokeAll(tasks);
            Map<String, Object> result = new TreeMap<>();
            for (Future<Map<String, Object>> f : futures) result.putAll(f.get());

            // DB에는 있는데 JPA엔 없는 테이블/뷰 리스트 (레거시/누락 감시)
            Set<String> jpaTables = entities.stream().map(this::resolveTableName).collect(Collectors.toSet());
            Set<String> orphanDbTables = new TreeSet<>(dbMeta.keySet());
            orphanDbTables.removeAll(jpaTables);
            result.put("_db_only_objects", orphanDbTables);
            return result;
        }
    }

    /** 엔티티 단위 검증 */
    private Map<String, Object> validateEntity(EntityType<?> et, Map<String, TableMeta> dbMeta) {
        String table = resolveTableName(et); // @Table(name) 우선, 없으면 클래스명 대문자
        Map<String, Object> report = new LinkedHashMap<>();
        Map<String, Object> detail = new LinkedHashMap<>();
        TableMeta tm = dbMeta.get(table);

        if (tm == null) {
            detail.put("error", "DB에 테이블/뷰가 존재하지 않음: " + table);
            report.put(table, detail);
            return report;
        }

        detail.put("db_columns", tm.columns);
        detail.put("db_primary_key", tm.pkColumns);
        detail.put("db_foreign_keys", tm.fkMap);

        // TODO: @Column(name, nullable, length, precision, scale) 세부 비교 추가
        // TODO: ENUM/숫자 정밀도/뷰-읽기전용 검증 추가
        report.put(table, detail);
        return report;
    }

    /** 물리 테이블명 해석 (@Table.name 우선) */
    private String resolveTableName(EntityType<?> et) {
        Class<?> clazz = et.getJavaType();
        jakarta.persistence.Table t = clazz.getAnnotation(jakarta.persistence.Table.class);
        if (t != null && !t.name().isBlank()) return t.name();
        return clazz.getSimpleName().toUpperCase(); // 프로젝트 규칙: 테이블명 대문자
    }

    /** DB 메타데이터 수집 (TABLE + VIEW) */
    private Map<String, TableMeta> loadDatabaseMeta() throws SQLException {
        Map<String, TableMeta> map = new TreeMap<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData dm = conn.getMetaData();
            try (ResultSet rs = dm.getTables(null, "public", "%", new String[]{"TABLE", "VIEW"})) {
                while (rs.next()) {
                    String name = rs.getString("TABLE_NAME");
                    map.put(name, new TableMeta(name));
                }
            }
            for (TableMeta tm : map.values()) {
                try (ResultSet rs = dm.getColumns(null, "public", tm.name, "%")) {
                    while (rs.next()) {
                        tm.columns.add(Map.of(
                                "column", rs.getString("COLUMN_NAME"),
                                "type", rs.getString("TYPE_NAME"),
                                "size", rs.getInt("COLUMN_SIZE"),
                                "nullable", rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable
                        ));
                    }
                }
                try (ResultSet rs = dm.getPrimaryKeys(null, "public", tm.name)) {
                    while (rs.next()) tm.pkColumns.add(rs.getString("COLUMN_NAME"));
                }
                try (ResultSet rs = dm.getImportedKeys(null, "public", tm.name)) {
                    while (rs.next()) {
                        tm.fkMap.put(
                                rs.getString("FKCOLUMN_NAME"),
                                rs.getString("PKTABLE_NAME") + "(" + rs.getString("PKCOLUMN_NAME") + ")"
                        );
                    }
                }
            }
        }
        return map;
    }

    /** 내부 보관 구조체 */
    private static class TableMeta {
        final String name;
        final List<Map<String, Object>> columns = new ArrayList<>();
        final List<String> pkColumns = new ArrayList<>();
        final Map<String, String> fkMap = new LinkedHashMap<>();
        TableMeta(String name) { this.name = name; }
    }
}
