package com.uogames.file.storage.model

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlin.jvm.java
import kotlin.text.orEmpty

object Errors {

    abstract class CommonError(val code: HttpStatusCode) : Exception()
    class BadRequestException(val info: String) : CommonError(HttpStatusCode.BadRequest)
    class NotFoundException(val info: String) : CommonError(HttpStatusCode.NotFound)
    class Forbidden(val info: String) : CommonError(HttpStatusCode.Forbidden)
    class UserNotProvided(val info: String): CommonError(HttpStatusCode.Unauthorized)


    suspend fun handle(call: ApplicationCall, cause: Throwable) {
        when (cause) {
            is CommonError -> handeCommonError(call, cause)
            else -> {
                cause.printStackTrace()
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = BaseModel<Nothing>(
                        error = ErrorModel(
                            message = cause.message.orEmpty(),
                            detail = cause::class.java.simpleName
                        )
                    )
                )
            }
        }
    }

    private suspend fun handeCommonError(call: ApplicationCall, cause: CommonError) {
        val message = when (cause) {
            is BadRequestException -> cause.info
            is NotFoundException -> cause.info
            is Forbidden -> cause.info
            else -> cause.code.toString()
        }
        return call.respond(
            status = cause.code,
            message = BaseModel<Nothing>(
                error = ErrorModel(
                    message = message,
                    detail = cause::class.java.simpleName
                )
            )
        )
    }


}