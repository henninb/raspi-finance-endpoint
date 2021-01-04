package finance.controllers

import finance.Application
import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Parameter
import finance.domain.Payment
import finance.helpers.AccountBuilder
import finance.helpers.PaymentBuilder
import finance.services.AccountService
import finance.services.ParameterService
import finance.services.PaymentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.sql.Date

@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentControllerSpec extends BaseControllerSpec {

    @Autowired
    protected PaymentService paymentService

    @Autowired
    protected AccountService accountService

    @Autowired
    protected ParameterService parmService

    @Shared
    protected Payment payment

    @Shared
    protected Account account

    @Shared
    protected Parameter parameter

    @Shared
    protected String jsonPayloadInvalidAmount = '{"accountNameOwner":"foo_test","amount":5.1288888, "guidSource":"78f65481-f351-4142-aff6-73e99d2a286d", "guidDestination":"0db56665-0d47-414e-93c5-e5ae4c5e4299", "transactionDate":"2020-11-12"}'


    @Shared
    protected String jsonPayloadMissingAmount = '{"accountNameOwner":"foo_test", "guidSource":"78f65481-f351-4142-aff6-73e99d2a286d", "guidDestination":"0db56665-0d47-414e-93c5-e5ae4c5e4299", "transactionDate":"2020-11-12"}'

    @Shared
    protected String jsonPayloadInvalidSourceGuid = '{"accountNameOwner":"foo_test", "amount":5.1288888, "guidSource":"invalid", "guidDestination":"0db56665-0d47-414e-93c5-e5ae4c5e4299", "transactionDate":"2020-11-12"}'

    void setupSpec() {
        payment = PaymentBuilder.builder().build()

        parameter = new Parameter()
        //TODO: do I need to set the Id?
        parameter.parameterId = 1
        parameter.parameterName = 'payment_account'
        parameter.parameterValue = 'bcu-checking_brian'

        account = AccountBuilder.builder().build()
        account.accountType = AccountType.Credit
        account.accountNameOwner = 'blah_brian'
    }

    void 'test Payment endpoint existing payment inserted and then deleted'() {
        given:
        parmService.insertParameter(parameter)
        payment.guidDestination = UUID.randomUUID()
        payment.guidSource = UUID.randomUUID()
        payment.transactionDate = Date.valueOf('2020-10-12')
        paymentService.insertPayment(payment)
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/payment/delete/' + payment.paymentId), HttpMethod.DELETE, entity, String)
        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test Payment endpoint existing payment inserted and then attempt to delete a non existent payment'() {
        given:
        parmService.insertParameter(parameter)
        payment.guidDestination = UUID.randomUUID()
        payment.guidSource = UUID.randomUUID()
        payment.transactionDate = Date.valueOf('2020-10-11')
        paymentService.insertPayment(payment)
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/payment/delete/123451'), HttpMethod.DELETE, entity, String)
        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'test insertPayment endpoint - happy path'() {
        given:
        payment.accountNameOwner = 'happy-path_brian'
        payment.guidDestination = UUID.randomUUID()
        payment.guidSource = UUID.randomUUID()
        payment.transactionDate = Date.valueOf('2020-10-10') //new Date(1605300155000)

        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payment, headers)
        parmService.insertParameter(parameter)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/payment/insert/'), HttpMethod.POST, entity, String)
        then:
        response.statusCode.is(HttpStatus.OK)
        0 * _
    }

    void 'test insertPayment failed due to setup issues'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payment, headers)
        parmService.deleteByParameterName(parameter.parameterName)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/payment/insert/'), HttpMethod.POST, entity, String)

        then:
        // TODO: Should this happen at the endpoint "thrown(RuntimeException)" or a 500?
        response.statusCode.is(HttpStatus.INTERNAL_SERVER_ERROR)
        0 * _

        cleanup:
        parmService.insertParameter(parameter)
    }

    //TODO: 10/24/2020 - this case need to fail to insert - take a look
    void 'test insertPayment failed due to setup issues - to a non-debit account'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        accountService.insertAccount(account)
        parmService.deleteByParameterName(parameter.parameterName)
        parameter.parameterValue = account.accountNameOwner
        parmService.insertParameter(parameter)
        HttpEntity entity = new HttpEntity<>(payment, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/payment/insert/'), HttpMethod.POST, entity, String)

        then:
        // TODO: Should this happen at the endpoint "thrown(RuntimeException)" or a 500?
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
}
