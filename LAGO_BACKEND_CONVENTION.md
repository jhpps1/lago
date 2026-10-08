
# 🏛️ LAGO 백엔드 프로젝트 구조 & 코드 컨벤션 (2025)

> **팀 협업, 리뷰, AI 코딩툴 사용, 신규 파일 생성 기준!**
>  
> [최신 갱신일: 2025.08 / 담당: @준형박, 팀원 전체]

---

## 📂 1. 프로젝트 디렉터리 구조 (표준 예시)

```
src/
  main/
    java/
      com/
        example/
          LAGO/
            LagoApplication.java
            config/
            constants/
            controller/
            domain/
            dto/
              request/
              response/
              AccountDto.java
            exception/
            repository/
            service/
            utils/
            ai/
              sentiment/
                dto/
              strategy/
                dto/
    resources/
      application.properties         # 공통 설정
      application-dev.properties     # 개발용
      application-prod.properties    # 배포용
  test/
```

- **Controller/Service/Repository/Domain/Dto** 등 반드시 명확 분리
- 요청 DTO는 `dto/request/`, 응답 DTO는 `dto/response/`, 공용 DTO만 `dto/`에
- 소문자 파일, RequestDto/ResponseDto 등 혼용 금지

---

## ✨ 2. 네이밍 컨벤션 (파일/클래스)

| 용도            | 예시                   | 위치          |
|:----------------|:-----------------------|:--------------|
| 요청 DTO        | `TradeRequest.java`    | dto/request/  |
| 응답 DTO        | `TradeResponse.java`   | dto/response/ |
| 내부 DTO        | `AccountDto.java`      | dto/          |
| 예외 응답       | `ErrorResponse.java`   | exception/    |
| 컨트롤러        | `StockController.java` | controller/   |
| 서비스          | `StockService.java`    | service/      |
| 엔티티          | `Stock.java`           | domain/       |
| 레포지토리      | `StockRepository.java` | repository/   |

> ❌ `RequestDto`, `ResponseDto`, `Res`, 소문자 시작 금지  
> ❌ 같은 이름 DTO 여러 위치 중복 생성 금지  
> ❌ Controller에서 Entity 바로 반환 금지 (항상 DTO 변환)

---

## 📝 3. 주석 컨벤션

- 클래스/메서드에 Javadoc 필수
```java
/**
 * 주식 매수 요청 DTO
 */
public class TradeRequest { ... }
```
- 중요한 로직/비즈니스 흐름만 한글 주석, TODO/FIXME 표준만 허용

---

## 🚦 4. PR & 코드 리뷰 규칙

- DTO/Entity 등 공용 파일 수정 전 팀에 공지
- PR 전 항상 최신 develop/backend-dev 브랜치 pull/rebase
- 네이밍 컨벤션, 파일 위치, Request/Response 혼용 등 위반은 리뷰에서 반드시 지적
- 코드 스타일/의존성(import)도 리뷰 범위

---

## 💡 5. 실전 예시

### TradeRequest.java
```java
package com.example.LAGO.dto.request;

/**
 * 주식 매수 요청 DTO
 */
public class TradeRequest {
    private Long userId;
    private Long stockId;
    private Integer quantity;
}
```

### TradeResponse.java
```java
package com.example.LAGO.dto.response;

/**
 * 주식 매수 응답 DTO
 */
public class TradeResponse {
    private Long transactionId;
    private String status;
    private Integer afterBalance;
}
```

---

## ⚠️ 6. 자주 발생하는 실수

- Request/Response/Dto 혼용, 대소문자 혼동
- 같은 이름 파일 여러 위치에 생성 (중복 생성 금지)
- RequestDto, ResponseDto 등 접미사 남용
- Controller에서 Entity/Domain 직접 반환 → 반드시 DTO로 감싸야 함!

---

## 🌍 8. 환경별 설정 관리

| 환경 | 파일명 | 용도 | 활성화 방법 |
|:-----|:-------|:-----|:------------|
| 공통 | `application.properties` | 모든 환경 공통 설정 | 항상 로드 |
| 개발 | `application-dev.properties` | 로컬 개발/디버깅 | `--spring.profiles.active=dev` (기본) |
| 배포 | `application-prod.properties` | Docker/운영 배포 | `SPRING_PROFILES_ACTIVE=prod` |

### 환경별 주요 차이점:
- **공통:** 애플리케이션명, Swagger, Security 설정
- **개발:** 실제 EC2 DB, 상세 로그, 로컬 Redis  
- **배포:** 환경변수 보안, 성능 최적화, Docker 설정

---

## 🧑‍💻 7. 자동화/도구 추천

- [ ] Checkstyle, SonarLint, EditorConfig 등 코드 스타일 자동 검사
- [ ] Notion/README에 본 컨벤션 고정, AI 코딩툴/신규 멤버 Onboarding에 활용

---

# 🎯 컨벤션을 지키면 협업/리뷰/자동화가 쉬워집니다!
- 질문/피드백/코드/구조 개선 의견 환영

