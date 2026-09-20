package com.gabinkenko.tikapub.tiktok

import com.gabinkenko.tikapub.tiktok.model.CreatorInfoResponse
import com.gabinkenko.tikapub.tiktok.model.InitVideoRequest
import com.gabinkenko.tikapub.tiktok.model.InitVideoResponse
import com.gabinkenko.tikapub.tiktok.model.PublishStatusRequest
import com.gabinkenko.tikapub.tiktok.model.PublishStatusResponse
import com.gabinkenko.tikapub.tiktok.model.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

/** Thin wrapper over the TikTok for Developers OAuth + Content Posting API (v2). */
interface TikTokApiService {

    @FormUrlEncoded
    @POST("v2/oauth/token/")
    suspend fun exchangeCodeForToken(
        @Field("client_key") clientKey: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String,
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("redirect_uri") redirectUri: String,
        @Field("code_verifier") codeVerifier: String,
    ): Response<TokenResponse>

    @FormUrlEncoded
    @POST("v2/oauth/token/")
    suspend fun refreshToken(
        @Field("client_key") clientKey: String,
        @Field("client_secret") clientSecret: String,
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String,
    ): Response<TokenResponse>

    @POST("v2/post/publish/creator_info/query/")
    suspend fun queryCreatorInfo(
        @Header("Authorization") bearerToken: String,
    ): Response<CreatorInfoResponse>

    @POST("v2/post/publish/video/init/")
    suspend fun initVideoUpload(
        @Header("Authorization") bearerToken: String,
        @Body request: InitVideoRequest,
    ): Response<InitVideoResponse>

    @POST("v2/post/publish/status/fetch/")
    suspend fun fetchPublishStatus(
        @Header("Authorization") bearerToken: String,
        @Body request: PublishStatusRequest,
    ): Response<PublishStatusResponse>
}
