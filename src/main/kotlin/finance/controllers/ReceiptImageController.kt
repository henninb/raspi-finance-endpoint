package finance.controllers

import finance.domain.ReceiptImage
import finance.domain.toCreatedResponse
import finance.domain.toListOkResponse
import finance.domain.toOkResponse
import finance.services.ReceiptImageService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
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

@Tag(name = "Receipt Image Management", description = "Operations for managing receipt images")
@RestController
@RequestMapping("/api/receipt/image")
@PreAuthorize("hasAuthority('USER')")
class ReceiptImageController(
    private val receiptImageService: ReceiptImageService,
) : StandardizedBaseController(),
    StandardRestController<ReceiptImage, Long> {
    @Operation(summary = "Get all active receipt images")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Active receipt images retrieved"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<ReceiptImage>> = receiptImageService.findAllActive().toListOkResponse()

    @Operation(summary = "Get receipt image by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Receipt image retrieved"),
            ApiResponse(responseCode = "404", description = "Receipt image not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/{receiptImageId}", produces = ["application/json"])
    override fun findById(
        @PathVariable("receiptImageId") id: Long,
    ): ResponseEntity<ReceiptImage> = receiptImageService.findById(id).toOkResponse()

    @Operation(summary = "Create receipt image")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Receipt image created"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "409", description = "Conflict/duplicate"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(
        @Valid @RequestBody entity: ReceiptImage,
    ): ResponseEntity<ReceiptImage> = receiptImageService.save(entity).toCreatedResponse()

    @Operation(summary = "Update receipt image by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Receipt image updated"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "404", description = "Receipt image not found"),
            ApiResponse(responseCode = "409", description = "Conflict"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @PutMapping("/{receiptImageId}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(
        @PathVariable("receiptImageId") id: Long,
        @Valid @RequestBody entity: ReceiptImage,
    ): ResponseEntity<ReceiptImage> = receiptImageService.update(entity).toOkResponse()

    @Operation(summary = "Delete receipt image by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Receipt image deleted"),
            ApiResponse(responseCode = "404", description = "Receipt image not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @DeleteMapping("/{receiptImageId}", produces = ["application/json"])
    override fun deleteById(
        @PathVariable("receiptImageId") id: Long,
    ): ResponseEntity<ReceiptImage> = receiptImageService.deleteById(id).toOkResponse()
}
