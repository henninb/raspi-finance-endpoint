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

import java.sql.Date

@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentControllerSpec extends Specification {

    @LocalServerPort
    protected int port

    @Shared
    protected TestRestTemplate restTemplate

    @Shared
    protected HttpHeaders headers

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

    void setupSpec() {
        restTemplate = new TestRestTemplate()
        headers = new HttpHeaders()
        payment = PaymentBuilder.builder().build()

        parameter = new Parameter()
        //TODO: do I need to set the Id?
        parameter.parameterId = 1
        parameter.parameterName = 'payment_account'
        parameter.parameterValue = 'bcu-checking_brian'

        account = AccountBuilder.builder().build()
        account.accountType = AccountType.Credit
        account.accountNameOwner = "blah_brian"
    }

    private String createURLWithPort(String uri) {
        return 'http://localhost:' + port + uri
    }

    void 'test Payment endpoint existing payment inserted and then deleted'() {
        given:
        parmService.insertParm(parameter)
        payment.guidDestination = UUID.randomUUID()
        payment.guidSource = UUID.randomUUID()
        payment.transactionDate = Date.valueOf("2020-10-12")
        paymentService.insertPayment(payment)
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/payment/delete/" + payment.paymentId), HttpMethod.DELETE, entity, String)
        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test Payment endpoint existing payment inserted and then attempt to delete a non existent payment'() {
        given:
        parmService.insertParm(parameter)
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
        parmService.insertParm(parameter)

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
        parmService.deleteByParmName(parameter.parameterName)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/payment/insert/'), HttpMethod.POST, entity, String)

        then:
        // TODO: Should this happen at the endpoint "thrown(RuntimeException)" or a 500?
        response.statusCode.is(HttpStatus.INTERNAL_SERVER_ERROR)
        0 * _

        cleanup:
        parmService.insertParm(parameter)
    }

    //TODO: 10/24/2020 - this case need to fail to insert - take a look
    //TODO: build fails in intellij
    void 'test insertPayment failed due to setup issues - to a non-debit account'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        accountService.insertAccount(account)
        parmService.deleteByParmName(parameter.parameterName)
        parameter.parameterValue = account.accountNameOwner
        parmService.insertParm(parameter)
        HttpEntity entity = new HttpEntity<>(payment, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/payment/insert/'), HttpMethod.POST, entity, String)

        then:
        // TODO: Should this happen at the endpoint "thrown(RuntimeException)" or a 500?
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }
}
