package finance.controllers

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import finance.helpers.SmartFamilyMemberBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("func")
class FamilyMemberControllerIsolatedSpec extends BaseControllerSpec {

    private static final String ENDPOINT = 'family-members'

    private ResponseEntity<String> postMember(String owner = testOwner, String name = null, FamilyRelationship rel = FamilyRelationship.Self) {
        def builder = SmartFamilyMemberBuilder.builderForOwner(owner)
                .asRelationship(rel)
        if (name) builder.withMemberName(name)
        FamilyMember member = builder.build()
        return insertEndpoint(ENDPOINT, member.toString())
    }

    void 'should create and retrieve family member by id'() {
        when:
        def post = postMember(testOwner, "${testOwner}", FamilyRelationship.Self)
        Long id = extractLong(post.body, 'familyMemberId')
        def get = restTemplate.exchange(createURLWithPort("/api/${ENDPOINT}/${id}"), HttpMethod.GET, new HttpEntity<>(null, headers), String)

        then:
        post.statusCode == HttpStatus.CREATED
        get.statusCode == HttpStatus.OK
        extractLong(get.body, 'familyMemberId') == id
    }

    void 'should list by owner and relationship'() {
        when:
        def post = postMember(testOwner, "${testOwner}-spouse", FamilyRelationship.Spouse)
        def listOwner = restTemplate.exchange(createURLWithPort("/api/${ENDPOINT}/owner/${testOwner}"), HttpMethod.GET, new HttpEntity<>(null, headers), String)
        def listRel = restTemplate.exchange(createURLWithPort("/api/${ENDPOINT}/owner/${testOwner}/relationship/Spouse"), HttpMethod.GET, new HttpEntity<>(null, headers), String)

        then:
        post.statusCode == HttpStatus.CREATED
        listOwner.statusCode == HttpStatus.OK
        listRel.statusCode == HttpStatus.OK
        listRel.body.toLowerCase().contains('spouse')
    }

    void 'should prevent duplicate family member for owner+name'() {
        when:
        String uniqueName = "${testOwner}-dup"
        def a = postMember(testOwner, uniqueName, FamilyRelationship.Self)
        def b = postMember(testOwner, uniqueName, FamilyRelationship.Self)

        then:
        a.statusCode == HttpStatus.CREATED
        b.statusCode == HttpStatus.CONFLICT
    }

    void 'should update active status and soft delete'() {
        when:
        def post = postMember(testOwner, "${testOwner}-child", FamilyRelationship.Child)
        Long id = extractLong(post.body, 'familyMemberId')
        def patch = restTemplate.exchange(createURLWithPort("/api/${ENDPOINT}/${id}/active?active=false"), HttpMethod.PATCH, new HttpEntity<>(null, headers), String)
        def del = restTemplate.exchange(createURLWithPort("/api/${ENDPOINT}/${id}"), HttpMethod.DELETE, new HttpEntity<>(null, headers), String)

        then:
        post.statusCode == HttpStatus.CREATED
        patch.statusCode == HttpStatus.OK
        del.statusCode == HttpStatus.OK
    }

    private Long extractLong(String json, String field) {
        def m = (json =~ /\"${field}\":(\d+)/)
        return m ? Long.parseLong(m[0][1]) : 0L
    }
}
