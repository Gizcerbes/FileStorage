package com.uogames.file.storage.service

import com.uogames.file.storage.db.Database
import com.uogames.file.storage.model.AccessType
import com.uogames.file.storage.model.FileInfoDTO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class FileService(
    private val folder: String
) {

    private val catalog = Database.FileCatalog


    suspend fun save(
        byteArray: ByteArray,
        accessType: AccessType,
        contentType: String,
    ): String {
        val filename = suspendTransaction {
            catalog.insertAndGetId {
                it[catalog.size] = byteArray.size
                it[catalog.accessType] = accessType
                it[catalog.contentType] = contentType
            }.value.toKotlinUuid().toHexString()
        }

        val wFolder = File(folder).apply { mkdirs() }
        File(wFolder, filename).writeBytes(byteArray)

        return filename

    }

    suspend fun exists(filename: String): Boolean = File(folder, filename).exists()

    suspend fun getFile(filename: String): Pair<File, FileInfoDTO>? {
        val id = Uuid.parseHex(filename).toJavaUuid()
        val file = File(folder, filename)
        if (!file.exists()) return null
        val data = suspendTransaction {
            val data = catalog.selectAll()
                .where { catalog.id eq id }
                .limit(1)
                .first()
            data
        }
        return file to FileInfoDTO(
            fileName = filename,
            size = data[catalog.size],
            createdAd = data[catalog.createdAt],
            lastRequest = System.currentTimeMillis(),
            accessType = data[catalog.accessType],
            exists = true,
            contentType = data[catalog.contentType],
            requests = data[catalog.requests] + 1
        )
    }

    suspend fun addRead(filename: String) = suspendTransaction {
        val id = Uuid.parseHex(filename).toJavaUuid()
        catalog.update(
            where = { catalog.id eq id }
        ) {
            it[lastRequest] = System.currentTimeMillis()
            it[requests] = catalog.requests.plus(1)
        }
    }

    suspend fun delete(filename: String): Boolean {
        val id = Uuid.parseHex(filename).toJavaUuid()
        val file = File(folder, filename)
        val result = file.delete()
        suspendTransaction { catalog.deleteWhere { catalog.id eq id } }
        return result
    }

    suspend fun fileInfo(filename: String): FileInfoDTO? {
        val id = Uuid.parseHex(filename).toJavaUuid()
        val exists = File(folder, filename).exists()
        return suspendTransaction {
            catalog.selectAll()
                .where { catalog.id eq id }
                .limit(1)
                .firstOrNull()
                ?.let {
                    FileInfoDTO(
                        fileName = filename,
                        size = it[catalog.size],
                        createdAd = it[catalog.createdAt],
                        lastRequest = it[catalog.lastRequest],
                        accessType = it[catalog.accessType],
                        exists = exists,
                        contentType = it[catalog.contentType],
                        requests = it[catalog.requests]
                    )
                }
        }
    }

    suspend fun findOlderThen(time: Long): List<String> = suspendTransaction {
        catalog.selectAll()
            .where { catalog.lastRequest less time }
            .map { it[catalog.id].value.toKotlinUuid().toHexString() }
            .toList()
    }

    suspend fun resetLastRequest(ids: List<String>) = suspendTransaction {
        val uuidIDS = ids.map { Uuid.parseHex(it).toJavaUuid() }
        catalog.update(
            where = { catalog.id inList uuidIDS },
            body = { it[catalog.lastRequest] = System.currentTimeMillis() }
        )
    }

    suspend fun storageState(): Map<String, Long> {
        val folder = File(folder)
        val onControl = suspendTransaction {
            catalog.select(catalog.size.sum()).firstOrNull()?.get(catalog.size.sum()) ?: 0
        }
        return mapOf(
            "total_space" to folder.totalSpace,
            "free" to folder.freeSpace,
            "on_control" to onControl.toLong()
        )
    }


}