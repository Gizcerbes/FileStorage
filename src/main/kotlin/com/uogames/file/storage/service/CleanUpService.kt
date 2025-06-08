package com.uogames.file.storage.service

import com.uogames.file.storage.client.KtorClient
import io.ktor.http.*
import kotlinx.coroutines.*

class CleanUpService(
    fileService: FileService,
    ktorClient: KtorClient,
    repeatTime: Long,
    oldMils: Long,
) {

    private val workScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    init {

        workScope.launch {

            while (true) {
                delay(repeatTime)
                val filesForDelete = fileService.findOlderThen(System.currentTimeMillis() - oldMils)
                val data = filesForDelete.groupBy { filename ->
                    runCatching { ktorClient.checkExists(filename) }.getOrNull()
                }
                data[HttpStatusCode.OK]?.let { fileService.resetLastRequest(it) }
                data[HttpStatusCode.NotFound]?.forEach { fileService.delete(it) }
            }
        }
    }


}