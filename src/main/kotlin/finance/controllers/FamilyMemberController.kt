package finance.controllers

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import finance.domain.ServiceResult
import finance.domain.toCreatedResponse
import finance.domain.toListOkResponse
import finance.domain.toOkResponse
import finance.services.FamilyMemberService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
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

@Tag(name = "Family Member Management", description = "Operations for managing family members")
@RestController
@RequestMapping("/api/family-members")
@PreAuthorize("hasAuthority('USER')")
open class FamilyMemberController(
    private val familyMemberService: FamilyMemberService,
) : StandardizedBaseController(),
    StandardRestController<FamilyMember, Long> {
    @Operation(summary = "Get all active family members")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Active family members retrieved"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<FamilyMember>> = familyMemberService.findAllActive().toListOkResponse()

    @Operation(summary = "Get family member by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Family member retrieved"),
            ApiResponse(responseCode = "404", description = "Family member not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/{familyMemberId}", produces = ["application/json"])
    override fun findById(
        @PathVariable("familyMemberId") id: Long,
    ): ResponseEntity<FamilyMember> = familyMemberService.findByIdServiceResult(id).toOkResponse()

    @Operation(summary = "Create family member")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Family member created"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "409", description = "Conflict/duplicate"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(
        @Valid @RequestBody entity: FamilyMember,
    ): ResponseEntity<FamilyMember> = familyMemberService.save(entity).toCreatedResponse()

    @Operation(summary = "Update family member by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Family member updated"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "404", description = "Family member not found"),
            ApiResponse(responseCode = "409", description = "Conflict"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @PutMapping("/{familyMemberId}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(
        @PathVariable("familyMemberId") id: Long,
        @Valid @RequestBody entity: FamilyMember,
    ): ResponseEntity<FamilyMember> = familyMemberService.update(entity).toOkResponse()

    @Operation(summary = "Delete family member by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Family member deleted"),
            ApiResponse(responseCode = "404", description = "Family member not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @DeleteMapping("/{familyMemberId}", produces = ["application/json"])
    override fun deleteById(
        @PathVariable("familyMemberId") id: Long,
    ): ResponseEntity<FamilyMember> {
        val findResult = familyMemberService.findByIdAnyStatus(id)
        if (findResult !is ServiceResult.Success) return findResult.toOkResponse()
        val memberToDelete = findResult.data
        return when (val deleteResult = familyMemberService.deleteByIdAnyStatus(id)) {
            is ServiceResult.Success -> ResponseEntity.ok(memberToDelete)
            is ServiceResult.NotFound -> ResponseEntity.notFound().build()
            else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    // ===== BUSINESS LOGIC ENDPOINTS =====

    @GetMapping("/owner/{owner}", produces = ["application/json"])
    fun byOwner(
        @PathVariable owner: String,
    ): ResponseEntity<List<FamilyMember>> = ResponseEntity.ok(familyMemberService.findByOwner())

    @GetMapping("/owner/{owner}/relationship/{relationship}", produces = ["application/json"])
    fun byOwnerAndRelationship(
        @PathVariable owner: String,
        @PathVariable relationship: FamilyRelationship,
    ): ResponseEntity<List<FamilyMember>> = ResponseEntity.ok(familyMemberService.findByOwnerAndRelationship(relationship))

    @PutMapping("/{id}/activate")
    fun activateMember(
        @PathVariable id: Long,
    ): ResponseEntity<Map<String, String>> =
        try {
            if (familyMemberService.updateActiveStatus(id, true)) {
                ResponseEntity.ok(mapOf("message" to "Family member activated"))
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (ex: Exception) {
            logger.error("Failed to activate family member $id: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to activate family member: ${ex.message}", ex)
        }

    @PutMapping("/{id}/deactivate")
    fun deactivateMember(
        @PathVariable id: Long,
    ): ResponseEntity<Map<String, String>> =
        try {
            if (familyMemberService.updateActiveStatus(id, false)) {
                ResponseEntity.ok(mapOf("message" to "Family member deactivated"))
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (ex: Exception) {
            logger.error("Failed to deactivate family member $id: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to deactivate family member: ${ex.message}", ex)
        }
}
