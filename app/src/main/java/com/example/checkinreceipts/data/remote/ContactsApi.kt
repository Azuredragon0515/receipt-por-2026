package com.example.checkinreceipts.data.remote
import com.example.checkinreceipts.data.remote.dto.ContactDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ContactsApi {
    @GET("users")
    suspend fun list(): List<ContactDto>
    @POST("users")
    suspend fun create(@Body body: ContactDto): ContactDto

    @DELETE("users/{id}")
    suspend fun delete(@Path("id") id: Long)
}