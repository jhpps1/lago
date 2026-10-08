package com.lago.app.data.repository

import android.util.Log
import com.lago.app.data.remote.api.AuthApiService
import com.lago.app.data.remote.dto.CompleteSignupRequest
import com.lago.app.data.remote.dto.CompleteSignupResponse
import com.lago.app.data.remote.dto.GoogleLoginRequest
import com.lago.app.data.remote.dto.GoogleLoginResponse
import com.lago.app.domain.repository.AuthRepository
import com.lago.app.util.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService
) : AuthRepository {

    override suspend fun googleLogin(accessToken: String): Resource<GoogleLoginResponse> {
        return try {
            Log.d("AuthRepository", "Google 로그인 요청 시작")
            Log.d("AuthRepository", "AccessToken 길이: ${accessToken.length}")
            Log.d("AuthRepository", "AccessToken 앞 50자: ${accessToken.take(50)}")
            
            val request = GoogleLoginRequest(accessToken)
            Log.d("AuthRepository", "요청 객체 생성: $request")
            
            val response = authApiService.googleLogin(request)
            Log.d("AuthRepository", "API 응답 수신 - 성공 여부: ${response.isSuccessful}")
            Log.d("AuthRepository", "응답 코드: ${response.code()}")
            Log.d("AuthRepository", "응답 메시지: ${response.message()}")
            
            if (response.isSuccessful) {
                val body = response.body()
                Log.d("AuthRepository", "응답 본문: $body")
                body?.let {
                    Log.d("AuthRepository", "Google 로그인 성공")
                    Resource.Success(it)
                } ?: Resource.Error("응답이 비어있습니다.")
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("AuthRepository", "Google 로그인 실패 - 에러 본문: $errorBody")
                Resource.Error("Google 로그인 실패: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Google 로그인 예외 발생: ${e.message}", e)
            Resource.Error("네트워크 오류: ${e.message}")
        }
    }

    override suspend fun kakaoLogin(accessToken: String): Resource<GoogleLoginResponse> {
        return try {
            val response = authApiService.kakaoLogin(GoogleLoginRequest(accessToken))
            if (response.isSuccessful) {
                response.body()?.let {
                    Resource.Success(it)
                } ?: Resource.Error("응답이 비어있습니다.")
            } else {
                Resource.Error("Kakao 로그인 실패: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("네트워크 오류: ${e.message}")
        }
    }

    override suspend fun completeSignup(tempToken: String, nickname: String, personality: String): Resource<CompleteSignupResponse> {
        return try {
            Log.d("AuthRepository", "회원가입 완료 요청 시작")
            Log.d("AuthRepository", "TempToken: $tempToken")
            Log.d("AuthRepository", "Nickname: $nickname")
            Log.d("AuthRepository", "Personality: $personality")
            
            val request = CompleteSignupRequest(tempToken, nickname, personality)
            Log.d("AuthRepository", "회원가입 요청 객체: $request")
            
            Log.d("AuthRepository", "API 호출 시작: /api/auth/complete-signup")
            val response = authApiService.completeSignup(request)
            
            Log.d("AuthRepository", "회원가입 API 응답 수신 - 성공 여부: ${response.isSuccessful}")
            Log.d("AuthRepository", "응답 코드: ${response.code()}")
            Log.d("AuthRepository", "응답 메시지: ${response.message()}")
            Log.d("AuthRepository", "응답 헤더: ${response.headers()}")
            
            if (response.isSuccessful) {
                val body = response.body()
                Log.d("AuthRepository", "회원가입 응답 본문: $body")
                body?.let {
                    Log.d("AuthRepository", "회원가입 완료 성공")
                    Resource.Success(it)
                } ?: Resource.Error("응답이 비어있습니다.")
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("AuthRepository", "회원가입 실패 - 에러 본문: $errorBody")
                Log.e("AuthRepository", "회원가입 실패 - 응답 원시 데이터: ${response.raw()}")
                Resource.Error("회원가입 완료 실패: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "회원가입 완료 예외 발생: ${e.message}", e)
            Log.e("AuthRepository", "예외 스택 트레이스: ", e)
            Resource.Error("네트워크 오류: ${e.message}")
        }
    }
}