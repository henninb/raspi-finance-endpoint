package finance.controllers

import finance.Application
import finance.domain.Payment
import finance.domain.Transaction
import finance.helpers.PaymentBuilder
import finance.repositories.ParameterRepository
import finance.repositories.PaymentRepository
import finance.repositories.TransactionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.*
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
    protected Long paymentId = 0

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
        Payment payment1 = paymentRepository.findAll().find {it.accountNameOwner == 'delete-test_brian'}
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/payment/delete/' + payment1.paymentId), HttpMethod.DELETE, entity, String)
        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test insert Payment - to prepare the delete transaction'() {
        given:
        String accountNameOwner = 'delete-me_brian'
        Payment payment = PaymentBuilder.builder()
                .withTransactionDate(Date.valueOf('2020-12-13'))
                .withAccountNameOwner(accountNameOwner)
                .build()
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payment, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/payment/insert/'), HttpMethod.POST, entity, String)

        then:
        response.statusCode.is(HttpStatus.OK)
        0 * _
    }

    void 'test delete transaction of a payment'() {
        given:
        Transaction transaction = transactionRepository.findAll().find {it.accountNameOwner == 'delete-me_brian'}
        Payment payment1 = paymentRepository.findAll().find {it.accountNameOwner == 'delete-me_brian'}

        when:
        transactionRepository.deleteByGuid(transaction.guid)

        then:
        paymentRepository.findById(payment1.paymentId).isEmpty()
        noExceptionThrown()
    }

    void 'test Payment endpoint existing payment inserted and then attempt to delete a non existent payment'() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/payment/delete/123451"), HttpMethod.DELETE, entity, String)
        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'test insert Payment - pay a debit account'() {
        given:
        payment.accountNameOwner = 'bank_brian'
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payment, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/payment/insert/'), HttpMethod.POST, entity, String)

        then:
        response.statusCode.is(HttpStatus.BAD_REQUEST)
        0 * _
    }

    @Unroll
    void 'test insertPayment endpoint - failure for irregular payload'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/payment/insert/'), HttpMethod.POST,
                entity, String)
        then:
        response.statusCode.is(httpStatus)
        response.body.contains(responseBody)
        0 * _

        where:
        payload                      | httpStatus             | responseBody
        'badJson'                    | HttpStatus.BAD_REQUEST | 'Unrecognized token'
        '{"test":1}'                 | HttpStatus.BAD_REQUEST | 'value failed for JSON property accountNameOwner due to missing'
        '{badJson:"test"}'           | HttpStatus.BAD_REQUEST | 'was expecting double-quote to start field'
        jsonPayloadInvalidAmount     | HttpStatus.BAD_REQUEST | 'Cannot insert payment as there is a constraint violation on the data.'
        jsonPayloadMissingAmount     | HttpStatus.BAD_REQUEST | 'value failed for JSON property amount due to missing'
        jsonPayloadInvalidSourceGuid | HttpStatus.BAD_REQUEST | 'Cannot insert payment as there is a constraint violation on the data'
    }

//    @Transactional
    void 'test insert Payment - missing payment setup'() {
        given:
        Payment payment = PaymentBuilder.builder().build()
//        Parameter parameter = ParameterBuilder.builder()
//                .withParameterName('payment_account')
//                .withParameterValue('bank_brian')
//                .build()
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payment, headers)
        parameterRepository.deleteByParameterName('payment_account')

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/payment/insert/'), HttpMethod.POST, entity, String)

        then:
        response.statusCode.is(HttpStatus.BAD_REQUEST)
        0 * _
    }
}
