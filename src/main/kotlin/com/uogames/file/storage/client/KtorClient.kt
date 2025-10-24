package com.uogames.file.storage.client

import com.uogames.file.storage.util.JsonExt
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.auth.AuthScheme
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class KtorClient(
    private val existsRequest: String,
    private val allowRequest: String,
    private val clientToken: String,
) {


    private val checkClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(json = JsonExt.json) }
        defaultRequest {
            header(HttpHeaders.Authorization, "${AuthScheme.Bearer} $clientToken")
        }
    }

    private val allowClient = HttpClient(CIO){
        install(ContentNegotiation) { json(json = JsonExt.json) }
    }

    suspend fun checkExists(filename: String): HttpStatusCode = checkClient.post(existsRequest) {
        contentType(ContentType.Application.Json)
        setBody(buildJsonObject {
            put("file_name", filename)
        })
    }.status


    suspend fun allowRequest(token: String): HttpStatusCode = allowClient.post(allowRequest) {
        header(HttpHeaders.Authorization, token)
    }.status


}