package finance.controllers

import finance.domain.Reward
import finance.domain.toCreatedResponse
import finance.domain.toListOkResponse
import finance.domain.toOkResponse
import finance.services.RewardService
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

@Tag(name = "Reward Management", description = "Operations for managing reward tiers")
@RestController
@RequestMapping("/api/reward")
@PreAuthorize("hasAuthority('USER')")
class RewardController(
    private val rewardService: RewardService,
) : StandardizedBaseController(),
    StandardRestController<Reward, Long> {
    @Operation(summary = "Get all active reward tiers")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Active reward tiers retrieved"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<Reward>> = rewardService.findAllActive().toListOkResponse()

    @Operation(summary = "Get reward tier by id")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Reward tier retrieved"),
            ApiResponse(responseCode = "404", description = "Reward tier not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/{rewardId}", produces = ["application/json"])
    override fun findById(
        @PathVariable("rewardId") id: Long,
    ): ResponseEntity<Reward> = rewardService.findById(id).toOkResponse()

    @Operation(summary = "Create reward tier")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Reward tier created"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "409", description = "Conflict/duplicate"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(
        @Valid @RequestBody entity: Reward,
    ): ResponseEntity<Reward> = rewardService.save(entity).toCreatedResponse()

    @Operation(summary = "Update reward tier")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Reward tier updated"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "404", description = "Reward tier not found"),
            ApiResponse(responseCode = "409", description = "Conflict"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @PutMapping("/{rewardId}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(
        @PathVariable("rewardId") id: Long,
        @Valid @RequestBody entity: Reward,
    ): ResponseEntity<Reward> {
        entity.rewardId = id
        return rewardService.update(entity).toOkResponse()
    }

    @Operation(summary = "Delete reward tier by id")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Reward tier deleted"),
            ApiResponse(responseCode = "404", description = "Reward tier not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @DeleteMapping("/{rewardId}", produces = ["application/json"])
    override fun deleteById(
        @PathVariable("rewardId") id: Long,
    ): ResponseEntity<Reward> = rewardService.deleteById(id).toOkResponse()
}
