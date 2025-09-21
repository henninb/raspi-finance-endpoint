package finance.controllers

import finance.domain.TransactionState
import finance.domain.ValidationAmount
import finance.services.IValidationAmountService
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification

class ValidationAmountControllerMoreSpec extends Specification {

    def service = Mock(IValidationAmountService)
    def controller = new ValidationAmountController(service)

    void "findAllActive returns 200 with list (standardized)"() {
        when:
        def resp = controller.findAllActive()

        then:
        1 * service.findAllActiveValidationAmounts() >> [new ValidationAmount(validationId: 1L)]
        resp.statusCode == HttpStatus.OK
        resp.body.size() == 1
    }

    void "findById returns 404 then 200 (standardized)"() {
        when:
        controller.findById(9L)

        then:
        1 * service.findValidationAmountById(9L) >> Optional.empty()
        thrown(ResponseStatusException)

        when:
        def resp = controller.findById(10L)

        then:
        1 * service.findValidationAmountById(10L) >> Optional.of(new ValidationAmount(validationId: 10L))
        resp.statusCode == HttpStatus.OK
    }

    void "save returns 201 (standardized)"() {
        given:
        def va = new ValidationAmount(validationId: 0L)

        when:
        def resp = controller.save(va)

        then:
        1 * service.insertValidationAmount(va) >> new ValidationAmount(validationId: 2L)
        resp.statusCode == HttpStatus.CREATED
        resp.body.validationId == 2L
    }

    void "update returns 404 then 200 (standardized)"() {
        given:
        def va = new ValidationAmount(validationId: 3L)

        when:
        controller.update(3L, va)

        then:
        1 * service.findValidationAmountById(3L) >> Optional.empty()
        thrown(ResponseStatusException)

        when:
        def resp = controller.update(4L, new ValidationAmount(validationId: 4L))

        then:
        1 * service.findValidationAmountById(4L) >> Optional.of(new ValidationAmount(validationId: 4L))
        1 * service.updateValidationAmount({ it.validationId == 4L }) >> { it[0] }
        resp.statusCode == HttpStatus.OK
    }

    void "legacy insertValidationAmount returns 200 and maps validation errors to 400"() {
        given:
        def va = new ValidationAmount(validationId: 0L)

        when:
        def resp = controller.insertValidationAmount(va, 'acct')

        then:
        1 * service.insertValidationAmount('acct', va) >> new ValidationAmount(validationId: 5L)
        resp.statusCode == HttpStatus.OK

        when:
        def bad = controller.insertValidationAmount(va, 'acct')

        then:
        1 * service.insertValidationAmount('acct', va) >> { throw new jakarta.validation.ValidationException('bad') }
        bad.statusCode == HttpStatus.BAD_REQUEST
        bad.body instanceof Map
    }

    void "selectValidationAmountByAccountId returns 200"() {
        when:
        def resp = controller.selectValidationAmountByAccountId('acct', 'Cleared')

        then:
        1 * service.findValidationAmountByAccountNameOwner('acct', TransactionState.Cleared) >> new ValidationAmount(validationId: 6L)
        resp.statusCode == HttpStatus.OK
        resp.body.validationId == 6L
    }
}
