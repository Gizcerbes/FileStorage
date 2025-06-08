package com.uogames.file.storage.util

import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

object UuidExt {


    @OptIn(ExperimentalUuidApi::class)
    fun Uuid.Companion.randomV7(): Uuid {
        val value = random().toByteArray()
        val timestamp = System.currentTimeMillis()
        value[6] = (value[6].toInt() and 0x0F or 0x70).toByte()
        value[8] = (value[8].toInt() and 0x3F or 0x80).toByte()
        repeat(6) { value[it] = ((timestamp shr (40 - 8 * it)) and 0xFF).toByte() }
        return fromByteArray(value)
    }

    @OptIn(ExperimentalUuidApi::class)
    fun uuidV7() : UUID = Uuid.randomV7().toJavaUuid()


}