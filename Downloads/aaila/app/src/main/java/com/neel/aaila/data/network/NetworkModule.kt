package com.neel.aaila.data.network

import com.neel.aaila.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private const val DEFAULT_BACKEND_URL = "https://aaila-backend.vercel.app"

    fun resolveBaseUrl(): String {
        val configured = try {
            val field = BuildConfig::class.java.getField("AAILA_BACKEND_URL")
            (field.get(null) as? String)?.trim()
        } catch (_: Throwable) {
            null
        }
        val base = if (!configured.isNullOrBlank()) configured else DEFAULT_BACKEND_URL
        return if (base.endsWith("/")) base else "$base/"
    }

    fun createMoshi(): Moshi {
        return Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    fun createOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    fun createApiService(
        okHttpClient: OkHttpClient,
        moshi: Moshi,
        baseUrl: String = resolveBaseUrl()
    ): AailaApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AailaApiService::class.java)
    }
}
