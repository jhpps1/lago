package com.lago.app.domain.repository

import com.lago.app.data.remote.dto.GoogleLoginResponse
import com.lago.app.util.Resource

interface AuthRepository {
    suspend fun googleLogin(accessToken: String): Resource<GoogleLoginResponse>
    suspend fun kakaoLogin(accessToken: String): Resource<GoogleLoginResponse>
}