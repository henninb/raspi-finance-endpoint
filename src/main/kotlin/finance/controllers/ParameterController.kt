package finance.controllers

import finance.domain.Parameter
import finance.domain.ServiceResult
import finance.domain.toCreatedResponse
import finance.domain.toListOkResponse
import finance.domain.toOkResponse
import finance.services.ParameterService
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

@Tag(name = "Parameter Management", description = "Operations for managing parameters")
@RestController
@RequestMapping("/api/parameter")
@PreAuthorize("hasAuthority('USER')")
class ParameterController(
    private val parameterService: ParameterService,
) : StandardizedBaseController(),
    StandardRestController<Parameter, String> {
    @Operation(summary = "Get all active parameters")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Active parameters retrieved"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<Parameter>> = parameterService.findAllActive().toListOkResponse()

    @Operation(summary = "Get parameter by name")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Parameter retrieved"),
            ApiResponse(responseCode = "404", description = "Parameter not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/{parameterName}", produces = ["application/json"])
    override fun findById(
        @PathVariable("parameterName") id: String,
    ): ResponseEntity<Parameter> = parameterService.findByParameterNameStandardized(id).toOkResponse()

    @Operation(summary = "Create parameter")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Parameter created"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "409", description = "Conflict/duplicate"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(
        @Valid @RequestBody entity: Parameter,
    ): ResponseEntity<Parameter> = parameterService.save(entity).toCreatedResponse()

    @Operation(summary = "Update parameter by name")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Parameter updated"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "404", description = "Parameter not found"),
            ApiResponse(responseCode = "409", description = "Conflict"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @PutMapping("/{parameterName}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(
        @PathVariable("parameterName") id: String,
        @Valid @RequestBody entity: Parameter,
    ): ResponseEntity<Parameter> {
        val existsResult = parameterService.findByParameterNameStandardized(id)
        if (existsResult !is ServiceResult.Success) return existsResult.toOkResponse()
        val updatedParameter =
            Parameter(
                parameterId = existsResult.data.parameterId,
                parameterName = id,
                parameterValue = entity.parameterValue,
                activeStatus = entity.activeStatus,
            )
        return parameterService.update(updatedParameter).toOkResponse()
    }

    @Operation(summary = "Delete parameter by name")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Parameter deleted"),
            ApiResponse(responseCode = "404", description = "Parameter not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @DeleteMapping("/{parameterName}", produces = ["application/json"])
    override fun deleteById(
        @PathVariable("parameterName") id: String,
    ): ResponseEntity<Parameter> = parameterService.deleteByParameterNameStandardized(id).toOkResponse()
}
