package finance.controllers

import finance.Application
import finance.domain.Parameter
import finance.domain.Payment
import finance.helpers.ParameterBuilder
import finance.helpers.PaymentBuilder
import finance.repositories.ParameterRepository
import finance.repositories.PaymentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

import javax.transaction.Transactional
import java.sql.Date

@Stepwise
@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentControllerSpec extends BaseControllerSpec {

    @Autowired
    protected PaymentRepository paymentRepository

    @Autowired
    protected ParameterRepository parameterRepository

    @Shared
    protected Payment payment = PaymentBuilder.builder().build()

    @Shared
    protected String jsonPayloadInvalidAmount = '{"accountNameOwner":"foo_test","amount":5.1288888, "guidSource":"78f65481-f351-4142-aff6-73e99d2a286d", "guidDestination":"0db56665-0d47-414e-93c5-e5ae4c5e4299", "transactionDate":"2020-11-12"}'

    @Shared
    protected String jsonPayloadMissingAmount = '{"accountNameOwner":"foo_test", "guidSource":"78f65481-f351-4142-aff6-73e99d2a286d", "guidDestination":"0db56665-0d47-414e-93c5-e5ae4c5e4299", "transactionDate":"2020-11-12"}'

    @Shared
    protected String jsonPayloadInvalidSourceGuid = '{"accountNameOwner":"foo_test", "amount":5.1288888, "guidSource":"invalid", "guidDestination":"0db56665-0d47-414e-93c5-e5ae4c5e4299", "transactionDate":"2020-11-12"}'

    void 'test insert Payment'() {
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payment, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/payment/insert/'), HttpMethod.POST, entity, String)
        then:
        response.statusCode.is(HttpStatus.OK)
        0 * _
    }

    @Ignore('need to rethink the logic')
    void 'test delete Payment'() {
        given:
        payment.transactionDate = Date.valueOf('2020-10-13')
        Payment result = paymentRepository.save(payment)
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/payment/delete/' + result.paymentId), HttpMethod.DELETE, entity, String)
        then:
        response.statusCode == HttpStatus.OK
        0 * _
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
