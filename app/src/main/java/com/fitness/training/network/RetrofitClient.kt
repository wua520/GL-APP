package com.fitness.training.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    
    // TODO: 上线前需要配置正式服务器地址
    // 开发环境使用局域网IP，生产环境需要使用域名
    private const val BASE_URL = "https://api.yourapp.com/" // 替换为你的域名
    
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        android.util.Log.d("OkHttp", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val request = chain.request()
            android.util.Log.d("RetrofitClient", "发起请求: ${request.method} ${request.url}")
            try {
                val response = chain.proceed(request)
                android.util.Log.d("RetrofitClient", "响应: ${response.code}")
                response
            } catch (e: Exception) {
                android.util.Log.e("RetrofitClient", "请求失败: ${e.javaClass.simpleName}: ${e.message}")
                throw e
            }
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
