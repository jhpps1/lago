package com.lago.app.data.repository

import com.lago.app.data.remote.api.AuthApiService
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
            val response = authApiService.googleLogin(GoogleLoginRequest(accessToken))
            if (response.isSuccessful) {
                response.body()?.let {
                    Resource.Success(it)
                } ?: Resource.Error("응답이 비어있습니다.")
            } else {
                Resource.Error("Google 로그인 실패: ${response.message()}")
            }
        } catch (e: Exception) {
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
}