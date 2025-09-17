package finance.controllers

import finance.domain.Description
import finance.domain.MergeDescriptionsRequest
import finance.services.DescriptionService
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification

class DescriptionControllerMergeAndDeleteSpec extends Specification {

    def service = Mock(DescriptionService)
    def controller = new DescriptionController(service)

    void "deleteByDescription legacy returns 404 then 200"() {
        when:
        controller.deleteByDescription('gone')

        then:
        1 * service.findByDescriptionName('gone') >> Optional.empty()
        thrown(ResponseStatusException)

        when:
        def resp = controller.deleteByDescription('present')

        then:
        1 * service.findByDescriptionName('present') >> Optional.of(new Description(descriptionName: 'present'))
        1 * service.deleteByDescriptionName('present') >> true
        resp.statusCode == HttpStatus.OK
    }

    void "mergeDescriptions returns 400 on invalid request and 200 on success"() {
        when:
        controller.mergeDescriptions(new MergeDescriptionsRequest([], ''))

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST

        when:
        def resp = controller.mergeDescriptions(new MergeDescriptionsRequest(['a','b'], 't'))

        then:
        1 * service.mergeDescriptions('t', ['a','b']) >> new Description(descriptionName: 't')
        resp.statusCode == HttpStatus.OK
        resp.body.descriptionName == 't'
    }
}

