package finance.controllers

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import finance.helpers.SmartFamilyMemberBuilder
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("func")
class FamilyMemberControllerFunctionalSpec extends BaseControllerFunctionalSpec {

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
        given:
        def post = postMember(testOwner, "${testOwner}-child", FamilyRelationship.Child)
        Long id = extractLong(post.body, 'familyMemberId')

        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> deactivateResponse
        try {
            deactivateResponse = restTemplate.exchange(
                createURLWithPort("/api/${ENDPOINT}/${id}/deactivate"),
                HttpMethod.PUT, entity, String)
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            deactivateResponse = new ResponseEntity<>(ex.getResponseBodyAsString(), ex.getResponseHeaders(), ex.getStatusCode())
        }

        ResponseEntity<String> deleteResponse
        try {
            deleteResponse = restTemplate.exchange(
                createURLWithPort("/api/${ENDPOINT}/${id}"),
                HttpMethod.DELETE, entity, String)
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            deleteResponse = new ResponseEntity<>(ex.getResponseBodyAsString(), ex.getResponseHeaders(), ex.getStatusCode())
        }

        then:
        post.statusCode == HttpStatus.CREATED
        deactivateResponse.statusCode == HttpStatus.OK
        deleteResponse.statusCode == HttpStatus.OK
    }

    void 'should retrieve all family members'() {
        when:
        def post1 = postMember(testOwner, "${testOwner}-member1", FamilyRelationship.Self)
        def post2 = postMember(testOwner, "${testOwner}-member2", FamilyRelationship.Spouse)
        def post3 = postMember(testOwner, "${testOwner}-member3", FamilyRelationship.Child)
        def getAll = restTemplate.exchange(createURLWithPort("/api/${ENDPOINT}"), HttpMethod.GET, new HttpEntity<>(null, headers), String)

        then:
        post1.statusCode == HttpStatus.CREATED
        post2.statusCode == HttpStatus.CREATED
        post3.statusCode == HttpStatus.CREATED
        getAll.statusCode == HttpStatus.OK
        getAll.body.contains("${testOwner}-member1")
        getAll.body.contains("${testOwner}-member2")
        getAll.body.contains("${testOwner}-member3")
    }

    private Long extractLong(String json, String field) {
        def m = (json =~ /\"${field}\":(\d+)/)
        return m ? Long.parseLong(m[0][1]) : 0L
    }
}
