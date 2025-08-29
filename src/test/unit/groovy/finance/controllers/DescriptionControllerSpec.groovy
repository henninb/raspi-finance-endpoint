package finance.controllers

import finance.domain.Description
import finance.services.DescriptionService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification
import spock.lang.Subject

class DescriptionControllerSpec extends Specification {

    DescriptionService descriptionService = GroovyMock(DescriptionService)

    @Subject
    DescriptionController controller = new DescriptionController(descriptionService)

    def "selectAllDescriptions returns OK and payload"() {
        given:
        List<Description> list = [new Description(descriptionId: 1L, activeStatus: true, descriptionName: 'amazon')]

        when:
        ResponseEntity<List<Description>> response = controller.selectAllDescriptions()

        then:
        1 * descriptionService.fetchAllDescriptions() >> list
        response.statusCode == HttpStatus.OK
        response.body == list
    }

    def "selectAllDescriptions maps service failure to 500"() {
        when:
        controller.selectAllDescriptions()

        then:
        1 * descriptionService.fetchAllDescriptions() >> { throw new RuntimeException('boom') }
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "selectDescriptionName returns item or NOT_FOUND"() {
        when:
        ResponseEntity<Description> response = controller.selectDescriptionName('amazon')

        then:
        1 * descriptionService.findByDescriptionName('amazon') >> Optional.of(new Description(descriptionId: 11L, activeStatus: true, descriptionName: 'amazon'))
        response.statusCode == HttpStatus.OK

        when:
        controller.selectDescriptionName('missing')

        then:
        1 * descriptionService.findByDescriptionName('missing') >> Optional.empty()
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
        ex.reason == 'Description not found: missing'
    }

    def "insertDescription returns CREATED on success"() {
        given:
        def input = new Description(descriptionId: 0L, activeStatus: true, descriptionName: 'new')
        def created = new Description(descriptionId: 1L, activeStatus: true, descriptionName: 'new')

        when:
        ResponseEntity<Description> response = controller.insertDescription(input)

        then:
        1 * descriptionService.insertDescription(input) >> created
        response.statusCode == HttpStatus.CREATED
        response.body == created
    }

    def "insertDescription maps DataIntegrityViolationException to conflict"() {
        given:
        def desc = new Description(descriptionId: 0L, activeStatus: true, descriptionName: 'dupe')

        when:
        controller.insertDescription(desc)

        then:
        1 * descriptionService.insertDescription(desc) >> { throw new DataIntegrityViolationException('dupe') }
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.CONFLICT
        ex.reason == 'Duplicate description found.'
    }

    def "updateDescription returns OK when found"() {
        given:
        def existing = new Description(descriptionId: 3L, activeStatus: true, descriptionName: 'existing')
        def payload = new Description(descriptionId: 3L, activeStatus: true, descriptionName: 'updated')
        def updated = new Description(descriptionId: 3L, activeStatus: true, descriptionName: 'updated')

        when:
        ResponseEntity<Description> response = controller.updateDescription('existing', payload)

        then:
        1 * descriptionService.findByDescriptionName('existing') >> Optional.of(existing)
        1 * descriptionService.updateDescription(payload) >> updated
        response.statusCode == HttpStatus.OK
        response.body == updated
    }

    def "updateDescription NOT_FOUND when missing"() {
        given:
        def payload = new Description(descriptionId: 3L, activeStatus: true, descriptionName: 'w')

        when:
        controller.updateDescription('nope', payload)

        then:
        1 * descriptionService.findByDescriptionName('nope') >> Optional.empty()
        0 * descriptionService.updateDescription(_)
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
        ex.reason == 'Description not found: nope'
    }

    def "deleteByDescription returns OK when exists"() {
        given:
        def existing = new Description(descriptionId: 44L, activeStatus: true, descriptionName: 'old')

        when:
        ResponseEntity<Description> response = controller.deleteByDescription('old')

        then:
        1 * descriptionService.findByDescriptionName('old') >> Optional.of(existing)
        1 * descriptionService.deleteByDescriptionName('old')
        response.statusCode == HttpStatus.OK
        response.body == existing
    }

    def "deleteByDescription returns NOT_FOUND when missing"() {
        when:
        controller.deleteByDescription('missing')

        then:
        1 * descriptionService.findByDescriptionName('missing') >> Optional.empty()
        0 * descriptionService.deleteByDescriptionName(_)
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
        ex.reason == 'Description not found: missing'
    }

def "mergeDescriptions returns OK with merged description"() {
        given:
        def payload = new finance.controllers.MergeDescriptionsRequest(["a","b"], "merged")
        def merged = new Description(descriptionId: 99L, activeStatus: true, descriptionName: 'merged')

        when:
        ResponseEntity<Description> response = controller.mergeDescriptions(payload)

        then:
        1 * descriptionService.mergeDescriptions('merged', ["a","b"]) >> merged
        response.statusCode == HttpStatus.OK
        response.body == merged
    }

    def "mergeDescriptions maps service failure to 500"() {
        given:
        def payload = new finance.controllers.MergeDescriptionsRequest(["x"], "y")

        when:
        controller.mergeDescriptions(payload)

        then:
        1 * descriptionService.mergeDescriptions('y', ["x"]) >> { throw new RuntimeException('boom') }
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }
}
