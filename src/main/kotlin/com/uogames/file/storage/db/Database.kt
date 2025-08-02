package com.uogames.file.storage.db

import com.uogames.file.storage.config.PostgresConfig
import com.uogames.file.storage.model.AccessType
import com.uogames.file.storage.util.DatabaseEXT
import io.r2dbc.spi.ConnectionFactoryOptions
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.io.File
import java.sql.Connection

object Database {


    fun init(dbFolder: String): Database {
        File(dbFolder).mkdirs()

        val db = Database.connect("jdbc:sqlite:$dbFolder/data.db")
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        transaction {
            SchemaUtils.create(FileCatalog)
        }
        return db
    }

    suspend fun initPostgres(pc: PostgresConfig) {
        val db = Database.connect(
            url = "jdbc:postgresql://${pc.host}:${pc.port}/postgres",
            driver = "org.postgresql.Driver",
            user = pc.user,
            password = pc.password,
        )
        transaction(db) {
            val datNameObj = object : Table("pg_database") {
                val datName = text("datname")
            }
            val dbList = datNameObj.select(datNameObj.datName).map { it[datNameObj.datName] }
            if (pc.dbName !in dbList) {
                connection.autoCommit = true
                SchemaUtils.createDatabase(pc.dbName)
                connection.autoCommit = false
            }
            commit()
        }
        org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager.closeAndUnregister(db)

        R2dbcDatabase.connect(
//            url = "r2dbc:postgresql://${pc.host}:${pc.port}/${pc.dbName}",
            databaseConfig = {
                connectionFactoryOptions {
                    option(ConnectionFactoryOptions.HOST, pc.host)
                    option(ConnectionFactoryOptions.PORT, pc.port)
                    option(ConnectionFactoryOptions.USER, pc.user)
                    option(ConnectionFactoryOptions.PASSWORD, pc.password)
                    option(ConnectionFactoryOptions.DATABASE, pc.dbName)
                    option(ConnectionFactoryOptions.SSL,false)
                    option(ConnectionFactoryOptions.DRIVER, "postgresql")
                }
            }
        )
        suspendTransaction {
            org.jetbrains.exposed.v1.r2dbc.SchemaUtils.create(FileCatalog)
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