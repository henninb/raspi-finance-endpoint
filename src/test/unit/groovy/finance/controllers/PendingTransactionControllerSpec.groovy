package finance.controllers

import finance.domain.PendingTransaction
import finance.repositories.PendingTransactionRepository
import finance.services.StandardizedPendingTransactionService
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification

import java.sql.Date
import java.time.LocalDate
import java.math.BigDecimal
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import jakarta.validation.Validation

class PendingTransactionControllerSpec extends Specification {

    PendingTransactionRepository repo = Mock()
    StandardizedPendingTransactionService service = new StandardizedPendingTransactionService(repo)
    def controller = new PendingTransactionController(service)

    def setup() {
        // Inject dependencies normally autowired by Spring
        service.meterService = new finance.services.MeterService(new SimpleMeterRegistry())
        service.validator = Validation.buildDefaultValidatorFactory().validator
    }

    private PendingTransaction validPending(Long id = 0L) {
        return new PendingTransaction(
                id,
                "abc_owner",
                Date.valueOf(LocalDate.now()),
                "Test Description",
                new BigDecimal("12.34"),
                "pending",
                "owner1",
                null
        )
    }

    void "findAllActive returns list and 200"() {
        given:
        def p = validPending(0L)
        repo.findAll() >> [p]

        when:
        def resp = controller.findAllActive()

        then:
        resp.statusCode == HttpStatus.OK
        resp.body.size() == 1
    }

    void "findById not found throws 404"() {
        given:
        repo.findByPendingTransactionIdOrderByTransactionDateDesc(99L) >> Optional.empty()

        when:
        controller.findById(99L)

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
    }

    void "findById success returns 200 and body"() {
        given:
        def p = validPending(7L)
        repo.findByPendingTransactionIdOrderByTransactionDateDesc(7L) >> Optional.of(p)

        when:
        def resp = controller.findById(7L)

        then:
        resp.statusCode == HttpStatus.OK
        resp.body.pendingTransactionId == 7L
    }

    void "save creates and returns 201"() {
        given:
        def p = validPending(0L)
        def saved = validPending(1L)
        repo.saveAndFlush(_ as PendingTransaction) >> { args -> saved }

        when:
        def resp = controller.save(p)

        then:
        resp.statusCode == HttpStatus.CREATED
        resp.body.pendingTransactionId == 1L
    }
}
