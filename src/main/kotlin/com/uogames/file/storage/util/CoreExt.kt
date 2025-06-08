package com.uogames.file.storage.util

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object CoreExt {

    @OptIn(ExperimentalUuidApi::class)
    suspend inline fun RoutingCall.receiveUuidOrError(
        pathName: String,
        error: () -> Uuid
    ): Uuid{
        val name = pathParameters[pathName]?.let { runCatching { Uuid.parseHex(it) }.getOrNull() }
        return name ?: run {
            respond(HttpStatusCode.BadRequest)
            error()
        }
    }

    suspend inline fun <T> RoutingCall.requireNotNull(
        data: T,
        onNull: () -> T
    ): T {
        return data ?: run {
            respond(HttpStatusCode.NotFound)
            onNull()
        }
    }
}