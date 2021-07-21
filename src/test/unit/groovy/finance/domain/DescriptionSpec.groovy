package finance.domain

class DescriptionSpec extends BaseDomainSpec {
    protected String jsonPayload = '{"description":"bar", "activeStatus":true}'

    void 'test -- JSON serialization to Description'() {

        when:
        Description description = mapper.readValue(jsonPayload, Description)

        then:
        description.description == "bar"
        0 * _
    }
}
