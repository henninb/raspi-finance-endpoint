package finance.controllers

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import finance.domain.ServiceResult
import finance.services.StandardizedFamilyMemberService
import jakarta.validation.Valid
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@CrossOrigin
@RestController
@RequestMapping("/api/family-members")
open class FamilyMemberController(
    private val standardizedFamilyMemberService: StandardizedFamilyMemberService,
) : StandardizedBaseController() {
    // ===== STANDARDIZED CRUD ENDPOINTS =====

    /**
     * GET /api/family-members/active
     * Returns all active family members
     */
    @GetMapping("/active", produces = ["application/json"])
    fun findAllActive(): ResponseEntity<List<FamilyMember>> =
        when (val result = standardizedFamilyMemberService.findAllActive()) {
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
     * POST /api/family-members
     * Create new family member
     */
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    fun save(
        @Valid @RequestBody member: FamilyMember,
    ): ResponseEntity<*> =
        when (val result = standardizedFamilyMemberService.save(member)) {
            is ServiceResult.Success -> {
                logger.info("Family member created successfully: ${member.memberName} for owner: ${member.owner}")
                ResponseEntity.status(HttpStatus.CREATED).body(result.data)
            }
            is ServiceResult.ValidationError -> {
                logger.warn("Validation error creating family member: ${result.errors}")
                ResponseEntity.badRequest().body(mapOf("errors" to result.errors))
            }
            is ServiceResult.BusinessError -> {
                logger.warn("Business error creating family member: ${result.message}")
                val userMessage =
                    if (result.errorCode == "DATA_INTEGRITY_VIOLATION") {
                        "Duplicate family member found"
                    } else {
                        result.message
                    }
                ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to userMessage))
            }
            is ServiceResult.SystemError -> {
                logger.error("System error creating family member: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Internal server error"))
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Any>()
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
    ): ResponseEntity<List<FamilyMember>> = ResponseEntity.ok(standardizedFamilyMemberService.findByOwner(owner))

    /**
     * GET /api/family-members/owner/{owner}/relationship/{relationship}
     * Get family members by owner and relationship type
     */
    @GetMapping("/owner/{owner}/relationship/{relationship}", produces = ["application/json"])
    fun byOwnerAndRelationship(
        @PathVariable owner: String,
        @PathVariable relationship: FamilyRelationship,
    ): ResponseEntity<List<FamilyMember>> = ResponseEntity.ok(standardizedFamilyMemberService.findByOwnerAndRelationship(owner, relationship))

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
            val ok = standardizedFamilyMemberService.updateActiveStatus(id, true)
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
            val ok = standardizedFamilyMemberService.updateActiveStatus(id, false)
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

    /**
     * DELETE /api/family-members/{id}
     * Soft delete a family member (sets active status to false)
     */
    @DeleteMapping("/{id}")
    fun softDelete(
        @PathVariable id: Long,
    ): ResponseEntity<Map<String, String>> =
        try {
            logger.info("Soft deleting family member: $id")
            val ok = standardizedFamilyMemberService.softDelete(id)
            if (ok) {
                logger.info("Family member soft deleted successfully: $id")
                ResponseEntity.ok(mapOf("message" to "Family member deleted successfully"))
            } else {
                logger.warn("Family member not found for deletion: $id")
                ResponseEntity.notFound().build<Map<String, String>>()
            }
        } catch (ex: Exception) {
            logger.error("Failed to soft delete family member $id: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete family member: ${ex.message}", ex)
        }
}
