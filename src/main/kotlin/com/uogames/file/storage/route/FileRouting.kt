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
import io.ktor.util.date.*
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

        call.response.header(HttpHeaders.LastModified, GMTDate(file.second.createdAd).toHttpDate())
        call.response.header(HttpHeaders.ETag, file.second.fileName)

        val isNoneMatch = call.request.header(HttpHeaders.IfNoneMatch) == filename.toHexString()
        val isModifiedSince = call.request.header(HttpHeaders.IfModifiedSince) == GMTDate(file.second.createdAd).toHttpDate()

        if (isNoneMatch || isModifiedSince) {
            return@get call.respond(HttpStatusCode.NotModified)
        } else {
            call.respondBytes(file.first.readBytes(), ContentType.parse(file.second.contentType)) {
                this.caching = CachingOptions(
                    CacheControl.MaxAge(
                        maxAgeSeconds = Int.MAX_VALUE,
                        mustRevalidate = false,
                        visibility = CacheControl.Visibility.Public
                    )
                )
            }
        }
    }

    get("/file/info/{file_name}") {
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

        get("/storage/info") {
            call.respond(filesService.storageState())
        }

        delete("/file/{file_name}") {
            val filename = call.receiveUuidOrError("file_name") { return@delete }
            filesService.delete(filename.toHexString())
            call.respond(HttpStatusCode.OK)
        }
    }


}