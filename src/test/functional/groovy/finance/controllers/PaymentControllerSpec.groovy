package finance.controllers

import finance.Application
import finance.domain.Parameter
import finance.domain.Payment
import finance.domain.Transaction
import finance.helpers.ParameterBuilder
import finance.helpers.PaymentBuilder
import finance.repositories.ParameterRepository
import finance.repositories.PaymentRepository
import finance.repositories.TransactionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

import java.sql.Date

@Stepwise
@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentControllerSpec extends BaseControllerSpec {

    @Autowired
    protected PaymentRepository paymentRepository

    @Autowired
    protected ParameterRepository parameterRepository

    @Autowired
    protected TransactionRepository transactionRepository

    @Shared
    protected Payment payment = PaymentBuilder.builder().build()

    @Shared
    protected String jsonPayloadInvalidAmount = '{"accountNameOwner":"foo_test","amount":5.1288888, "guidSource":"78f65481-f351-4142-aff6-73e99d2a286d", "guidDestination":"0db56665-0d47-414e-93c5-e5ae4c5e4299", "transactionDate":"2020-11-12"}'

    @Shared
    protected String jsonPayloadMissingAmount = '{"accountNameOwner":"foo_test", "guidSource":"78f65481-f351-4142-aff6-73e99d2a286d", "guidDestination":"0db56665-0d47-414e-93c5-e5ae4c5e4299", "transactionDate":"2020-11-12"}'

    @Shared
    protected String jsonPayloadInvalidSourceGuid = '{"accountNameOwner":"foo_test", "amount":5.1288888, "guidSource":"invalid", "guidDestination":"0db56665-0d47-414e-93c5-e5ae4c5e4299", "transactionDate":"2020-11-12"}'

    @Shared
    protected String endpointName = 'payment'

    void 'test insert Payment'() {
        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, payment.toString())

        then:
        response.statusCode.is(HttpStatus.OK)
        0 * _
    }

    void 'test insert Payment - duplicate'() {
        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, payment.toString())

        then:
        response.statusCode.is(HttpStatus.BAD_REQUEST)
        0 * _
    }

    void 'test insert Payment - prepare for delete'() {
        given:
        String accountNameOwner = 'delete-test_brian'
        Payment payment = PaymentBuilder.builder()
                .withTransactionDate(Date.valueOf('2020-10-13'))
                .withAccountNameOwner(accountNameOwner)
                .build()

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, payment.toString())

        then:
        response.statusCode.is(HttpStatus.OK)
        0 * _
    }

    void 'test delete Payment'() {
        given:
        Payment payment1 = paymentRepository.findAll().find { it.accountNameOwner == 'delete-test_brian' }

        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, payment1.paymentId.toString())

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test delete transaction of a payment'() {
        given:
        String accountNameOwner = 'delete-me_brian'
        Payment payment = PaymentBuilder.builder()
                .withTransactionDate(Date.valueOf('2020-12-13'))
                .withAccountNameOwner(accountNameOwner)
                .build()

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, payment.toString())

        then:
        response.statusCode.is(HttpStatus.OK)

        when:
        Transaction transaction = transactionRepository.findAll().find { it.accountNameOwner == accountNameOwner }

        then:
        transaction.accountNameOwner == accountNameOwner

        when:
        Payment payment1 = paymentRepository.findAll().find { it.accountNameOwner == accountNameOwner }

        then:
        payment1.accountNameOwner == accountNameOwner

        when:
        ResponseEntity<String> responseDelete = deleteEndpoint('transaction', transaction.guid)

        then:
        responseDelete.statusCode.is(HttpStatus.OK)
        paymentRepository.findById(payment1.paymentId).isEmpty()
    }

    void 'test Payment endpoint existing payment inserted and then attempt to delete a non existent payment'() {
        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, '1234567890')

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'test insert Payment - pay a debit account'() {
        given:
        payment.accountNameOwner = 'bank_brian'

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, payment.toString())

        then:
        response.statusCode.is(HttpStatus.BAD_REQUEST)
        0 * _
    }

    @Unroll
    void 'test insertPayment endpoint - failure for irregular payload'() {
        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, payload)

        then:
        response.statusCode.is(httpStatus)
        response.body.contains(responseBody)
        0 * _

        where:
        payload                      | httpStatus             | responseBody
        'badJson'                    | HttpStatus.BAD_REQUEST | 'Unrecognized token'
        '{"test":1}'                 | HttpStatus.BAD_REQUEST | 'value failed for JSON property accountNameOwner due to missing'
        '{badJson:"test"}'           | HttpStatus.BAD_REQUEST | 'was expecting double-quote to start field'
        jsonPayloadInvalidAmount     | HttpStatus.BAD_REQUEST | 'Cannot insert payment as there is a constraint violation on the data'
        jsonPayloadMissingAmount     | HttpStatus.BAD_REQUEST | 'value failed for JSON property amount due to missing'
        jsonPayloadInvalidSourceGuid | HttpStatus.BAD_REQUEST | 'Cannot insert payment as there is a constraint violation on the data'
    }

    void 'test insert Payment - missing payment setup'() {
        given:
        Payment payment = PaymentBuilder.builder().build()
        Parameter parameter = ParameterBuilder.builder()
                .withParameterName('payment_account')
                .withParameterValue('bank_brian')
                .build()

        when:
        ResponseEntity<String> responseDelete = deleteEndpoint('parm', parameter.parameterName)

        then:
        responseDelete.statusCode.is(HttpStatus.OK)

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, payment.toString())

        then:
        response.statusCode.is(HttpStatus.BAD_REQUEST)

        when:
        ResponseEntity<String> responseInsert = insertEndpoint('parm', parameter.toString())

        then:
        responseInsert.statusCode.is(HttpStatus.OK)
    }
}
