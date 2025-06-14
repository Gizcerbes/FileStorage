package com.uogames.file.storage.route

import com.uogames.file.storage.model.AccessType
import com.uogames.file.storage.model.FileDataDTO
import com.uogames.file.storage.service.FileService
import com.uogames.file.storage.util.CoreExt.receiveUuidOrError
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun Route.files(
    filesService: FileService,
    fileSizeLimit: Long,
) {


    get("/file/{file_name}") {
        val filename = call.receiveUuidOrError("file_name") { return@get }
        val file = requireNotNull(filesService.getFile(filename.toHexString())) { return@get }
        val fileInfo = requireNotNull(filesService.fileInfo(filename.toHexString())) { return@get }

        call.respondBytes(file.readBytes(), ContentType.parse(fileInfo.contentType)) {
            this.caching = CachingOptions(
                CacheControl.MaxAge(
                    maxAgeSeconds = Int.MAX_VALUE,
                    mustRevalidate = false,
                    visibility = CacheControl.Visibility.Public
                )
            )
        }
    }

    get("/info/{file_name}") {
        val filename = call.receiveUuidOrError("file_name") { return@get }
        val data = requireNotNull(filesService.fileInfo(filename.toHexString())) { return@get }
        call.respond(data)
    }


    authenticate("api-key") {
        post("/file/upload") {
            val filenameList = ArrayList<String>()
            call.receiveMultipart(fileSizeLimit).forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        val contentType = part.contentType ?: ContentType.Any
                        val fileBytes = part.provider().toByteArray()
                        val filename =
                            filesService.save(fileBytes, AccessType.PUBLIC, contentType = contentType.toString())
                        filenameList.add(filename)
                    }

                    else -> {}
                }
                part.dispose()
            }
            call.respond(FileDataDTO(nameList = filenameList))
        }



        delete("/file/{file_name}") {
            val filename = call.receiveUuidOrError("file_name") { return@delete }
            filesService.delete(filename.toHexString())
            call.respond(HttpStatusCode.OK)
        }
    }


}