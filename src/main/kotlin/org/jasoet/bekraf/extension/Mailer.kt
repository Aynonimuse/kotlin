package org.jasoet.bekraf.extension

import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter
import com.sun.jersey.core.util.MultivaluedMapImpl
import com.sun.jersey.multipart.FormDataMultiPart
import com.sun.jersey.multipart.file.FileDataBodyPart
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.json.JsonObject
import org.jasoet.bekraf.extension.logger
import java.io.File
import javax.ws.rs.core.MediaType


/**
 * [Documentation Here]
 *
 * @author Deny Prasetyo.
 */
data class Destination(val to: String, val params: Map<String, String> = emptyMap())

private class Mailer(val sender: String = "no-reply@mail.ra-system.net") {

    private val log = logger(Mailer::class)

    private fun credentials(): Pair<String, HTTPBasicAuthFilter> {
        return "https://api.mailgun.net/v3/mail.ra-system.net/messages" to
            HTTPBasicAuthFilter("api", "key-558d8549bc8615885e8c910a7b20a101")
    }

    fun sendMessage(to: String,
                    subject: String,
                    html: String,
                    attachments: List<File> = emptyList()): JsonObject {
        val destinations = listOf(Destination(to))
        return sendMessage(destinations, subject, html, attachments)
    }

    fun sendBulkMessage(destinations: List<Destination>,
                        subject: String,
                        html: String,
                        attachments: List<File> = emptyList()): List<JsonObject> {
        return destinations.partition(500).map {
            sendMessage(it, subject, html, attachments)
        }
    }

    private fun sendMessage(destinations: List<Destination>,
                            subject: String,
                            html: String,
                            attachments: List<File>): JsonObject {

        val credentials = credentials()
        val client = Client.create()
        client.addFilter(credentials.second)
        val webResource = client.resource(credentials.first)

        fun post(): ClientResponse? {
            return if (attachments.isEmpty()) {
                val formData = MultivaluedMapImpl()
                formData.add("from", sender)
                destinations.forEach {
                    formData.add("to", it.to)
                }
                formData.add("recipient-variables", destinations.toJson().toString())
                formData.add("subject", subject)
                formData.add("html", html)
                webResource
                    .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                    .post(ClientResponse::class.java, formData)
            } else {
                val formData = FormDataMultiPart()
                formData.field("from", sender)
                destinations.forEach {
                    formData.field("to", it.to)
                }
                formData.field("recipient-variables", destinations.toJson().toString())
                formData.field("subject", subject)
                formData.field("html", html)

                attachments.filter { it.isFile && it.exists() }.forEach { file ->
                    formData.bodyPart(FileDataBodyPart("attachment", file, MediaType.APPLICATION_OCTET_STREAM_TYPE))
                }

                webResource
                    .type(MediaType.MULTIPART_FORM_DATA_TYPE)
                    .post(ClientResponse::class.java, formData)
            }
        }


        return try {
            val clientResponse = post()
            val body = JsonObject(clientResponse?.getEntity(String::class.java))
            val resultJson = JsonObject()
            resultJson.put("status", clientResponse?.statusInfo?.statusCode)
            resultJson.put("reason", clientResponse?.statusInfo?.reasonPhrase)
            resultJson.put("class", clientResponse?.statusInfo?.family?.name)
            resultJson.put("body", body)

            log.info("Success Send Email [${destinations.toJson()}]-[$subject]-[$body]")

            resultJson
        } catch(throwable: Exception) {
            log.info("Exception when Send Email [${destinations.toJson()}]-[$subject], ${throwable.message}", throwable)
            val resultJson = JsonObject()
            resultJson.put("status", HttpResponseStatus.INTERNAL_SERVER_ERROR)
            resultJson.put("reason", throwable.message)
            resultJson
        }
    }
}

private fun List<Destination>.partition(partitionSize: Int): List<List<Destination>> {
    val maxIteration = this.size / partitionSize
    return (0..maxIteration).map {
        val start = it * partitionSize
        val end = start + partitionSize
        this.subList(start, if (end >= this.size) this.size else end)
    }.filter { it.isNotEmpty() }
}

private fun List<Destination>.toJson(): JsonObject {
    val result = JsonObject()
    this.forEach { destination ->
        result.put(destination.to, destination.params.toJson())
    }
    return result
}

private fun Map<String, String>.toJson(): JsonObject {
    val result = JsonObject()
    this.forEach { map ->
        val (key, value) = map
        result.put(key, value)
    }
    return result
}

fun sendEmail(sender: String = "no-reply@ra-system.net",
              to: String,
              subject: String,
              html: String,
              attachments: List<File> = emptyList()): JsonObject {
    return Mailer(sender).sendMessage(to, subject, html, attachments)
}

fun sendBulkEmail(sender: String = "no-reply@ra-system.net",
                  to: List<Destination>,
                  subject: String,
                  html: String,
                  attachments: List<File> = emptyList()): List<JsonObject> {
    return Mailer(sender).sendBulkMessage(to, subject, html, attachments)
}