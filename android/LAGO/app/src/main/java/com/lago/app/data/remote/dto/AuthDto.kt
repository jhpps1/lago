package com.lago.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GoogleLoginRequest(
    @SerializedName("accessToken")
    val accessToken: String
)

data class GoogleLoginResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: LoginData?
)

data class LoginData(
    @SerializedName("status")
    val status: String?, // "EXISTING_USER" or "NEW_USER"
    @SerializedName("user")
    val user: UserDto?,
    @SerializedName("accessToken")
    val accessToken: String?,
    @SerializedName("refreshToken")
    val refreshToken: String?,
    @SerializedName("tempToken")
    val tempToken: String?,
    @SerializedName("suggestedNickname")
    val suggestedNickname: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("needsSignup")
    val needsSignup: Boolean?
)

data class UserDto(
    @SerializedName("userId")
    val id: Long,
    @SerializedName("email")
    val email: String,
    @SerializedName("nickname")
    val nickname: String?,
    @SerializedName("provider")
    val provider: String,
    @SerializedName("personality")
    val personality: String?
)

data class CompleteSignupRequest(
    @SerializedName("tempToken")
    val tempToken: String,
    @SerializedName("nickname")
    val nickname: String,
    @SerializedName("personality")
    val personality: String
)

data class CompleteSignupResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("expiresIn")
    val expiresIn: String?,
    @SerializedName("accessToken")
    val accessToken: String?,
    @SerializedName("tokenType")
    val tokenType: String?,
    @SerializedName("user")
    val user: UserDto?,
    @SerializedName("refreshToken")
    val refreshToken: String?
)