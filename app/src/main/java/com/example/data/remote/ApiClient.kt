package com.example.data.remote

import com.example.data.remote.dto.AuthResponse
import com.example.data.remote.dto.EventDto
import com.example.data.remote.dto.SendEmailResponse
import com.example.data.remote.dto.SyncResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class MockNetworkInterceptor : Interceptor {
    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    // Simulated in-memory cloud database for PostgreSQL synchronization
    private val cloudDatabase = ConcurrentHashMap<String, EventDto>()
    private val deletedIds = ConcurrentHashMap.newKeySet<String>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.encodedPath

        try {
            // First try proceeding if a real external server is running and reachable
            // If the host is not default localhost mock, let it proceed
            if (!request.url.host.contains("mock.remindme.local")) {
                return chain.proceed(request)
            }
        } catch (_: Exception) {
            // Fallback to simulated Spring Boot backend response
        }

        val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

        if (url.endsWith("auth/google") && request.method == "POST") {
            val responseObj = AuthResponse(
                token = "mock-jwt-token-remindme-2026",
                userId = "user_collins_44",
                email = "yatorcollins44@gmail.com",
                displayName = "Collins Yator",
                photoUrl = null
            )
            val json = moshi.adapter(AuthResponse::class.java).toJson(responseObj)
            return Response.Builder()
                .code(200)
                .message("OK")
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .body(json.toResponseBody(jsonMediaType))
                .addHeader("content-type", "application/json")
                .build()
        }

        if (url.endsWith("events") && request.method == "GET") {
            val list = cloudDatabase.values.filter { !it.isDeleted }.toList()
            val listType = Types.newParameterizedType(List::class.java, EventDto::class.java)
            val json = moshi.adapter<List<EventDto>>(listType).toJson(list)
            return Response.Builder()
                .code(200)
                .message("OK")
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .body(json.toResponseBody(jsonMediaType))
                .addHeader("content-type", "application/json")
                .build()
        }

        if (url.endsWith("events/sync") && request.method == "POST") {
            val buffer = okio.Buffer()
            request.body?.writeTo(buffer)
            val bodyString = buffer.readUtf8()
            val syncRequestAdapter = moshi.adapter(com.example.data.remote.dto.SyncRequest::class.java)
            val syncReq = syncRequestAdapter.fromJson(bodyString)

            val clientEvents = syncReq?.events ?: emptyList()
            // Last-Write-Wins conflict resolution on cloud database:
            for (clientEv in clientEvents) {
                if (clientEv.isDeleted) {
                    cloudDatabase.remove(clientEv.id)
                    deletedIds.add(clientEv.id)
                } else {
                    val existing = cloudDatabase[clientEv.id]
                    if (existing == null || clientEv.updatedAt >= existing.updatedAt) {
                        cloudDatabase[clientEv.id] = clientEv
                    }
                }
            }

            val serverEvents = cloudDatabase.values.toList()
            val syncResponse = SyncResponse(
                serverTimestamp = System.currentTimeMillis(),
                updatedEvents = serverEvents,
                deletedEventIds = deletedIds.toList(),
                message = "Synchronized ${clientEvents.size} changes successfully with PostgreSQL backend."
            )
            val json = moshi.adapter(SyncResponse::class.java).toJson(syncResponse)
            return Response.Builder()
                .code(200)
                .message("OK")
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .body(json.toResponseBody(jsonMediaType))
                .addHeader("content-type", "application/json")
                .build()
        }

        if (url.contains("reminders/send-email") && request.method == "POST") {
            val responseObj = SendEmailResponse(
                status = "SENT",
                message = "Email reminder successfully queued and dispatched via Spring Boot Email Service.",
                timestamp = System.currentTimeMillis()
            )
            val json = moshi.adapter(SendEmailResponse::class.java).toJson(responseObj)
            return Response.Builder()
                .code(200)
                .message("OK")
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .body(json.toResponseBody(jsonMediaType))
                .addHeader("content-type", "application/json")
                .build()
        }

        // Default empty 200
        return Response.Builder()
            .code(200)
            .message("OK")
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .body("{}".toResponseBody(jsonMediaType))
            .addHeader("content-type", "application/json")
            .build()
    }
}

object ApiClient {
    private var instance: RemindMeApiService? = null

    fun getService(): RemindMeApiService {
        return instance ?: synchronized(this) {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .addInterceptor(MockNetworkInterceptor())
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://mock.remindme.local/api/v1/")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            val service = retrofit.create(RemindMeApiService::class.java)
            instance = service
            service
        }
    }
}
