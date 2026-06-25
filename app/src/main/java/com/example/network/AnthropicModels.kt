package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MessageDto(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class MessagesRequest(
    val model: String,
    @Json(name = "max_tokens") val maxTokens: Int,
    val messages: List<MessageDto>,
    val system: String? = null
)

@JsonClass(generateAdapter = true)
data class ContentBlockDto(
    val type: String,
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class UsageDto(
    @Json(name = "input_tokens") val inputTokens: Int,
    @Json(name = "output_tokens") val outputTokens: Int
)

@JsonClass(generateAdapter = true)
data class MessagesResponse(
    val id: String,
    val role: String,
    val content: List<ContentBlockDto>,
    val model: String,
    val usage: UsageDto
)
