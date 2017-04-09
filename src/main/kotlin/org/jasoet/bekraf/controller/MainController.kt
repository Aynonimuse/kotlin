package org.jasoet.bekraf.controller

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.Router
import org.jasoet.bekraf.extension.DataInconsistentException
import org.jasoet.bekraf.extension.NotAllowedException
import org.jasoet.bekraf.extension.NullObjectException
import org.jasoet.bekraf.extension.RegistrationException
import org.jasoet.bekraf.extension.endWithJson
import org.jasoet.bekraf.extension.first
import org.jasoet.bekraf.extension.header
import org.jasoet.bekraf.extension.logger
import org.jasoet.bekraf.extension.serveStatic
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Named

/**
 * [Documentation Here]
 *
 * @author Deny Prasetyo.
 */


class MainController @Inject constructor(override val router: Router,
                                         val insitutionController: InstitutionController,
                                         val locationController: LocationController,
                                         @Named("imageLocation") val imageLocation: String) : Controller({

    val log = logger(MainController::class)

    route("/static/*").serveStatic()
    route().last().handler { it.fail(404) }

    mountSubRouter("/api/institution/", insitutionController.create())
    mountSubRouter("/api/location/", locationController.create())

    route().first().failureHandler { errorContext ->
        val e: Throwable? = errorContext.failure()
        if (e != null) {
            log.error(e.message, e)
        }
        val code = when (e) {
            is FileNotFoundException -> HttpResponseStatus.NOT_FOUND.code()
            is NullObjectException -> HttpResponseStatus.NOT_FOUND.code()
            is DataInconsistentException -> HttpResponseStatus.INTERNAL_SERVER_ERROR.code()
            is NotAllowedException -> HttpResponseStatus.METHOD_NOT_ALLOWED.code()
            is SecurityException -> HttpResponseStatus.UNAUTHORIZED.code()
            is RegistrationException -> HttpResponseStatus.BAD_REQUEST.code()
            else ->
                if (errorContext.statusCode() > 0) {
                    errorContext.statusCode()
                } else {
                    500
                }
        }

        val acceptHeader = errorContext.header("Accept") ?: ""
        val contentTypeHeader = errorContext.header("Content-Type") ?: ""
        if (acceptHeader.matches(".*/json$".toRegex()) || contentTypeHeader.matches(".*/json$".toRegex())) {
            val result = mapOf(
                    "success" to false,
                    "message" to errorContext.failure().message
            )
            errorContext.response().setStatusCode(code).endWithJson(result)
        } else {
            errorContext
                    .reroute(HttpMethod.GET, "/static/html/$code.html")
        }
    }
})