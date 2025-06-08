package com.uogames.file.storage.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FileDataDTO(
    @SerialName("name_list") val nameList: List<String>
)