package finance.controllers

import finance.domain.TransactionState
import finance.domain.ValidationAmount
import finance.services.ValidationAmountService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Subject

class ValidationAmountControllerSpec extends Specification {

    ValidationAmountService service = GroovyMock(ValidationAmountService)

    @Subject
    ValidationAmountController controller = new ValidationAmountController(service)

    def "selectValidationAmountByAccountId normalizes state value and returns OK"() {
        given:
        def va = new ValidationAmount(validationId: 1L, accountId: 2L, validationDate: new java.sql.Timestamp(0L), activeStatus: true, transactionState: TransactionState.Cleared, amount: 1.23G)

        when:
        ResponseEntity<ValidationAmount> response = controller.selectValidationAmountByAccountId('acct', 'cleared')

        then:
        1 * service.findValidationAmountByAccountNameOwner('acct', TransactionState.Cleared) >> va
        response.statusCode == HttpStatus.OK
        response.body == va
    }

    def "insertValidationAmount returns OK on success"() {
        given:
        def validationAmount = new ValidationAmount(validationId: 0L, accountId: 1L, validationDate: new java.sql.Timestamp(0L), activeStatus: true, transactionState: TransactionState.Cleared, amount: 1.23G)
        def created = new ValidationAmount(validationId: 1L, accountId: 1L, validationDate: new java.sql.Timestamp(0L), activeStatus: true, transactionState: TransactionState.Cleared, amount: 1.23G)

        when:
        ResponseEntity<ValidationAmount> resp = controller.insertValidationAmount(validationAmount, 'acct')

        then:
        1 * service.insertValidationAmount('acct', validationAmount) >> created
        resp.statusCode == HttpStatus.OK
        resp.body == created
    }

    def "insertValidationAmount maps validation exception to 400"() {
        when:
        ResponseEntity<?> resp = controller.insertValidationAmount(new ValidationAmount(), 'acct')

        then:
        1 * service.insertValidationAmount('acct', _) >> { throw new jakarta.validation.ValidationException('bad') }
        resp.statusCode == HttpStatus.BAD_REQUEST
        (resp.body as Map).error.toString().startsWith('Validation error:')
    }
}
