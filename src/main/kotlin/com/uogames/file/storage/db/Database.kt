package com.uogames.file.storage.db

import com.uogames.file.storage.config.PostgresConfig
import com.uogames.file.storage.model.AccessType
import com.uogames.file.storage.util.DatabaseEXT
import io.r2dbc.spi.ConnectionFactoryOptions
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

object Database {

    suspend fun initPostgres(pc: PostgresConfig) {
        R2dbcDatabase.connect(
            databaseConfig = {
                connectionFactoryOptions {
                    option(ConnectionFactoryOptions.HOST, pc.host)
                    option(ConnectionFactoryOptions.PORT, pc.port)
                    option(ConnectionFactoryOptions.USER, pc.user)
                    option(ConnectionFactoryOptions.PASSWORD, pc.password)
                    option(ConnectionFactoryOptions.DATABASE, pc.dbName)
                    option(ConnectionFactoryOptions.SSL, false)
                    option(ConnectionFactoryOptions.DRIVER, "postgresql")
                }
            }
        )

        suspendTransaction {
            SchemaUtils.create(FileCatalog)
        }
    }

    object FileCatalog : DatabaseEXT.UuidV7Table() {
        val size = integer("size")
        val createdAt = long("created_at").clientDefault { System.currentTimeMillis() }
        val lastRequest = long("last_request").clientDefault { System.currentTimeMillis() }
        val accessType = enumerationByName<AccessType>("access_type", 10)
        val contentType = text("content_type")
        val requests = long("requests").default(0)
    }


}