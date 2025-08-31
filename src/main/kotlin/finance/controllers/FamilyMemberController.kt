package finance.controllers

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import finance.services.IFamilyMemberService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@CrossOrigin
@RestController
@RequestMapping("/api/family-members", "/family-members")
open class FamilyMemberController(private val familyMemberService: IFamilyMemberService) : BaseController() {

    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insert(@RequestBody member: FamilyMember): ResponseEntity<FamilyMember> {
        return try {
            ResponseEntity.status(HttpStatus.CREATED).body(familyMemberService.insertFamilyMember(member))
        } catch (ex: DataIntegrityViolationException) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate family member")
        } catch (ex: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.message, ex)
        }
    }

    @GetMapping("/{id}", produces = ["application/json"])
    fun getById(@PathVariable id: Long): ResponseEntity<FamilyMember> {
        val member = familyMemberService.findById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(member)
    }

    @GetMapping(produces = ["application/json"])
    fun getAll(): ResponseEntity<List<FamilyMember>> =
        ResponseEntity.ok(familyMemberService.findAll())

    @GetMapping("/all", produces = ["application/json"])
    fun getAllWithSuffix(): ResponseEntity<List<FamilyMember>> =
        ResponseEntity.ok(familyMemberService.findAll())

    @GetMapping("/owner/{owner}", produces = ["application/json"])
    fun byOwner(@PathVariable owner: String): ResponseEntity<List<FamilyMember>> =
        ResponseEntity.ok(familyMemberService.findByOwner(owner))

    @GetMapping("/owner/{owner}/relationship/{relationship}", produces = ["application/json"])
    fun byOwnerAndRelationship(
        @PathVariable owner: String,
        @PathVariable relationship: FamilyRelationship
    ): ResponseEntity<List<FamilyMember>> =
        ResponseEntity.ok(familyMemberService.findByOwnerAndRelationship(owner, relationship))

    @PatchMapping("/{id}/active")
    fun updateActive(@PathVariable id: Long, @RequestParam active: Boolean): ResponseEntity<Map<String, String>> {
        val ok = familyMemberService.updateActiveStatus(id, active)
        return if (ok) ResponseEntity.ok(mapOf("message" to "Active status updated")) else ResponseEntity.notFound().build()
    }

    @DeleteMapping("/{id}")
    fun softDelete(@PathVariable id: Long): ResponseEntity<Map<String, String>> {
        val ok = familyMemberService.softDelete(id)
        return if (ok) ResponseEntity.ok(mapOf("message" to "Family member deleted successfully")) else ResponseEntity.notFound().build()
    }
}

