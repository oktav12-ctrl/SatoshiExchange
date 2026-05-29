package com.example.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

data class RemoteTodo(
    val id: Int? = null,
    val userId: Int = 1,
    val title: String,
    val completed: Boolean
)

interface TodoPlaceholderApi {
    @GET("todos")
    suspend fun getTodos(
        @Query("_limit") limit: Int = 10
    ): List<RemoteTodo>

    @POST("todos")
    suspend fun createTodo(
        @Body todo: RemoteTodo
    ): RemoteTodo

    @PUT("todos/{id}")
    suspend fun updateTodo(
        @Path("id") id: Int,
        @Body todo: RemoteTodo
    ): RemoteTodo

    @DELETE("todos/{id}")
    suspend fun deleteTodo(
        @Path("id") id: Int
    ): Response<ResponseBody>

    companion object {
        private const val BASE_URL = "https://jsonplaceholder.typicode.com/"

        fun create(): TodoPlaceholderApi {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(TodoPlaceholderApi::class.java)
        }
    }
}
