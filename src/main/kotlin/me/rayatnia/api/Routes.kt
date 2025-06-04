package me.rayatnia.api

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.rayatnia.application.commands.UploadUserDataCommand
import me.rayatnia.application.queries.GetUserDataQuery
import utils.Logger

fun Application.configureRouting(
    uploadCommand: UploadUserDataCommand,
    getUserDataQuery: GetUserDataQuery
) {
    val logger = Logger.withContext(mapOf("component" to "Routes"))

    routing {
        get("/") {
            call.respondText("Hello World! Ktor HTTP/2 server running.")
        }

        post("/api/users/upload") {
            logger.info("Received file upload request")
            
            try {
                val multipart = call.receiveMultipart()
                multipart.forEachPart { part ->
                    if (part is PartData.FileItem) {
                        logger.debug("Processing uploaded file", 
                            "originalFileName" to part.originalFileName,
                            "contentType" to part.contentType
                        )
                        
                        val originalFileName = part.originalFileName ?: "unknown.csv"
                        val result = uploadCommand.handle(originalFileName, part.streamProvider())
                        
                        result.fold(
                            onSuccess = { event ->
                                logger.info("File upload successful", "fileId" to event.aggregateId)
                                call.respond(HttpStatusCode.OK, mapOf(
                                    "message" to "File uploaded successfully",
                                    "fileId" to event.aggregateId
                                ))
                            },
                            onFailure = { e ->
                                logger.error("Failed to process file upload", e)
                                call.respond(HttpStatusCode.BadRequest, mapOf(
                                    "message" to "Failed to process file",
                                    "error" to e.message
                                ))
                            }
                        )
                    }
                    part.dispose()
                }
            } catch (e: Exception) {
                logger.error("Failed to process file upload", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "message" to "Failed to process file",
                    "error" to e.message
                ))
            }
        }

        get("/api/users") {
            logger.info("Received request to get all users")
            
            try {
                val users = getUserDataQuery.getAll()
                logger.info("Retrieved users successfully", "count" to users.size)
                call.respond(users)
            } catch (e: Exception) {
                logger.error("Failed to retrieve users", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "message" to "Failed to retrieve users",
                    "error" to e.message
                ))
            }
        }

        get("/api/users/{id}") {
            val id = call.parameters["id"]
            logger.info("Received request to get user by ID", "userId" to id)
            
            try {
                if (id == null) {
                    logger.warn("User ID not provided")
                    call.respond(HttpStatusCode.BadRequest, mapOf(
                        "message" to "User ID is required"
                    ))
                    return@get
                }

                val user = getUserDataQuery.getById(id)
                if (user != null) {
                    logger.info("Retrieved user successfully", "userId" to id)
                    call.respond(user)
                } else {
                    logger.warn("User not found", "userId" to id)
                    call.respond(HttpStatusCode.NotFound, mapOf(
                        "message" to "User not found"
                    ))
                }
            } catch (e: Exception) {
                logger.error("Failed to retrieve user", e, "userId" to id)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "message" to "Failed to retrieve user",
                    "error" to e.message
                ))
            }
        }
    }
} 