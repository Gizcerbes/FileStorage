package com.uogames.file.storage.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BaseModel<T : DataResponse>(
    @SerialName("error") val error: ErrorModel? = null,
    @SerialName("data") val data: T? = null
)
