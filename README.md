# 라고할때

주식 투자 초보자를 위한 모의투자 학습 앱이다. SSAFY 13기 공통 프로젝트이고, 기간은 2025.07.07부터 2025.08.18까지다.

알기 쉬운 용어로 학습하고, 실제 주가를 붙인 모의투자로 실전 경험을 쌓는 것이 목표다.

[영상 포트폴리오](https://youtu.be/wsXnRQZ3P1M)

<img src="images/LAGO_thumbnail.JPG" alt="썸네일" width="80%"/>

## 목차

- [화면](#화면)
- [기술 특징](#기술-특징)
- [기술 스택](#기술-스택)
- [패키지 구조](#패키지-구조)
- [산출물](#산출물)
- [팀](#팀)

## 화면

### 메인

<img src="images/main/agree_conditions.jpg" alt="약관동의" height="280"/> <img src="images/main/personality_result.png" alt="성향검사" height="280"/>

<img src="images/main/home_top.jpg" alt="홈 상단" height="280"/> <img src="images/main/home_bottom.png" alt="홈 하단" height="280"/> <img src="images/main/transaction_history.jpg" alt="거래내역" height="280"/>

### 모의투자

<img src="images/trade/list_1.png" alt="종목 리스트 1" height="280"/> <img src="images/trade/list_2.jpg" alt="종목 리스트 2" height="280"/>

<img src="images/trade/chart_1.png" alt="차트 1" height="280"/> <img src="images/trade/chart_2.png" alt="차트 2" height="280"/> <img src="images/trade/chart_3.jpg" alt="차트 3" height="280"/>

<img src="images/trade/trade_1.jpg" alt="주문 1" height="280"/> <img src="images/trade/trade_2.jpg" alt="주문 2" height="280"/>

### 차트 패턴 분석

<img src="images/analyze/analyze_1.jpg" alt="패턴 분석 1" height="280"/> <img src="images/analyze/analyze_2.png" alt="패턴 분석 2" height="280"/>

### 학습

<img src="images/study/study_home.png" alt="학습 홈" height="280"/> <img src="images/study/chart_learn.png" alt="차트 패턴 학습" height="280"/> <img src="images/study/terms.png" alt="단어장" height="280"/>

<img src="images/study/quiz_daily.jpg" alt="데일리 퀴즈" height="280"/> <img src="images/study/quiz_result_daily.jpg" alt="데일리 퀴즈 결과" height="280"/>

<img src="images/study/quiz_random.jpg" alt="랜덤 퀴즈" height="280"/> <img src="images/study/quiz_result_random.jpg" alt="랜덤 퀴즈 결과" height="280"/>

### 역사 챌린지

<img src="images/challenge/challenge_home.png" alt="역사 챌린지" height="280"/>

### 뉴스

<img src="images/news/news1.jpg" alt="뉴스 목록" height="280"/> <img src="images/news/news2.jpg" alt="뉴스" height="280"/> <img src="images/news/news3_detail.jpg" alt="뉴스 상세" height="280"/>

### 성향별 매매봇

<img src="images/trade_bot/trade_bot1.png" alt="매매봇 1" height="280"/> <img src="images/trade_bot/trade_bot2.jpg" alt="매매봇 2" height="280"/>

### 마이페이지

<img src="images/mypage/mypage1.jpg" alt="마이페이지 1" height="280"/> <img src="images/mypage/mypage2.jpg" alt="마이페이지 2" height="280"/> <img src="images/mypage/mypage3_ranking.jpg" alt="랭킹" height="280"/>

## 기술 특징

### 웹소켓 구독

한국투자증권 API로 실제 주가를 받고, 웹소켓으로 화면에 보낸다. 화면에 보이는 종목만 구독하고, 스크롤로 사라진 종목은 구독을 끊는다.

### PostgreSQL과 TimescaleDB

`ticks`는 `schema_setup.sql`의 하이퍼테이블이다. 기준 열은 `ts`이고, `stock_info_id`로 2분할한다. 연속 집계는 일(`ticks_day`), 주(`ticks_week`), 월(`ticks_month`)이다. 분 단위 집계 뷰는 이 스키마에 없다. 압축 정책과 삽입 속도 측정값은 이 저장소에 없다.

### 차트 패턴 분석

가격만 비교하지 않는다. 선형회귀로 추세선을 그리고, 기울기와 결정 계수로 차트 패턴을 찾는다.

### 데일리 퀴즈

매일 랜덤한 시간에 데일리 퀴즈 알람을 보낸다.

### 뉴스

FinBERT는 별도 Flask 컨테이너다. Spring이 HTTP로 호출한다. compose에서 Flask는 호스트 5000, Spring은 8081이다.

기사는 구글 RSS에서 주소를 찾고, 기사 주소 해독은 selectolax를 쓴다. 언론사 본문을 읽은 뒤 감정 점수를 -1.0에서 1.0으로 맞춘다.

Selenium 코드는 있다. `RENDER_JS`가 켜져 있으면 JS 렌더링을 시도한다. FinBERT 이미지에는 Chrome과 chromedriver가 없어서 그 경로는 동작하지 않는다.

### 성향별 매매봇

점수는 뉴스 감정, 기술 점수, 등락률을 합친다. 기술 점수에는 RSI, MACD, 볼린저밴드, 이동평균, 골든 크로스, 데드 크로스가 들어 있다. 기술 지표 계산은 Java 21 가상 스레드로 병렬 처리한다.

| 성향 | 점수 | 매수 | 매도 |
| --- | --- | --- | --- |
| 화끈이 | (뉴스×1.3 + 기술 + 등락률) / 3 | 0.3 이상 | -0.2 미만 |
| 적극이 | (뉴스 + 기술×1.2 + 등락률) / 3 | 0.1 이상 | -0.3 미만 |
| 균형이 | (뉴스 + 기술 + 등락률) / 3 × 0.8 | 0.5 이상 | -0.5 미만 |
| 조심이 | 뉴스가 음수면 1.5배, 아니면 0.7배 | 0.7 이상 | -0.3 미만 |

Spring 스케줄러가 매분 AI 봇을 실행한다. 종목은 삼성전자(005930), 수량은 1주다. 가격은 Redis `realtime:stock:{코드}`의 `closePrice`다. 없으면 `ticks` 종가다. 둘 다 없으면 주문하지 않는다. 주문은 Redis `orders-stream`에 넣고, 컨슈머가 기존 주문 처리로 넘긴다.

## 기술 스택

| 분류 | 기술 |
| --- | --- |
| 모바일 | Android Studio, Kotlin, Jetpack Compose, Dagger Hilt, Room, Coroutines, TradingView Charting Library |
| 백엔드 | Java 21, Spring Boot, Spring Security, WebFlux, Spring Data JPA, OAuth2, JWT |
| 네트워크 | 한국투자증권 API, Retrofit, OkHttp, WebSocket, OpenAPI 3 |
| 데이터 | PostgreSQL 14, TimescaleDB, Redis 7.2, Firebase Storage |
| AI | Flask, OpenAI, PyTorch, Transformers, ko-FinBERT, Pandas |
| 인프라 | Docker, Docker Compose, AWS EC2, Ubuntu, Spring Actuator |
| 협업 | GitLab, Jira, Notion, Mattermost |

## 패키지 구조

### Android

```text
com.lago.app/
├── data/
│   ├── cache/                 # 차트, 관심종목, 실시간 데이터
│   ├── local/                 # Room DAO, Entity, SharedPreferences
│   ├── remote/                # API, DTO, WebSocket
│   ├── repository/
│   ├── scheduler/
│   └── service/
├── di/
├── domain/                    # entity, repository, usecase
├── fcm/
└── presentation/
    ├── ui/                    # chart, home, login, news, study 등
    └── viewmodel/
```

### 백엔드

```text
BE/src/main/java/com/example/LAGO/
├── LagoApplication.java
├── ai/                        # FinBERT 감정분석, 매매 전략
├── config/                    # Security, WebSocket, Redis, KIS
├── controller/
├── domain/
├── dto/
├── repository/
├── realtime/                  # KIS WebSocket, Redis Stream, TimescaleDB
├── service/                   # 자동매매봇, 기술적분석, 계좌
├── kis/
├── scheduler/                 # 데일리 퀴즈, 뉴스 수집, 자산 업데이트
├── utils/
├── exception/
├── converter/
└── constants/
```

아키텍처 그림은 [images/LAGO_architecture.png](images/LAGO_architecture.png)에 있다.

![시스템 아키텍처](images/LAGO_architecture.png)

## 산출물

- 포팅 매뉴얼은 `exec` 폴더에 있다.
- [기능 명세서](https://www.notion.so/237085cabd3480fea27ce09b3c59625c?pvs=21)
- [와이어프레임](https://www.figma.com/design/utXuqPzMOWMkrvwPFUTlTF/%EB%9D%BC%EA%B3%A0%ED%95%A0%EB%95%8C-%EC%99%80%EC%9D%B4%EC%96%B4%ED%94%84%EB%A0%88%EC%9E%84?node-id=0-1&t=ge4MRh3gd1kZeJVe-1)
- [API 명세서](https://www.notion.so/API-22a085cabd348039bc81edc6ec3eeeec?pvs=21)
- [ERD](https://dbdiagram.io/d/689d9e061d75ee360a90baed)
- [시퀀스 다이어그램](https://www.notion.so/22a085cabd3480678fd8cd9a8e29707a?pvs=21)
- [간트 차트](https://docs.google.com/spreadsheets/d/1wQ9IBqwJeYietFdrB4bGjVusZWbprBePGl_xuRKNgzI/edit?usp=sharing)

## 팀

| 김수진 | 김해민 | 박경찬 | 박승균 | 박준형 | 최혜림 |
| --- | --- | --- | --- | --- | --- |
| <img src="images/kimsujin.jpg" width="120"/> | <img src="이미지URL" width="120"/> | <img src="images/parkgyungchan.jpg" width="120"/> | <img src="![image.png](attachment:8f5857f5-07fd-4c86-9e3d-5b16c406b554:image.png)" width="120"/> | <img src="이미지URL" width="120"/> | <img src="이미지URL" width="120"/> |
| 팀장, PM. 중간/최종 발표, PPT, UI/UX, 기능 명세서, 간트 차트. Android는 마이페이지, 포트폴리오, 랭킹, 뉴스. BE는 계좌 조회, 관심 뉴스. | 캐릭터 디자인. BE는 실시간 주가, 차트 조회, 관심 종목, Entity. | 기획, PL, 시퀀스 다이어그램. Android는 투자 메인, 모의투자, 역사적 챌린지. BE는 주식 거래, 실시간 뉴스. | Android는 메인, 학습, 포트폴리오, 랭킹, FCM. BE는 단어장, 퀴즈, 소셜 로그인. AI는 뉴스 감정 분석. | 서버 환경, 서버 최적화, DB 설계, 성향별 매매봇 API. | 문서, API 설계와 명세서. BE는 차트 패턴 분석, 역사적 챌린지. |
