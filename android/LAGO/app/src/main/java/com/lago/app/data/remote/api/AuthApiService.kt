package com.lago.app.data.remote.api

import com.lago.app.data.remote.dto.CompleteSignupRequest
import com.lago.app.data.remote.dto.CompleteSignupResponse
import com.lago.app.data.remote.dto.GoogleLoginRequest
import com.lago.app.data.remote.dto.GoogleLoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    
    @POST("login/google")
    suspend fun googleLogin(
        @Body request: GoogleLoginRequest
    ): Response<GoogleLoginResponse>
    
    @POST("login/kakao")
    suspend fun kakaoLogin(
        @Body request: GoogleLoginRequest // 카카오도 같은 구조
    ): Response<GoogleLoginResponse>
    
    @POST("api/auth/complete-signup")
    suspend fun completeSignup(
        @Body request: CompleteSignupRequest
    ): Response<CompleteSignupResponse>
}