package com.example.LAGO.controller;

import com.example.LAGO.service.SchemaValidationService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 스키마 정합성 점검 컨트롤러
 * GET /api/test/schema-diff
 * - DB ↔ JPA 요약 리포트(JSON) 반환
 */
@RestController
@RequestMapping("/api/test")
public class TestSchemaController {

    private final SchemaValidationService schemaValidationService;

    public TestSchemaController(SchemaValidationService schemaValidationService) {
        this.schemaValidationService = schemaValidationService;
    }

    @Operation(summary = "DB ↔ JPA 스키마 정합성 점검",
            description = "테이블/뷰/컬럼/PK/FK 요약 및 DB에만 존재하는 객체 목록 반환")
    @GetMapping("/schema-diff")
    public ResponseEntity<Map<String, Object>> schemaDiff() throws Exception {
        return ResponseEntity.ok(schemaValidationService.validateAll());
    }
}
