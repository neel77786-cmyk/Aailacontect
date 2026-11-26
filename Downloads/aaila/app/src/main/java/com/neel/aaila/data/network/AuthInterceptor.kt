package com.neel.aaila.data.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the Bearer JWT token to outgoing requests and handles HTTP 401 responses
 * by clearing the local session so the user is prompted to re-authenticate.
 */
class AuthInterceptor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        // Public endpoints do not require an Authorization header
        val isPublicEndpoint = path.endsWith("/api/auth/login") ||
            path.endsWith("/api/auth/register") ||
            path.endsWith("/api/health")

        val token = tokenManager.getToken()
        val request = if (!isPublicEndpoint && !token.isNullOrBlank()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(request)

        if (response.code == 401 && !isPublicEndpoint) {
            tokenManager.notifyUnauthorized()
        }

        return response
    }
}
