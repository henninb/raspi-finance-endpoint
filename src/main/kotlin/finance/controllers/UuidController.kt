package finance.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

/**
 * Controller for secure UUID generation
 * Provides server-side UUID generation for enhanced security
 */
@RestController
@Tag(name = "UUID Utilities", description = "Server-side UUID generation endpoints")
@RequestMapping("/api/uuid")
class UuidController : BaseController() {
    /**
     * Generate a secure UUID on the server side
     *
     * @return ResponseEntity containing the generated UUID and timestamp
     */
    @Operation(summary = "Generate single UUID with timestamp")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "UUID generated"),
            ApiResponse(responseCode = "500", description = "Failed to generate UUID"),
        ],
    )
    @PostMapping(
        value = ["/generate"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun generateUuid(): ResponseEntity<Map<String, Any>> {
        logger.info("UUID generation requested")

        try {
            val uuid = UUID.randomUUID().toString()
            val timestamp = Instant.now().toEpochMilli()

            val response =
                mapOf(
                    "uuid" to uuid,
                    "timestamp" to timestamp,
                    "source" to "server",
                )

            logger.debug("Generated UUID: {}", uuid)
            return ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error generating UUID", e)
            return ResponseEntity
                .internalServerError()
                .body(mapOf("error" to "Failed to generate UUID"))
        }
    }

    /**
     * Generate multiple UUIDs at once for batch operations
     * Limited to prevent abuse
     *
     * @param count Number of UUIDs to generate (max 100)
     * @return ResponseEntity containing array of generated UUIDs
     */
    @Operation(summary = "Generate multiple UUIDs (max 100)")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "UUIDs generated"),
            ApiResponse(responseCode = "400", description = "Invalid count"),
            ApiResponse(responseCode = "500", description = "Failed to generate UUIDs"),
        ],
    )
    @PostMapping(
        value = ["/generate/batch"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun generateBatchUuids(count: Int = 1): ResponseEntity<Map<String, Any>> {
        logger.info("Batch UUID generation requested for {} UUIDs", count)

        if (count <= 0 || count > 100) {
            logger.warn("Invalid UUID count requested: {}", count)
            return ResponseEntity
                .badRequest()
                .body(mapOf("error" to "Count must be between 1 and 100"))
        }

        try {
            val uuids = (1..count).map { UUID.randomUUID().toString() }
            val timestamp = Instant.now().toEpochMilli()

            val response =
                mapOf(
                    "uuids" to uuids,
                    "count" to uuids.size,
                    "timestamp" to timestamp,
                    "source" to "server",
                )

            logger.debug("Generated {} UUIDs", uuids.size)
            return ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error generating batch UUIDs", e)
            return ResponseEntity
                .internalServerError()
                .body(mapOf("error" to "Failed to generate UUIDs"))
        }
    }

    /**
     * Health check endpoint for UUID service
     *
     * @return ResponseEntity indicating service status
     */
    @Operation(summary = "Health check for UUID service")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Service healthy")])
    @PostMapping(
        value = ["/health"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun healthCheck(): ResponseEntity<Map<String, Any>> =
        ResponseEntity.ok(
            mapOf(
                "status" to "healthy",
                "service" to "uuid-generation",
                "timestamp" to Instant.now().toEpochMilli(),
            ),
        )
}
