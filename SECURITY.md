# 뺀 파일

팀 코드는 전부 넣었다. 아래만 빠졌다.

- `exec/빌드_배포_문서 (1).md`, `exec/외부_서비스_정보_문서 (1).md`에 DB 비밀번호, OAuth, 증권사 키가 본문으로 있었다. 값은 여기 안 적는다.
- `BE/.claude/settings.local.json`에 네이버 클라이언트 아이디와 시크릿이 있었다. 파일은 뺐다. `BE/start-with-dev-profile.bat`, `BE/start-with-env.bat`에도 같은 값이 있어서 비웠다.
- `.env`는 안 올렸다. 키 이름은 `.env.example`만 본다.
- `android/**/build`, `.gradle`, `.class`는 소스가 아니다. Windows에서 경로가 길어서 clone이 깨진다.
- `firebaseServiceAccountKey.json`은 gitignore다. Docker 빌드가 이 파일을 찾으니, 루트에 빈 JSON `{}`를 직접 두면 된다. 실제 Firebase 키는 넣지 않는다.
- `android/LAGO/local.properties`는 개인 SDK 경로라 뺐다.
- `SecurityConfig`는 Swagger 보려고 개발 중에 요청을 전부 열어 둔 상태다. 운영 설정은 아니다.
