package com.neel.aaila

import com.neel.aaila.data.network.AailaApiService
import com.neel.aaila.data.network.AuthResponse
import com.neel.aaila.data.network.GenericResponse
import com.neel.aaila.data.network.LiveKitTokenRequest
import com.neel.aaila.data.network.LiveKitTokenResponse
import com.neel.aaila.data.network.LoginRequest
import com.neel.aaila.data.network.MeResponse
import com.neel.aaila.data.network.MembersResponse
import com.neel.aaila.data.network.NetworkModule
import com.neel.aaila.data.network.RegisterRequest
import com.neel.aaila.data.network.AddMemberRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class BackendAuthUnitTest {

    @Test
    fun `resolveBaseUrl always ends with trailing slash`() {
        val url = NetworkModule.resolveBaseUrl()
        assertTrue(url.endsWith("/"))
        assertTrue(url.startsWith("https://"))
    }

    @Test
    fun `moshi parses AuthResponse with userId and token`() {
        val moshi = NetworkModule.createMoshi()
        val adapter = moshi.adapter(AuthResponse::class.java)
        val json = """
            {
              "success": true,
              "userId": "fb_uid_999",
              "familyId": "HASSAN-101",
              "familyName": "Hassan Family",
              "adminName": "Neel",
              "role": "ADMIN",
              "token": "jwt.token.here",
              "expiresIn": "24h"
            }
        """.trimIndent()
        val parsed = adapter.fromJson(json)
        assertEquals(true, parsed?.success)
        assertEquals("fb_uid_999", parsed?.userId)
        assertEquals("HASSAN-101", parsed?.familyId)
        assertEquals("jwt.token.here", parsed?.token)
    }

    @Test
    fun `moshi parses LiveKitTokenResponse`() {
        val moshi = NetworkModule.createMoshi()
        val adapter = moshi.adapter(LiveKitTokenResponse::class.java)
        val json = """
            {
              "success": true,
              "token": "lk_jwt_token",
              "url": "wss://aaila.livekit.cloud",
              "room": "HASSAN-101_SundayCall"
            }
        """.trimIndent()
        val parsed = adapter.fromJson(json)
        assertEquals(true, parsed?.success)
        assertEquals("lk_jwt_token", parsed?.token)
        assertEquals("wss://aaila.livekit.cloud", parsed?.url)
        assertEquals("HASSAN-101_SundayCall", parsed?.room)
    }
}
