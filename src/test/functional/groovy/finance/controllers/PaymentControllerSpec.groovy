package finance.controllers

import finance.Application
import finance.domain.Parm
import finance.domain.Payment
import finance.helpers.PaymentBuilder
import finance.services.ParmService
import finance.services.PaymentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentControllerSpec extends Specification {

    @LocalServerPort
    protected int port

    TestRestTemplate restTemplate = new TestRestTemplate()

    @Shared
    HttpHeaders headers

    @Autowired
    PaymentService paymentService

    @Autowired
    ParmService parmService

    @Shared
    Payment payment

    def setup() {
        headers = new HttpHeaders()
        payment = PaymentBuilder.builder().build()

        Parm parm = new Parm()
        parm.parmName = 'payment_account'
        parm.parmValue = 'bcu-checking_brian'
        //TODO: parm insert
        parmService.insertParm(parm)
        println('insertParm')
    }

    private String createURLWithPort(String uri) {
        println "port = ${port}"

        return "http://localhost:" + port + uri
    }

    def "test Payment endpoint paymentId found and deleted"() {
        given:
        def paymentId = 1
//        Parm parm = new Parm()
//        parm.parmName = 'payment_account'
//        parm.parmValue = 'bcu-checking_brian'
//        parmService.insertParm(parm)
        paymentService.insertPayment(payment)
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/payment/delete/" + paymentId), HttpMethod.DELETE,
                entity, String.class)
        then:
        response.statusCode == HttpStatus.OK
        0 * _

    }

//    def "test findAccount endpoint accountNameOwner not found"() {
//        given:
//        HttpEntity entity = new HttpEntity<>(null, headers)
//
//        when:
//        ResponseEntity<String> response = restTemplate.exchange(
//                createURLWithPort("/account/select/" + UUID.randomUUID().toString()), HttpMethod.GET,
//                entity, String.class)
//        then:
//        response.statusCode == HttpStatus.NOT_FOUND
//        0 * _
//    }
//
//    def "test deleteAccount endpoint"() {
//        given:
//        accountService.insertAccount(account)
//
//        HttpEntity entity = new HttpEntity<>(null, headers)
//
//        when:
//        ResponseEntity<String> response = restTemplate.exchange(
//                createURLWithPort("/account/delete/" + account.accountNameOwner), HttpMethod.DELETE,
//                entity, String.class)
//        then:
//        response.statusCode == HttpStatus.OK
//        0 * _
//
//        cleanup:
//        accountService.deleteByAccountNameOwner(account.accountNameOwner)
//    }
//

    @Ignore
    def "test insertPayment endpoint"() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payment, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/payment/insert/"), HttpMethod.POST,
                entity, String.class)
        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }
}
