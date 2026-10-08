# 이 저장소에서 뺀 것

코드는 팀 프로젝트 전체다. 아래만 빼 두었다.

- `exec/빌드_배포_문서 (1).md`, `exec/외부_서비스_정보_문서 (1).md`에는 DB 비밀번호, OAuth 시크릿, 증권사 키가 본문으로 들어 있었다. 값은 여기 적지 않는다. 그 키는 재발급해야 한다.
- `BE/.claude/settings.local.json`에 네이버 클라이언트 아이디와 시크릿이 들어 있었다. 파일은 저장소에서 뺐다. `BE/start-with-dev-profile.bat`, `BE/start-with-env.bat`에도 같은 값이 있어서 비웠다. 네이버 키도 재발급해야 한다. 원본 팀 저장소 이력에 같은 값이 남아 있을 수 있다.
- `.env`는 커밋하지 않는다. 키 이름은 `.env.example`만 본다.
- `android/**/build`, `.gradle`, `.class` 같은 빌드 산출물은 소스가 아니고, Windows에서 경로가 길어 clone이 깨진다.
- `firebaseServiceAccountKey.json`은 `.gitignore`에 있다. Docker 빌드가 이 파일을 찾으므로, 저장소 루트에 빈 JSON `{}`를 직접 만들어야 한다. 실제 Firebase 키를 넣지 않는다.
- `android/LAGO/local.properties`는 개인 SDK 경로라 넣지 않았다.
- `SecurityConfig`는 개발 중 Swagger를 열려고 모든 요청을 허용한다. 운영 설정이 아니다.
