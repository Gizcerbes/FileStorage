package com.uogames.file.storage.service

import com.uogames.file.storage.db.Database
import com.uogames.file.storage.model.AccessType
import com.uogames.file.storage.model.FileInfoDTO
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class FileService(
    private val folder: String,
) {

    private val catalog = Database.FileCatalog

    suspend fun save(
        byteArray: ByteArray,
        accessType: AccessType,
        contentType: String,
    ): String {
        val filename = transaction {
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
        val data = transaction {
            val data = catalog.selectAll()
                .where { catalog.id eq id }
                .limit(1)
                .first()

            catalog.update(
                where = { catalog.id eq id }
            ) {
                it[lastRequest] = System.currentTimeMillis()
                it[requests] = data[requests] + 1
            }
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

    suspend fun delete(filename: String) {
        val id = Uuid.parseHex(filename).toJavaUuid()
        val file = File(folder, filename)
        file.delete()
        transaction {
            catalog.deleteWhere { catalog.id eq id }
        }
    }

    suspend fun fileInfo(filename: String): FileInfoDTO? {
        val id = Uuid.parseHex(filename).toJavaUuid()
        val exists = File(folder, filename).exists()
        return transaction {
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

    suspend fun findOlderThen(time: Long): List<String> = transaction {
        catalog.selectAll()
            .where { catalog.lastRequest less time }
            .map { it[catalog.id].value.toKotlinUuid().toHexString() }
    }

    suspend fun resetLastRequest(ids: List<String>) = transaction {
        val uuidIDS = ids.map { Uuid.parseHex(it).toJavaUuid() }
        catalog.update(
            where = { catalog.id inList uuidIDS },
            body = { it[catalog.lastRequest] = System.currentTimeMillis() }
        )
    }

    suspend fun storageState(): Map<String, Long> {
        val folder = File(folder)
        val onControl = transaction {
            catalog.select(catalog.size.sum()).firstOrNull()?.get(catalog.size.sum()) ?: 0
        }
        return mapOf(
            "total_space" to folder.totalSpace,
            "free" to folder.freeSpace,
            "on_control" to onControl.toLong()
        )
    }


}