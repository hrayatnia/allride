package me.rayatnia.api

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.rayatnia.application.commands.UploadUserDataCommand
import me.rayatnia.application.queries.GetUserDataQuery

fun Application.configureRouting(
    uploadCommand: UploadUserDataCommand,
    getUserDataQuery: GetUserDataQuery
) {
    routing {
        route("/api/v1/users") {
            post("/upload") {
                val multipart = call.receiveMultipart()
                var fileItem: PartData.FileItem? = null
                var originalFileName: String? = null
                
                try {
                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                fileItem = part
                                originalFileName = part.originalFileName
                            }
                            else -> part.dispose()
                        }
                        if (fileItem != null) return@forEachPart
                    }
                    
                    if (fileItem == null || originalFileName == null) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No file uploaded or filename missing"))
                        return@post
                    }
                    
                    fileItem!!.streamProvider().use { inputStream ->
                        val result = uploadCommand.handle(originalFileName!!, inputStream)
                        result.fold(
                            onSuccess = { event ->
                                call.respond(HttpStatusCode.Accepted, mapOf(
                                    "message" to "File uploaded successfully",
                                    "fileId" to event.aggregateId
                                ))
                            },
                            onFailure = { error ->
                                call.respond(HttpStatusCode.BadRequest, mapOf("error" to error.message))
                            }
                        )
                    }
                } finally {
                    fileItem?.dispose?.invoke()
                }
            }
            
            get {
                val users = getUserDataQuery.getAll()
                call.respond(users)
            }
            
            get("/{id}") {
                val id = call.parameters["id"] ?: throw IllegalArgumentException("User ID is required")
                val user = getUserDataQuery.getById(id)
                if (user != null) {
                    call.respond(user)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
            
            get("/email/{email}") {
                val email = call.parameters["email"] ?: throw IllegalArgumentException("Email is required")
                val user = getUserDataQuery.getByEmail(email)
                if (user != null) {
                    call.respond(user)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
} 