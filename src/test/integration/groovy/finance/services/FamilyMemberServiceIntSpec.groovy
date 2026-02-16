package finance.services

import finance.Application
import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import finance.domain.ServiceResult
import finance.repositories.FamilyMemberRepository
import jakarta.validation.ValidationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

import java.sql.Timestamp

@ActiveProfiles("int")
@SpringBootTest
@ContextConfiguration(classes = Application)
@Transactional
class FamilyMemberServiceIntSpec extends Specification {

    private static final String TEST_OWNER = "owner_filter"

    @Autowired
    FamilyMemberService familyMemberService

    @Autowired
    FamilyMemberRepository familyMemberRepository

    void setup() {
        setSecurityContext(TEST_OWNER)
    }

    void cleanup() {
        SecurityContextHolder.clearContext()
    }

    private void setSecurityContext(String username) {
        def authorities = [new SimpleGrantedAuthority("USER")]
        def auth = new UsernamePasswordAuthenticationToken(username, "N/A", authorities)
        SecurityContextHolder.getContext().setAuthentication(auth)
    }

    void 'test findAllActive returns active family members'() {
        given:
        FamilyMember member1 = createFamilyMember(TEST_OWNER, "John", FamilyRelationship.Spouse, true)
        FamilyMember member2 = createFamilyMember(TEST_OWNER, "Jane", FamilyRelationship.Child, true)
        FamilyMember inactiveMember = createFamilyMember(TEST_OWNER, "Inactive", FamilyRelationship.Child, false)

        familyMemberRepository.save(member1)
        familyMemberRepository.save(member2)
        familyMemberRepository.save(inactiveMember)

        when:
        ServiceResult<List<FamilyMember>> result = familyMemberService.findAllActive()

        then:
        result instanceof ServiceResult.Success
        def members = (result as ServiceResult.Success<List<FamilyMember>>).data
        members.size() >= 2
        members.every { it.activeStatus == true }
    }

    void 'test save family member with valid data'() {
        given:
        FamilyMember newMember = createFamilyMember(TEST_OWNER, "TestMember", FamilyRelationship.Spouse, true)
        newMember.familyMemberId = 0L

        when:
        ServiceResult<FamilyMember> result = familyMemberService.save(newMember)

        then:
        result instanceof ServiceResult.Success
        def savedMember = (result as ServiceResult.Success<FamilyMember>).data
        savedMember.familyMemberId != null
        savedMember.memberName == "TestMember"
        savedMember.owner == TEST_OWNER
        savedMember.relationship == FamilyRelationship.Spouse
    }

    void 'test save family member with duplicate prevents creation'() {
        given:
        FamilyMember member1 = createFamilyMember(TEST_OWNER, "DupMember", FamilyRelationship.Spouse, true)
        member1.familyMemberId = 0L
        familyMemberService.save(member1)

        FamilyMember member2 = createFamilyMember(TEST_OWNER, "DupMember", FamilyRelationship.Spouse, true)
        member2.familyMemberId = 0L

        when:
        ServiceResult<FamilyMember> result = familyMemberService.save(member2)

        then:
        result instanceof ServiceResult.BusinessError
    }

    void 'test findByIdServiceResult returns existing family member'() {
        given:
        FamilyMember member = createFamilyMember(TEST_OWNER, "FindMe", FamilyRelationship.Child, true)
        member.familyMemberId = 0L
        ServiceResult<FamilyMember> saveResult = familyMemberService.save(member)
        def savedId = (saveResult as ServiceResult.Success<FamilyMember>).data.familyMemberId

        when:
        ServiceResult<FamilyMember> result = familyMemberService.findByIdServiceResult(savedId)

        then:
        result instanceof ServiceResult.Success
        def foundMember = (result as ServiceResult.Success<FamilyMember>).data
        foundMember.memberName == "FindMe"
    }

    void 'test findByIdServiceResult returns not found for non-existent member'() {
        when:
        ServiceResult<FamilyMember> result = familyMemberService.findByIdServiceResult(999999L)

        then:
        result instanceof ServiceResult.NotFound
    }

    void 'test update family member'() {
        given:
        FamilyMember member = createFamilyMember(TEST_OWNER, "UpdateMe", FamilyRelationship.Spouse, true)
        member.familyMemberId = 0L
        ServiceResult<FamilyMember> saveResult = familyMemberService.save(member)
        FamilyMember savedMember = (saveResult as ServiceResult.Success<FamilyMember>).data

        savedMember.memberName = "UpdatedName"
        savedMember.relationship = FamilyRelationship.Child

        when:
        ServiceResult<FamilyMember> updateResult = familyMemberService.update(savedMember)

        then:
        updateResult instanceof ServiceResult.Success
        def updatedMember = (updateResult as ServiceResult.Success<FamilyMember>).data
        updatedMember.memberName == "UpdatedName"
        updatedMember.relationship == FamilyRelationship.Child
    }

    void 'test deleteById soft deletes family member'() {
        given:
        FamilyMember member = createFamilyMember(TEST_OWNER, "DeleteMe", FamilyRelationship.Child, true)
        member.familyMemberId = 0L
        ServiceResult<FamilyMember> saveResult = familyMemberService.save(member)
        def savedId = (saveResult as ServiceResult.Success<FamilyMember>).data.familyMemberId

        when:
        ServiceResult<Boolean> deleteResult = familyMemberService.deleteById(savedId)

        then:
        deleteResult instanceof ServiceResult.Success
        (deleteResult as ServiceResult.Success<Boolean>).data == true

        and:
        ServiceResult<FamilyMember> findResult = familyMemberService.findByIdServiceResult(savedId)
        findResult instanceof ServiceResult.NotFound
    }

    void 'test insertFamilyMember legacy method'() {
        given:
        FamilyMember member = createFamilyMember(TEST_OWNER, "LegacyInsert", FamilyRelationship.Spouse, true)
        member.familyMemberId = 0L

        when:
        FamilyMember savedMember = familyMemberService.insertFamilyMember(member)

        then:
        savedMember != null
        savedMember.familyMemberId != null
        savedMember.memberName == "LegacyInsert"
    }

    void 'test insertFamilyMember throws exception for duplicate'() {
        given:
        FamilyMember member1 = createFamilyMember(TEST_OWNER, "DupLegacy", FamilyRelationship.Spouse, true)
        member1.familyMemberId = 0L
        familyMemberService.insertFamilyMember(member1)

        FamilyMember member2 = createFamilyMember(TEST_OWNER, "DupLegacy", FamilyRelationship.Spouse, true)
        member2.familyMemberId = 0L

        when:
        familyMemberService.insertFamilyMember(member2)

        then:
        thrown(DataIntegrityViolationException)
    }

    void 'test findByOwner returns members for owner'() {
        given:
        setSecurityContext("owner_filter")
        FamilyMember member1 = createFamilyMember("owner_filter", "Member1", FamilyRelationship.Spouse, true)
        FamilyMember member2 = createFamilyMember("owner_filter", "Member2", FamilyRelationship.Child, true)
        FamilyMember otherOwner = createFamilyMember("other_owner", "Member3", FamilyRelationship.Spouse, true)

        familyMemberRepository.save(member1)
        familyMemberRepository.save(member2)
        familyMemberRepository.save(otherOwner)

        when:
        List<FamilyMember> result = familyMemberService.findByOwner()

        then:
        result.size() == 2
        result.every { it.owner == "owner_filter" }
    }

    void 'test findByOwnerAndRelationship filters correctly'() {
        given:
        setSecurityContext("owner_rel")
        FamilyMember spouse = createFamilyMember("owner_rel", "Spouse1", FamilyRelationship.Spouse, true)
        FamilyMember child1 = createFamilyMember("owner_rel", "Child1", FamilyRelationship.Child, true)
        FamilyMember child2 = createFamilyMember("owner_rel", "Child2", FamilyRelationship.Child, true)

        familyMemberRepository.save(spouse)
        familyMemberRepository.save(child1)
        familyMemberRepository.save(child2)

        when:
        List<FamilyMember> children = familyMemberService.findByOwnerAndRelationship(FamilyRelationship.Child)
        List<FamilyMember> spouses = familyMemberService.findByOwnerAndRelationship(FamilyRelationship.Spouse)

        then:
        children.size() == 2
        children.every { it.relationship == FamilyRelationship.Child }
        spouses.size() == 1
        spouses[0].relationship == FamilyRelationship.Spouse
    }

    void 'test updateActiveStatus changes status'() {
        given:
        FamilyMember member = createFamilyMember(TEST_OWNER, "StatusMember", FamilyRelationship.Spouse, true)
        member.familyMemberId = 0L
        ServiceResult<FamilyMember> saveResult = familyMemberService.save(member)
        def savedId = (saveResult as ServiceResult.Success<FamilyMember>).data.familyMemberId

        when:
        boolean result = familyMemberService.updateActiveStatus(savedId, false)

        then:
        result == true

        and:
        def updatedMember = familyMemberRepository.findById(savedId).orElse(null)
        updatedMember.activeStatus == false
    }

    void 'test softDelete marks member as inactive'() {
        given:
        FamilyMember member = createFamilyMember(TEST_OWNER, "SoftDelete", FamilyRelationship.Child, true)
        member.familyMemberId = 0L
        ServiceResult<FamilyMember> saveResult = familyMemberService.save(member)
        def savedId = (saveResult as ServiceResult.Success<FamilyMember>).data.familyMemberId

        when:
        boolean result = familyMemberService.softDelete(savedId)

        then:
        result == true

        and:
        def deletedMember = familyMemberRepository.findById(savedId).orElse(null)
        deletedMember != null
        deletedMember.activeStatus == false
    }

    void 'test findAll returns all active members'() {
        given:
        FamilyMember member1 = createFamilyMember(TEST_OWNER, "All1", FamilyRelationship.Spouse, true)
        FamilyMember member2 = createFamilyMember(TEST_OWNER, "All2", FamilyRelationship.Child, true)
        familyMemberRepository.save(member1)
        familyMemberRepository.save(member2)

        when:
        List<FamilyMember> allMembers = familyMemberService.findAll()

        then:
        allMembers.size() >= 2
        allMembers.every { it.activeStatus == true }
    }

    private FamilyMember createFamilyMember(String owner, String name, FamilyRelationship relationship, boolean active) {
        FamilyMember member = new FamilyMember()
        member.owner = owner
        member.memberName = name
        member.relationship = relationship
        member.activeStatus = active
        member.dateAdded = new Timestamp(System.currentTimeMillis())
        member.dateUpdated = new Timestamp(System.currentTimeMillis())
        return member
    }
}
