package finance.controllers

import finance.domain.Description
import finance.domain.MergeDescriptionsRequest
import finance.domain.toCreatedResponse
import finance.domain.toListOkResponse
import finance.domain.toOkResponse
import finance.domain.toPagedOkResponse
import finance.services.DescriptionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@Tag(name = "Description Management", description = "Operations for managing descriptions")
@RestController
@RequestMapping("/api/description")
@PreAuthorize("hasAuthority('USER')")
class DescriptionController(
    private val descriptionService: DescriptionService,
) : StandardizedBaseController(),
    StandardRestController<Description, String> {
    @Operation(summary = "Get all active descriptions")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Active descriptions retrieved"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<Description>> = descriptionService.findAllActive().toListOkResponse()

    @Operation(summary = "Get all active descriptions (paginated)")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of descriptions returned"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active/paged", produces = ["application/json"])
    override fun findAllActivePaged(pageable: Pageable): ResponseEntity<Page<Description>> = descriptionService.findAllActive(pageable).toPagedOkResponse(pageable)

    @Operation(summary = "Get description by name")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Description retrieved"),
            ApiResponse(responseCode = "404", description = "Description not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/{descriptionName}", produces = ["application/json"])
    override fun findById(
        @PathVariable("descriptionName") id: String,
    ): ResponseEntity<Description> = descriptionService.findByDescriptionNameStandardized(id).toOkResponse()

    @Operation(summary = "Create description")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Description created"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "409", description = "Conflict/duplicate"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(
        @Valid @RequestBody entity: Description,
    ): ResponseEntity<Description> = descriptionService.save(entity).toCreatedResponse()

    @Operation(summary = "Update description by name")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Description updated"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "404", description = "Description not found"),
            ApiResponse(responseCode = "409", description = "Conflict"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @PutMapping("/{descriptionName}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(
        @PathVariable("descriptionName") id: String,
        @Valid @RequestBody entity: Description,
    ): ResponseEntity<Description> = descriptionService.update(entity).toOkResponse()

    @Operation(summary = "Delete description by name")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Description deleted"),
            ApiResponse(responseCode = "404", description = "Description not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @DeleteMapping("/{descriptionName}", produces = ["application/json"])
    override fun deleteById(
        @PathVariable("descriptionName") id: String,
    ): ResponseEntity<Description> = descriptionService.deleteByDescriptionNameStandardized(id).toOkResponse()

    // ===== BUSINESS LOGIC ENDPOINTS =====

    @Operation(summary = "Merge multiple descriptions into a target")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Descriptions merged"), ApiResponse(responseCode = "400", description = "Bad request"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @PostMapping("/merge", consumes = ["application/json"], produces = ["application/json"])
    fun mergeDescriptions(
        @RequestBody request: MergeDescriptionsRequest,
    ): ResponseEntity<Description> =
        try {
            if (request.targetName.isBlank() || request.sourceNames.isEmpty()) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "targetName and sourceNames are required")
            }
            ResponseEntity.ok(descriptionService.mergeDescriptions(request.targetName, request.sourceNames))
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to merge descriptions into ${request.targetName}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to merge descriptions")
        }
}
