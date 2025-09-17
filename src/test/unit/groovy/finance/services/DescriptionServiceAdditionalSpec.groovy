package finance.services

import finance.domain.Description
import finance.helpers.DescriptionBuilder

class DescriptionServiceAdditionalSpec extends BaseServiceSpec {

    void setup() {
        descriptionService.validator = validatorMock
        descriptionService.meterService = meterService
    }

    void "fetchAllDescriptions returns list from repo"() {
        given:
        def d1 = DescriptionBuilder.builder().withDescription('a').build()
        def d2 = DescriptionBuilder.builder().withDescription('b').build()

        when:
        def list = descriptionService.fetchAllDescriptions()

        then:
        1 * descriptionRepositoryMock.findByActiveStatusOrderByDescriptionName(true) >> [d1, d2]
        list*.descriptionName == ['a','b']
    }

    void "findByDescriptionName present and empty"() {
        when:
        def present = descriptionService.findByDescriptionName('x')
        def empty = descriptionService.findByDescriptionName('missing')

        then:
        1 * descriptionRepositoryMock.findByDescriptionName('x') >> Optional.of(DescriptionBuilder.builder().withDescription('x').build())
        1 * descriptionRepositoryMock.findByDescriptionName('missing') >> Optional.empty()
        present.isPresent()
        empty.isEmpty()
    }

    void "description(name) present and empty"() {
        when:
        def present = descriptionService.description('y')
        def empty = descriptionService.description('zzz')

        then:
        1 * descriptionRepositoryMock.findByDescriptionName('y') >> Optional.of(DescriptionBuilder.builder().withDescription('y').build())
        1 * descriptionRepositoryMock.findByDescriptionName('zzz') >> Optional.empty()
        present.isPresent()
        empty.isEmpty()
    }

    void "deleteByDescriptionName returns false when not found"() {
        when:
        def result = descriptionService.deleteByDescriptionName('missing')

        then:
        1 * descriptionRepositoryMock.findByDescriptionName('missing') >> Optional.empty()
        !result
    }
}

