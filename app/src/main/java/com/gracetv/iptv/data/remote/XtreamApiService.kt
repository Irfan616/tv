package com.gracetv.iptv.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

interface XtreamApiService {
    @GET
    suspend fun authenticate(
        @Url endpointUrl: String,
        @Query("username") username: String,
        @Query("password") password: String
    ): XtreamAuthResponse

    @GET
    suspend fun getLiveCategories(
        @Url endpointUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories"
    ): List<XtreamCategoryDto>

    @GET
    suspend fun getLiveStreams(
        @Url endpointUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams"
    ): List<XtreamLiveStreamDto>

    @GET
    suspend fun getVodCategories(
        @Url endpointUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_categories"
    ): List<XtreamCategoryDto>

    @GET
    suspend fun getVodStreams(
        @Url endpointUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_streams"
    ): List<XtreamVodStreamDto>

    @GET
    suspend fun getVodInfo(
        @Url endpointUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_info",
        @Query("vod_id") vodId: String
    ): XtreamVodInfoResponse

    @GET
    suspend fun getSeriesCategories(
        @Url endpointUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_categories"
    ): List<XtreamCategoryDto>

    @GET
    suspend fun getSeries(
        @Url endpointUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series"
    ): List<XtreamSeriesDto>

    @GET
    suspend fun getSeriesInfo(
        @Url endpointUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_info",
        @Query("series_id") seriesId: String
    ): XtreamSeriesInfoResponse

    @GET
    suspend fun downloadRaw(@Url url: String): ResponseBody
}

object NetworkClientFactory {
    fun createMoshi(): Moshi = Moshi.Builder()
        .add(ResilientStringAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun createOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "GraceTV/1.0 (Android IPTV Player)")
                .build()
            chain.proceed(request)
        }
        .build()

    fun createXtreamService(
        okHttpClient: OkHttpClient = createOkHttpClient(),
        moshi: Moshi = createMoshi()
    ): XtreamApiService {
        return Retrofit.Builder()
            .baseUrl("https://localhost/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(XtreamApiService::class.java)
    }
}
