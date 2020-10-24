package finance.controllers

import finance.Application
import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Parm
import finance.domain.Payment
import finance.helpers.AccountBuilder
import finance.helpers.PaymentBuilder
import finance.services.AccountService
import finance.services.ParmService
import finance.services.PaymentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import spock.lang.Specification

@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentControllerSpec extends Specification {

    @LocalServerPort
    protected int port

    @Shared
    TestRestTemplate restTemplate

    @Shared
    HttpHeaders headers

    @Autowired
    PaymentService paymentService

    @Autowired
    AccountService accountService

    @Autowired
    ParmService parmService

    @Shared
    Payment payment

    @Shared
    Account account

    @Shared
    Parm parm

    def setupSpec() {
        restTemplate = new TestRestTemplate()
        headers = new HttpHeaders()
        payment = PaymentBuilder.builder().build()

        parm = new Parm()
        parm.parm_id = 1
        parm.parmName = 'payment_account'
        parm.parmValue = 'bcu-checking_brian'

        account = AccountBuilder.builder().build()
        account.accountType = AccountType.Credit
        account.accountNameOwner = "blah_brian"
    }

    private String createURLWithPort(String uri) {
        println "port = ${port}"

        return "http://localhost:" + port + uri
    }

    def "test Payment endpoint existing payment inserted and then deleted"() {
        given:
        parmService.insertParm(parm)
        paymentService.insertPayment(payment)
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/payment/delete/" + payment.paymentId), HttpMethod.DELETE,  entity, String.class)
        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    def "test Payment endpoint existing payment inserted and then attempt to delete a non existent payment"() {
        given:
        parmService.insertParm(parm)
        paymentService.insertPayment(payment)
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/payment/delete/" + 123451), HttpMethod.DELETE,  entity, String.class)
        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    def "test insertPayment endpoint - happy path"() {
        given:
        payment.accountNameOwner = 'happy-path_brian'
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payment, headers)
        parmService.insertParm(parm)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/payment/insert/"), HttpMethod.POST, entity, String.class)
        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    def "test insertPayment failed due to setup issues"() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payment, headers)
        parmService.deleteByParmName(parm.parmName)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/payment/insert/"), HttpMethod.POST,  entity, String.class)

        then:
        // TODO: Should this happen at the endpoint "thrown(RuntimeException)" or a 500?
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        0 * _

        cleanup:
        parmService.insertParm(parm)
    }

    //TODO: 10/24/2020 - this case need to fail to insert
    def "test insertPayment failed due to setup issues - to a non-debit account"() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        accountService.insertAccount(account)
        parmService.deleteByParmName(parm.parmName)
        parm.parmValue = account.accountNameOwner
        parmService.insertParm(parm)
        HttpEntity entity = new HttpEntity<>(payment, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/payment/insert/"), HttpMethod.POST,  entity, String.class)

        then:
        // TODO: Should this happen at the endpoint "thrown(RuntimeException)" or a 500?
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

}
