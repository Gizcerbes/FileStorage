package com.uogames.file.storage.util

import com.uogames.file.storage.util.UuidExt.randomV7
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

object DatabaseEXT {


    fun <T : Table> T.upsert(
        where: SqlExpressionBuilder.() -> Op<Boolean>,
        data: T.(UpdateBuilder<Number>) -> Unit,
    ) {
        val exp = columns.first().count()
        val result = select(columns.first().count())
            .where(where)
            .firstOrNull()
            ?.get(exp) ?: 0L
        if (result == 0L) {
            insert(data)
        } else {
            update(where, null, data)
        }
    }


    @OptIn(ExperimentalUuidApi::class)
    open class UuidV7Table(name: String = "", columnName: String = "id") : IdTable<UUID>(name) {
        /** The identity column of this [UUIDTable], for storing UUIDs wrapped as [EntityID] instances. */
        final override val id: Column<EntityID<UUID>> = uuid(columnName)
            .clientDefault { Uuid.randomV7().toJavaUuid() }
            .entityId()
        final override val primaryKey = PrimaryKey(id)
    }

    @OptIn(ExperimentalUuidApi::class)
    open class StringIdTable(name: String = "", columnName: String = "id"): IdTable<String>(name){
        final override val id: Column<EntityID<String>> = text(columnName)
            .clientDefault { Uuid.randomV7().toJavaUuid().toString() }
            .entityId()
        final override val primaryKey = PrimaryKey(id)
    }


}