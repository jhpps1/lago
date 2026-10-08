@echo off
REM Set NAVER_CLIENT_ID and NAVER_CLIENT_SECRET in the environment before running.
REM Do not put the values in this file.
if "%NAVER_CLIENT_ID%"=="" echo NAVER_CLIENT_ID is not set
if "%NAVER_CLIENT_SECRET%"=="" echo NAVER_CLIENT_SECRET is not set
set SPRING_PROFILES_ACTIVE=dev
echo SPRING_PROFILES_ACTIVE=%SPRING_PROFILES_ACTIVE%
gradlew.bat bootRun --args='--spring.profiles.active=dev'
