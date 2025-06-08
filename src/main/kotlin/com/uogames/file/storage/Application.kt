package com.uogames.file.storage

import com.uogames.file.storage.client.KtorClient
import com.uogames.file.storage.db.Database
import com.uogames.file.storage.route.files
import com.uogames.file.storage.service.CleanUpService
import com.uogames.file.storage.service.FileService
import com.uogames.file.storage.util.JsonExt
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.routing.*
import java.time.LocalDateTime
import kotlin.system.measureTimeMillis

fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)


fun Application.module() {

    val adminToken = environment.config.property("ktor.admin_token").getString()
    val storageFolder = environment.config.property("ktor.storage_folder").getString()
    val fileSizeLimit = environment.config.property("ktor.file_size_limit").getString()
        .replace("_", "")
        .toLong()

    val clientToken = environment.config.property("ktor.clean_up.client_token").getString()
    val existsRequest = environment.config.property("ktor.clean_up.exists_request").getString()
    val repeatTime = environment.config.property("ktor.clean_up.repeat_time").getString()
        .replace("_", "")
        .toLong()
    val oldMils = environment.config.property("ktor.clean_up.old_mils").getString()
        .replace("_", "")
        .toLong()

    install(ContentNegotiation) { json(json = JsonExt.json) }
    install(CachingHeaders){
        options { _, outgoingContent ->
            when(outgoingContent.contentType?.withoutParameters()){
                ContentType.Application.OctetStream -> CachingOptions(CacheControl.MaxAge(maxAgeSeconds = Int.MAX_VALUE))
                else -> null
            }
        }
    }

    install(Authentication) {
        bearer("api-key") {
            authenticate {
                if (it.token == adminToken) Unit
                else null
            }
        }
    }

    intercept(ApplicationCallPipeline.Monitoring) {
        val processingTime = measureTimeMillis { proceed() }
        println("${LocalDateTime.now()}: Request ${call.request.httpMethod} ${call.request.uri} was running $processingTime mc")
    }


    Database.init(storageFolder)
    val fileService = FileService(storageFolder)
    val ktorClient = KtorClient(existsRequest,clientToken)
    val cleanUpService = CleanUpService(fileService,ktorClient, repeatTime, oldMils)

    routing {
        files(fileService, fileSizeLimit)
    }

}