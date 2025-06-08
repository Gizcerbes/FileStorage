package com.uogames.file.storage.db

import com.uogames.file.storage.model.AccessType
import com.uogames.file.storage.util.DatabaseEXT
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object Database {


    fun init(dbFolder: String): org.jetbrains.exposed.sql.Database {
        File(dbFolder).mkdirs()

        val db = org.jetbrains.exposed.sql.Database.connect("jdbc:sqlite:$dbFolder/data.db")

        transaction {
            SchemaUtils.create(FileCatalog)
        }
        return db
    }


    object FileCatalog : DatabaseEXT.UuidV7Table() {
        val size = integer("size")
        val createdAt = long("created_at").clientDefault { System.currentTimeMillis() }
        val lastRequest = long("last_request").clientDefault { System.currentTimeMillis() }
        val accessType = enumerationByName<AccessType>("access_type", 10)
    }


}