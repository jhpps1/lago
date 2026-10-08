# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

LAGO는 주식 교육 및 모의투자 Android 앱입니다. 

사용자에게 차트 패턴 학습, 퀴즈, 모의투자, AI 매매봇, 뉴스 등의 기능을 제공하며, 커스터마이징된 TradingView 차트 라이브러리를 통합한 고급 차트 분석 시스템을 제공합니다.

## Development Commands

### Build Commands
```bash
# 전체 프로젝트 빌드
./gradlew build

# 디버그 APK 빌드
./gradlew assembleDebug

# 릴리즈 APK 빌드
./gradlew assembleRelease

# 클린 빌드
./gradlew clean build
```

### Testing Commands
```bash
# 모든 단위 테스트 실행
./gradlew test

# 계측 테스트 실행
./gradlew connectedAndroidTest

# 특정 테스트 클래스 실행
./gradlew testDebugUnitTest --tests "com.example.ExampleUnitTest"

# Lint 분석 실행
./gradlew lint
```

### Chart Library Commands
```bash
# 차트 라이브러리 npm 의존성 설치
cd lightweightlibrary && npm install

# 차트 라이브러리 수동 빌드
cd lightweightlibrary && npm run compile

# 모든 차트 컴포넌트 빌드
cd lightweightlibrary && npm run compile-price-formatter && npm run compile-time-formatter && npm run compile-eval-plugin
```

## Architecture Overview

### Clean Architecture 구조

**Presentation Layer** (`presentation/`)
- Jetpack Compose로 구축된 UI 화면들
- StateFlow/LiveData로 UI 상태를 관리하는 ViewModel들
- Navigation Compose를 사용한 네비게이션
- `components/`, `widget/`의 재사용 가능한 UI 컴포넌트들

**Domain Layer** (`domain/`)
- 핵심 비즈니스 엔티티 정의
- 데이터 접근을 위한 Repository 인터페이스
- 비즈니스 로직을 캡슐화하는 UseCase들
- Android 프레임워크 의존성 없음

**Data Layer** (`data/`)
- 로컬/원격 데이터 소스를 조정하는 Repository 구현체들
- Retrofit을 사용한 API 통신을 위한 Remote Data Source
- Room 데이터베이스를 사용한 Local Data Source
- 데이터 변환을 위한 DTO와 Mapper들

### 핵심 컴포넌트들

**Dependency Injection** (`di/`)
- `NetworkModule`: Retrofit, OkHttp, API 서비스 설정 (30초 타임아웃)
- `DatabaseModule`: Room 데이터베이스와 DAO 제공
- `RepositoryModule`: Repository 구현체 바인딩
- `LocalDataModule`: SharedPreferences와 로컬 저장소

**Chart Integration** (`lightweightlibrary`)
- TradingView Lightweight Charts를 로컬에서 커스터마이징
- 매수/매도 신호 표시 기능 (사용자: 화살표, AI: 캐릭터 아이콘)
- 볼린저밴드, 거래량, 보조지표 커스터마이징 완료
- 멀티 패널 차트 지원
- 실시간 WebSocket 연결 (`symbol + timeframe` 구독)
- 한국 장 시간에 맞춘 차트 시간선
- 패턴 분석을 위한 현재 보고있는 구간 정보 제공

**Real-time Data Integration**
- WebSocket을 통한 실시간 차트 데이터 수신
- `{ symbol, timeframe, o, h, l, c, v, timestamp }` 형태 JSON
- 실시간 수익률 업데이트
- TimeScaleDB 연동 계획

## Key Features

### Chart System (진행 중)
- 커스텀 매수/매도 신호 표시
- 실시간 차트 업데이트
- 볼린저밴드 및 보조지표 표시
- 반응형/적응형 UI 구현 (다양한 기기 대응)
- 차트 설정 화면 개선 완료
- 패턴 분석 및 드로잉 기능 계획

### UI/UX Improvements (일부 완료)
- 바텀시트와 앱바 높이에 따른 동적 UI 조정
- 모의투자 리스트 화면 검색 및 정렬 기능 개선
- 반응형 레이아웃 구현
- 상단 앱바 수익률 표시 개선

### Investment Features
- 실시간 주식 정보 조회 및 검색
- 매수/매도 거래 시뮬레이션
- 포트폴리오 및 거래 내역 관리
- 관심 종목 관리
- AI 매매봇 개인화

### Learning System
- 차트 패턴 학습 (패턴 분석 UI 개선 중)
- 데일리/랜덤 퀴즈
- 투자 용어 단어장
- 역사 챌린지 (새 화면 구현 필요)

### News System
- 실시간 뉴스 조회
- 관심 종목별 뉴스 필터링
- 뉴스 상세 조회

## API Configuration

### Base Settings
- Base URL: `https://api.lago.com/`
- 네트워크 타임아웃: 30초
- JWT 토큰 인증
- 개발 빌드 HTTP 로깅 활성화

### API Categories
- **인증**: 로그인, 회원가입, 토큰 관리
- **회원 관리**: 프로필, 랭킹, 포트폴리오
- **모의투자**: 주식 정보, 매매, 관심 종목
- **AI 매매봇**: 매매봇 관리 및 개인화
- **학습**: 차트 패턴, 퀴즈, 용어사전
- **뉴스**: 실시간/관심종목 뉴스
- **기타**: 역사 챌린지, 프레임/테두리 샵

## Development Guidelines

### Code Organization
- Clean Architecture 레이어 분리
- 기능별 패키지 구조
- 공통 유틸리티는 `util/` 패키지
- UI 컴포넌트 재사용성 고려

### Key Libraries & Technologies
- **UI**: Jetpack Compose, Material3, Navigation Compose
- **DI**: Dagger Hilt (@AndroidEntryPoint, @HiltAndroidApp)
- **Database**: Room with coroutines, type converters
- **Network**: Retrofit, Gson, OkHttp (30초 타임아웃)
- **Charts**: 커스터마이징된 TradingView Lightweight Charts
- **Real-time**: WebSocket 연결
- **Async**: Kotlin Coroutines, ViewModel lifecycle scope

### Chart Development Notes
- `lightweightlibrary` 모듈 로컬 커스터마이징
- npm 빌드 자동화 (preBuild 태스크)
- JavaScript-Kotlin 브릿지 통신
- 매수/매도 신호 표시 커스터마이징
- 한국 주식 시장 시간대 적용
- 실시간 데이터 WebSocket 연동

### UI/UX Development
- 다양한 기기 크기 대응 (반응형/적응형)
- 바텀시트 및 네비게이션 바 높이 동적 조정
- 다크모드 비활성화 (MainActivity 설정)
- 특정 화면에서 하단 네비게이션 숨김 처리

### Current Development Status
**완료된 기능:**
- 볼린저밴드 메인 차트 표시
- 거래량/보조지표 색상 변경
- 앱바 높이 조정
- 모의투자 리스트 UI 개선
- 차트 설정 화면 개선

**진행 중인 작업:**
- 차트 매수/매도 신호 표시 기능
- 실시간 수익률 업데이트
- 반응형 UI 구현
- 바텀시트 위치 조정
- 패턴 분석 UI 개선

**계획된 기능:**
- 역사 챌린지 화면 구현
- 차트 드로잉 기능
- TimeScaleDB 연동
- 실시간 WebSocket 데이터 연동