package com.example.data.remote

import com.example.data.remote.dto.AuthResponse
import com.example.data.remote.dto.EventDto
import com.example.data.remote.dto.GoogleAuthRequest
import com.example.data.remote.dto.SendEmailReminderRequest
import com.example.data.remote.dto.SendEmailResponse
import com.example.data.remote.dto.SyncRequest
import com.example.data.remote.dto.SyncResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface RemindMeApiService {
    @POST("auth/google")
    suspend fun authenticateGoogle(@Body request: GoogleAuthRequest): Response<AuthResponse>

    @GET("events")
    suspend fun getEvents(): Response<List<EventDto>>

    @GET("events/{id}")
    suspend fun getEventById(@Path("id") id: String): Response<EventDto>

    @POST("events")
    suspend fun createEvent(@Body event: EventDto): Response<EventDto>

    @PUT("events/{id}")
    suspend fun updateEvent(@Path("id") id: String, @Body event: EventDto): Response<EventDto>

    @DELETE("events/{id}")
    suspend fun deleteEvent(@Path("id") id: String): Response<Unit>

    @POST("events/sync")
    suspend fun syncEvents(@Body syncRequest: SyncRequest): Response<SyncResponse>

    @POST("reminders/send-email")
    suspend fun sendEmailReminder(@Body request: SendEmailReminderRequest): Response<SendEmailResponse>
}
