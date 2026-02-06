package finance.controllers

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import finance.domain.ServiceResult
import finance.services.FamilyMemberService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
open class FamilyMemberController(
    private val familyMemberService: FamilyMemberService,
) : StandardizedBaseController(),
    StandardRestController<FamilyMember, Long> {
    // ===== STANDARDIZED CRUD ENDPOINTS =====

    /**
     * GET /api/family-members/active
     * Returns all active family members
     */
    @Operation(summary = "Get all active family members")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Active family members retrieved"),
            ApiResponse(responseCode = "404", description = "No family members found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<FamilyMember>> =
        when (val result = familyMemberService.findAllActive()) {
            is ServiceResult.Success -> {
                logger.info("Retrieved ${result.data.size} active family members")
                ResponseEntity.ok(result.data)
            }

            is ServiceResult.NotFound -> {
                logger.warn("No family members found")
                ResponseEntity.notFound().build<List<FamilyMember>>()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error retrieving family members: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<List<FamilyMember>>()
            }

            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<List<FamilyMember>>()
            }
        }

    /**
     * GET /api/family-members/{familyMemberId}
     * Get single family member by ID
     */
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
    ): ResponseEntity<FamilyMember> =
        when (val result = familyMemberService.findByIdServiceResult(id)) {
            is ServiceResult.Success<FamilyMember> -> {
                logger.info("Retrieved family member: $id")
                ResponseEntity.ok(result.data)
            }

            is ServiceResult.NotFound<FamilyMember> -> {
                logger.warn("Family member not found: $id")
                ResponseEntity.notFound().build()
            }

            is ServiceResult.SystemError<FamilyMember> -> {
                logger.error("System error retrieving family member $id: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }

            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }

    /**
     * POST /api/family-members
     * Create new family member
     */
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
    ): ResponseEntity<FamilyMember> =
        when (val result = familyMemberService.save(entity)) {
            is ServiceResult.Success -> {
                logger.info("Family member created successfully: ${entity.memberName} for owner: ${entity.owner}")
                ResponseEntity.status(HttpStatus.CREATED).body(result.data)
            }

            is ServiceResult.ValidationError -> {
                logger.warn("Validation error creating family member: ${result.errors}")
                ResponseEntity.badRequest().build()
            }

            is ServiceResult.BusinessError -> {
                logger.warn("Business error creating family member: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error creating family member: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }

            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }

    /**
     * PUT /api/family-members/{familyMemberId}
     * Update existing family member
     */
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
    ): ResponseEntity<FamilyMember> {
        @Suppress("REDUNDANT_ELSE_IN_WHEN") // Defensive programming: handle unexpected ServiceResult types
        return when (val result = familyMemberService.update(entity)) {
            is ServiceResult.Success -> {
                logger.info("Family member updated successfully: $id")
                ResponseEntity.ok(result.data)
            }

            is ServiceResult.NotFound -> {
                logger.warn("Family member not found for update: $id")
                ResponseEntity.notFound().build()
            }

            is ServiceResult.ValidationError -> {
                logger.warn("Validation error updating family member: ${result.errors}")
                ResponseEntity.badRequest().build()
            }

            is ServiceResult.BusinessError -> {
                logger.warn("Business error updating family member: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error updating family member: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }

            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * DELETE /api/family-members/{familyMemberId}
     * Delete family member (returns deleted entity)
     * Note: Finds member regardless of active status to support deleting already-deactivated members
     */
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
        // First get the family member to return it after deletion (regardless of active status)
        return when (val findResult = familyMemberService.findByIdAnyStatus(id)) {
            is ServiceResult.Success<FamilyMember> -> {
                val memberToDelete = findResult.data

                when (val deleteResult = familyMemberService.deleteByIdAnyStatus(id)) {
                    is ServiceResult.Success<Boolean> -> {
                        logger.info("Family member deleted successfully: $id")
                        ResponseEntity.ok(memberToDelete)
                    }

                    is ServiceResult.NotFound<Boolean> -> {
                        logger.warn("Family member not found for deletion: $id")
                        ResponseEntity.notFound().build()
                    }

                    is ServiceResult.SystemError<Boolean> -> {
                        logger.error("System error deleting family member: ${deleteResult.exception.message}", deleteResult.exception)
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                    }

                    else -> {
                        logger.error("Unexpected result type: $deleteResult")
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                    }
                }
            }

            is ServiceResult.NotFound<FamilyMember> -> {
                logger.warn("Family member not found for deletion: $id")
                ResponseEntity.notFound().build()
            }

            is ServiceResult.SystemError<FamilyMember> -> {
                logger.error("System error finding family member for deletion: ${findResult.exception.message}", findResult.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }

            else -> {
                logger.error("Unexpected result type: $findResult")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    // ===== BUSINESS LOGIC ENDPOINTS =====

    /**
     * GET /api/family-members/owner/{owner}
     * Get all family members by owner
     */
    @GetMapping("/owner/{owner}", produces = ["application/json"])
    fun byOwner(
        @PathVariable owner: String,
    ): ResponseEntity<List<FamilyMember>> = ResponseEntity.ok(familyMemberService.findByOwner())

    /**
     * GET /api/family-members/owner/{owner}/relationship/{relationship}
     * Get family members by owner and relationship type
     */
    @GetMapping("/owner/{owner}/relationship/{relationship}", produces = ["application/json"])
    fun byOwnerAndRelationship(
        @PathVariable owner: String,
        @PathVariable relationship: FamilyRelationship,
    ): ResponseEntity<List<FamilyMember>> = ResponseEntity.ok(familyMemberService.findByOwnerAndRelationship(relationship))

    /**
     * PUT /api/family-members/{id}/activate
     * Activate a family member
     */
    @PutMapping("/{id}/activate")
    fun activateMember(
        @PathVariable id: Long,
    ): ResponseEntity<Map<String, String>> =
        try {
            logger.info("Activating family member: $id")
            val ok = familyMemberService.updateActiveStatus(id, true)
            if (ok) {
                logger.info("Family member activated successfully: $id")
                ResponseEntity.ok(mapOf("message" to "Family member activated"))
            } else {
                logger.warn("Family member not found for activation: $id")
                ResponseEntity.notFound().build<Map<String, String>>()
            }
        } catch (ex: Exception) {
            logger.error("Failed to activate family member $id: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to activate family member: ${ex.message}", ex)
        }

    /**
     * PUT /api/family-members/{id}/deactivate
     * Deactivate a family member
     */
    @PutMapping("/{id}/deactivate")
    fun deactivateMember(
        @PathVariable id: Long,
    ): ResponseEntity<Map<String, String>> =
        try {
            logger.info("Deactivating family member: $id")
            val ok = familyMemberService.updateActiveStatus(id, false)
            if (ok) {
                logger.info("Family member deactivated successfully: $id")
                ResponseEntity.ok(mapOf("message" to "Family member deactivated"))
            } else {
                logger.warn("Family member not found for deactivation: $id")
                ResponseEntity.notFound().build<Map<String, String>>()
            }
        } catch (ex: Exception) {
            logger.error("Failed to deactivate family member $id: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to deactivate family member: ${ex.message}", ex)
        }
}
