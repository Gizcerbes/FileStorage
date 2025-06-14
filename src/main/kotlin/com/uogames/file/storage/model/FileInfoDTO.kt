package com.uogames.file.storage.model

import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FileInfoDTO(
    @SerialName("file_name") val fileName: String = "",
    @SerialName("size") val size: Int = 0,
    @SerialName("created_at") val createdAd: Long = 0,
    @SerialName("last_request") val lastRequest: Long = 0,
    @SerialName("access_type") val accessType: AccessType = AccessType.PUBLIC,
    @SerialName("exists") val exists: Boolean = false,
    @SerialName("content_type") val contentType: String = ContentType.Any.toString()
)