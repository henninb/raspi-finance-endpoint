package finance.controllers

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import finance.services.IFamilyMemberService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import jakarta.validation.Valid

@CrossOrigin
@RestController
@RequestMapping("/api/family-members")
open class FamilyMemberController(private val familyMemberService: IFamilyMemberService) :
    StandardizedBaseController(), StandardRestController<FamilyMember, Long> {

    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/family-members/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     */
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<FamilyMember>> {
        return handleCrudOperation("Find all active family members", null) {
            logger.debug("Retrieving all active family members")
            val members: List<FamilyMember> = familyMemberService.findAll()
            logger.info("Retrieved ${members.size} active family members")
            members
        }
    }

    /**
     * Standardized single entity retrieval - GET /api/family-members/std/{familyMemberId}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @GetMapping("/std/{familyMemberId}", produces = ["application/json"])
    override fun findById(@PathVariable familyMemberId: Long): ResponseEntity<FamilyMember> {
        return handleCrudOperation("Find family member by ID", familyMemberId) {
            logger.debug("Retrieving family member: $familyMemberId")
            val member = familyMemberService.findById(familyMemberId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Family member not found: $familyMemberId")
            logger.info("Retrieved family member: $familyMemberId")
            member
        }
    }

    /**
     * Standardized entity creation - POST /api/family-members
     * Returns 201 CREATED
     */
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(@Valid @RequestBody member: FamilyMember): ResponseEntity<FamilyMember> {
        return handleCreateOperation("FamilyMember", member.familyMemberId) {
            logger.info("Creating family member: ${member.memberName} for owner: ${member.owner}")
            val result = familyMemberService.insertFamilyMember(member)
            logger.info("Family member created successfully: ${result.familyMemberId}")
            result
        }
    }

    /**
     * Standardized entity update - PUT /api/family-members/std/{familyMemberId}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @PutMapping("/std/{familyMemberId}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(@PathVariable familyMemberId: Long, @Valid @RequestBody member: FamilyMember): ResponseEntity<FamilyMember> {
        return handleCrudOperation("Update family member", familyMemberId) {
            logger.info("Updating family member: $familyMemberId")
            // Validate family member exists first
            familyMemberService.findById(familyMemberId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Family member not found: $familyMemberId")
            val result = familyMemberService.updateFamilyMember(member)
            logger.info("Family member updated successfully: $familyMemberId")
            result
        }
    }

    /**
     * Standardized entity deletion - DELETE /api/family-members/std/{familyMemberId}
     * Returns 200 OK with deleted entity
     */
    @DeleteMapping("/std/{familyMemberId}", produces = ["application/json"])
    override fun deleteById(@PathVariable familyMemberId: Long): ResponseEntity<FamilyMember> {
        return handleDeleteOperation(
            "FamilyMember",
            familyMemberId,
            {
                val member = familyMemberService.findById(familyMemberId)
                if (member != null) java.util.Optional.of(member) else java.util.Optional.empty()
            },
            { familyMemberService.softDelete(familyMemberId) }
        )
    }

    // ===== LEGACY ENDPOINTS (BACKWARD COMPATIBILITY) =====

    /**
     * Legacy endpoint - GET /api/family-members
     * Maintains original behavior
     */
    @GetMapping(produces = ["application/json"])
    fun getAll(): ResponseEntity<List<FamilyMember>> =
        ResponseEntity.ok(familyMemberService.findAll())

    /**
     * Legacy endpoint - GET /api/family-members/all
     * Maintains original behavior
     */
    @GetMapping("/all", produces = ["application/json"])
    fun getAllWithSuffix(): ResponseEntity<List<FamilyMember>> =
        ResponseEntity.ok(familyMemberService.findAll())

    /**
     * Legacy endpoint - POST /api/family-members/insert
     * Maintains original behavior
     */
    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insert(@RequestBody member: FamilyMember): ResponseEntity<FamilyMember> {
        return try {
            logger.info("Inserting family member: ${member.memberName} for owner: ${member.owner} (legacy endpoint)")
            val result = familyMemberService.insertFamilyMember(member)
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
    fun getById(@PathVariable id: Long): ResponseEntity<FamilyMember> {
        return try {
            logger.debug("Retrieving family member: $id (legacy endpoint)")
            val member = familyMemberService.findById(id)
                ?: return ResponseEntity.notFound().build()
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
    fun byOwner(@PathVariable owner: String): ResponseEntity<List<FamilyMember>> =
        ResponseEntity.ok(familyMemberService.findByOwner(owner))

    /**
     * Business logic endpoint - GET /api/family-members/owner/{owner}/relationship/{relationship}
     * Preserved as-is, not part of standardization
     */
    @GetMapping("/owner/{owner}/relationship/{relationship}", produces = ["application/json"])
    fun byOwnerAndRelationship(
        @PathVariable owner: String,
        @PathVariable relationship: FamilyRelationship
    ): ResponseEntity<List<FamilyMember>> =
        ResponseEntity.ok(familyMemberService.findByOwnerAndRelationship(owner, relationship))

    /**
     * Business logic endpoint - PUT /api/family-members/{id}/activate
     * Preserved as-is, not part of standardization
     */
    @PutMapping("/{id}/activate")
    fun activateMember(@PathVariable id: Long): ResponseEntity<Map<String, String>> {
        return try {
            logger.info("Activating family member: $id")
            val ok = familyMemberService.updateActiveStatus(id, true)
            if (ok) {
                logger.info("Family member activated successfully: $id")
                ResponseEntity.ok(mapOf("message" to "Family member activated"))
            } else {
                logger.warn("Family member not found for activation: $id")
                ResponseEntity.notFound().build()
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
    fun deactivateMember(@PathVariable id: Long): ResponseEntity<Map<String, String>> {
        return try {
            logger.info("Deactivating family member: $id")
            val ok = familyMemberService.updateActiveStatus(id, false)
            if (ok) {
                logger.info("Family member deactivated successfully: $id")
                ResponseEntity.ok(mapOf("message" to "Family member deactivated"))
            } else {
                logger.warn("Family member not found for deactivation: $id")
                ResponseEntity.notFound().build()
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
    fun softDelete(@PathVariable id: Long): ResponseEntity<Map<String, String>> {
        return try {
            logger.info("Soft deleting family member: $id")
            val ok = familyMemberService.softDelete(id)
            if (ok) {
                logger.info("Family member soft deleted successfully: $id")
                ResponseEntity.ok(mapOf("message" to "Family member deleted successfully"))
            } else {
                logger.warn("Family member not found for deletion: $id")
                ResponseEntity.notFound().build()
            }
        } catch (ex: Exception) {
            logger.error("Failed to soft delete family member $id: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete family member: ${ex.message}", ex)
        }
    }
}

