package finance.controllers

import finance.domain.Description
import finance.services.DescriptionService
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification

class DescriptionControllerMoreSpec extends Specification {

    def service = Mock(DescriptionService)
    def controller = new DescriptionController(service)

    void "findAllActive returns 200 with list (standardized)"() {
        when:
        def resp = controller.findAllActive()

        then:
        1 * service.fetchAllDescriptions() >> [new Description(descriptionName: 'x')]
        resp.statusCode == HttpStatus.OK
        resp.body*.descriptionName == ['x']
    }

    void "findById returns 404 then 200 (standardized)"() {
        when:
        controller.findById('missing')

        then:
        1 * service.findByDescriptionName('missing') >> Optional.empty()
        thrown(ResponseStatusException)

        when:
        def resp = controller.findById('found')

        then:
        1 * service.findByDescriptionName('found') >> Optional.of(new Description(descriptionName: 'found'))
        resp.statusCode == HttpStatus.OK
    }

    void "save returns 201 (standardized)"() {
        given:
        def d = new Description(descriptionName: 'y')

        when:
        def resp = controller.save(d)

        then:
        1 * service.insertDescription(d) >> d
        resp.statusCode == HttpStatus.CREATED
    }

    void "update returns 404 then 200 (standardized)"() {
        given:
        def d = new Description(descriptionName: 'a')

        when:
        controller.update('a', d)

        then:
        1 * service.findByDescriptionName('a') >> Optional.empty()
        thrown(ResponseStatusException)

        when:
        def resp = controller.update('a', d)

        then:
        1 * service.findByDescriptionName('a') >> Optional.of(d)
        1 * service.updateDescription(d) >> d
        resp.statusCode == HttpStatus.OK
    }

    void "selectAllDescriptions legacy returns 200 or 500"() {
        when:
        def ok = controller.selectAllDescriptions()

        then:
        1 * service.fetchAllDescriptions() >> []
        ok.statusCode == HttpStatus.OK

        when:
        controller.selectAllDescriptions()

        then:
        1 * service.fetchAllDescriptions() >> { throw new RuntimeException('boom') }
        thrown(ResponseStatusException)
    }

    void "updateDescription legacy returns 404 then 200"() {
        given:
        def d = new Description(descriptionName: 'z')

        when:
        controller.updateDescription('z', d)

        then:
        1 * service.findByDescriptionName('z') >> Optional.empty()
        thrown(ResponseStatusException)

        when:
        def resp = controller.updateDescription('z', d)

        then:
        1 * service.findByDescriptionName('z') >> Optional.of(d)
        1 * service.updateDescription(d) >> d
        resp.statusCode == HttpStatus.OK
    }

    void "selectDescriptionName legacy returns 404 then 200"() {
        when:
        controller.selectDescriptionName('n')

        then:
        1 * service.findByDescriptionName('n') >> Optional.empty()
        thrown(ResponseStatusException)

        when:
        def resp = controller.selectDescriptionName('m')

        then:
        1 * service.findByDescriptionName('m') >> Optional.of(new Description(descriptionName: 'm'))
        resp.statusCode == HttpStatus.OK
    }

    void "insertDescription legacy returns 201 and maps DI to 409 and validation to 400"() {
        given:
        def d = new Description(descriptionName: 'p')

        when:
        def resp = controller.insertDescription(d)

        then:
        1 * service.insertDescription(d) >> d
        resp.statusCode == HttpStatus.CREATED

        when:
        controller.insertDescription(d)

        then:
        1 * service.insertDescription(d) >> { throw new org.springframework.dao.DataIntegrityViolationException('dup') }
        thrown(ResponseStatusException)

        when:
        controller.insertDescription(d)

        then:
        1 * service.insertDescription(d) >> { throw new jakarta.validation.ValidationException('bad') }
        thrown(ResponseStatusException)
    }
}

