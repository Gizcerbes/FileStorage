package com.uogames.file.storage.config

import io.ktor.server.application.*
import io.ktor.server.config.*

data class PostgresConfig(
    val user: String,
    val password: String,
    val dbName: String,
    val host: String,
    val port: Int,
) {

    companion object {

        fun build(application: Application) = PostgresConfig(
            user = application.environment.config.property("ktor.database.postgres.user").getAs(),
            password = application.environment.config.property("ktor.database.postgres.password").getAs(),
            dbName = application.environment.config.property("ktor.database.postgres.db_name").getAs(),
            host = application.environment.config.property("ktor.database.postgres.host").getAs(),
            port = application.environment.config.property("ktor.database.postgres.port").getAs()
        )

    }

}