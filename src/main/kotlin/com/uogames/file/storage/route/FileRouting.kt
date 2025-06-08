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
        call.respondFile(file) {

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
                        val fileBytes = part.provider().toByteArray()
                        val filename = filesService.save(fileBytes, AccessType.PUBLIC)
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