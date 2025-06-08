package com.uogames.file.storage.util

import kotlinx.serialization.json.Json

object JsonExt {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }
}

