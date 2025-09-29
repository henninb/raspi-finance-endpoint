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
open class FamilyMemberController(private val standardizedFamilyMemberService: StandardizedFamilyMemberService) :
    StandardizedBaseController() {
    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/family-members/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     */
    @GetMapping("/active", produces = ["application/json"])
    fun findAllActive(): ResponseEntity<List<FamilyMember>> {
        return when (val result = standardizedFamilyMemberService.findAllActive()) {
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
    }

    /**
     * Standardized single entity retrieval - GET /api/family-members/std/{familyMemberId}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @GetMapping("/std/{familyMemberId}", produces = ["application/json"])
    fun findById(
        @PathVariable familyMemberId: Long,
    ): ResponseEntity<FamilyMember> {
        return when (val result = standardizedFamilyMemberService.findByIdServiceResult(familyMemberId)) {
            is ServiceResult.Success -> {
                logger.info("Retrieved family member: $familyMemberId")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Family member not found: $familyMemberId")
                ResponseEntity.notFound().build<FamilyMember>()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving family member $familyMemberId: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<FamilyMember>()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<FamilyMember>()
            }
        }
    }

    /**
     * Standardized entity creation - POST /api/family-members
     * Returns 201 CREATED
     */
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    fun save(
        @Valid @RequestBody member: FamilyMember,
    ): ResponseEntity<*> {
        return when (val result = standardizedFamilyMemberService.save(member)) {
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
    }

    /**
     * Standardized entity update - PUT /api/family-members/std/{familyMemberId}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @PutMapping("/std/{familyMemberId}", consumes = ["application/json"], produces = ["application/json"])
    fun update(
        @PathVariable familyMemberId: Long,
        @Valid @RequestBody member: FamilyMember,
    ): ResponseEntity<*> {
        member.familyMemberId = familyMemberId
        return when (val result = standardizedFamilyMemberService.update(member)) {
            is ServiceResult.Success -> {
                logger.info("Family member updated successfully: $familyMemberId")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Family member not found for update: $familyMemberId")
                ResponseEntity.notFound().build<Any>()
            }
            is ServiceResult.ValidationError -> {
                logger.warn("Validation error updating family member: ${result.errors}")
                ResponseEntity.badRequest().body(mapOf("errors" to result.errors))
            }
            is ServiceResult.BusinessError -> {
                logger.warn("Business error updating family member: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to result.message))
            }
            is ServiceResult.SystemError -> {
                logger.error("System error updating family member $familyMemberId: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Internal server error"))
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Any>()
            }
        }
    }

    /**
     * Standardized entity deletion - DELETE /api/family-members/std/{familyMemberId}
     * Returns 200 OK with success message
     */
    @DeleteMapping("/std/{familyMemberId}", produces = ["application/json"])
    fun deleteById(
        @PathVariable familyMemberId: Long,
    ): ResponseEntity<*> {
        return when (val result = standardizedFamilyMemberService.deleteById(familyMemberId)) {
            is ServiceResult.Success -> {
                logger.info("Family member deleted successfully: $familyMemberId")
                ResponseEntity.ok(mapOf("message" to "Family member deleted successfully"))
            }
            is ServiceResult.NotFound -> {
                logger.warn("Family member not found for deletion: $familyMemberId")
                ResponseEntity.notFound().build<Any>()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error deleting family member $familyMemberId: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Internal server error"))
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Any>()
            }
        }
    }

    // ===== LEGACY ENDPOINTS (BACKWARD COMPATIBILITY) =====

    /**
     * Legacy endpoint - GET /api/family-members
     * Maintains original behavior
     */
    @GetMapping(produces = ["application/json"])
    fun getAll(): ResponseEntity<List<FamilyMember>> =
        ResponseEntity.ok(standardizedFamilyMemberService.findAll())

    /**
     * Legacy endpoint - GET /api/family-members/all
     * Maintains original behavior
     */
    @GetMapping("/all", produces = ["application/json"])
    fun getAllWithSuffix(): ResponseEntity<List<FamilyMember>> =
        ResponseEntity.ok(standardizedFamilyMemberService.findAll())

    /**
     * Legacy endpoint - POST /api/family-members/insert
     * Maintains original behavior
     */
    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insert(
        @RequestBody member: FamilyMember,
    ): ResponseEntity<FamilyMember> {
        return try {
            logger.info("Inserting family member: ${member.memberName} for owner: ${member.owner} (legacy endpoint)")
            val result = standardizedFamilyMemberService.insertFamilyMember(member)
            logger.info("Family member inserted successfully: ${result.familyMemberId}")
            ResponseEntity.status(HttpStatus.CREATED).body(result)
        } catch (ex: DataIntegrityViolationException) {
            logger.error("Failed to insert family member due to data integrity violation: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate family member")
        } catch (ex: Exception) {
            logger.error("Failed to insert family member: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.message, ex)
        }
    }

    /**
     * Legacy endpoint - GET /api/family-members/{id}
     * Maintains original behavior (note: conflicts with standardized /{familyMemberId})
     * Spring will resolve this based on order and specificity
     */
    @GetMapping("/{id}", produces = ["application/json"])
    fun getById(
        @PathVariable id: Long,
    ): ResponseEntity<FamilyMember> {
        return try {
            logger.debug("Retrieving family member: $id (legacy endpoint)")
            val member =
                standardizedFamilyMemberService.findById(id)
                    ?: return ResponseEntity.notFound().build<FamilyMember>()
            logger.info("Retrieved family member: $id")
            ResponseEntity.ok(member)
        } catch (ex: Exception) {
            logger.error("Failed to retrieve family member $id: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve family member: ${ex.message}", ex)
        }
    }

    // ===== BUSINESS LOGIC ENDPOINTS (PRESERVED) =====

    /**
     * Business logic endpoint - GET /api/family-members/owner/{owner}
     * Preserved as-is, not part of standardization
     */
    @GetMapping("/owner/{owner}", produces = ["application/json"])
    fun byOwner(
        @PathVariable owner: String,
    ): ResponseEntity<List<FamilyMember>> =
        ResponseEntity.ok(standardizedFamilyMemberService.findByOwner(owner))

    /**
     * Business logic endpoint - GET /api/family-members/owner/{owner}/relationship/{relationship}
     * Preserved as-is, not part of standardization
     */
    @GetMapping("/owner/{owner}/relationship/{relationship}", produces = ["application/json"])
    fun byOwnerAndRelationship(
        @PathVariable owner: String,
        @PathVariable relationship: FamilyRelationship,
    ): ResponseEntity<List<FamilyMember>> =
        ResponseEntity.ok(standardizedFamilyMemberService.findByOwnerAndRelationship(owner, relationship))

    /**
     * Business logic endpoint - PUT /api/family-members/{id}/activate
     * Preserved as-is, not part of standardization
     */
    @PutMapping("/{id}/activate")
    fun activateMember(
        @PathVariable id: Long,
    ): ResponseEntity<Map<String, String>> {
        return try {
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
    }

    /**
     * Business logic endpoint - PUT /api/family-members/{id}/deactivate
     * Preserved as-is, not part of standardization
     */
    @PutMapping("/{id}/deactivate")
    fun deactivateMember(
        @PathVariable id: Long,
    ): ResponseEntity<Map<String, String>> {
        return try {
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
    }

    /**
     * Business logic endpoint - DELETE /api/family-members/{id}
     * Preserved as-is, soft delete functionality
     * Note: This conflicts with standardized deleteById - Spring will handle resolution
     */
    @DeleteMapping("/{id}")
    fun softDelete(
        @PathVariable id: Long,
    ): ResponseEntity<Map<String, String>> {
        return try {
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
}
