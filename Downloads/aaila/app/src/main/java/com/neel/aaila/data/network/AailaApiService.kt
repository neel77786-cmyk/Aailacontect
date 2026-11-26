package com.neel.aaila.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AailaApiService {

    @GET("api/health")
    suspend fun healthCheck(): Response<GenericResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("api/auth/me")
    suspend fun getMe(): Response<MeResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<GenericResponse>

    @GET("api/family/members")
    suspend fun getMembers(): Response<MembersResponse>

    @POST("api/family/members")
    suspend fun addMember(@Body request: AddMemberRequest): Response<GenericResponse>

    @DELETE("api/family/members/{uid}")
    suspend fun removeMember(@Path("uid") uid: String): Response<GenericResponse>

    @POST("api/livekit/token")
    suspend fun getLiveKitToken(@Body request: LiveKitTokenRequest): Response<LiveKitTokenResponse>
}
