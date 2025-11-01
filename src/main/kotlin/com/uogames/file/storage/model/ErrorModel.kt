package com.uogames.file.storage.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorModel(
    @SerialName("message") val message: String,
    @SerialName("detail") val detail: String = ""
)
