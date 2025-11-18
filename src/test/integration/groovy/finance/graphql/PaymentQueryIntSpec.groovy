package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.graphql.GraphQLQueryController
import finance.domain.Account
import finance.domain.Payment
import finance.helpers.SmartAccountBuilder
import finance.repositories.AccountRepository
import finance.services.PaymentService
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class PaymentQueryIntSpec extends BaseIntegrationSpec {

    @Shared @Autowired
    PaymentService paymentService

    @Shared @Autowired
    AccountRepository accountRepository

    @Shared @Autowired
    GraphQLQueryController queryController

    @Shared
    String sourceAccountName

    @Shared
    String destinationAccountName

    def setupSpec() {
        def cleanOwner = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        sourceAccountName = "checkingsrc_${cleanOwner}"
        destinationAccountName = "creditdst_${cleanOwner}"

        Account source = SmartAccountBuilder.builderForOwner(testOwner)
                .withAccountNameOwner(sourceAccountName)
                .asDebit()
                .withCleared(new BigDecimal("2000.00"))
                .buildAndValidate()
        accountRepository.save(source)

        Account dest = SmartAccountBuilder.builderForOwner(testOwner)
                .withAccountNameOwner(destinationAccountName)
                .asCredit()
                .withCleared(new BigDecimal("-500.00"))
                .buildAndValidate()
        accountRepository.save(dest)
    }

    def "fetch all payments via query controller"() {
        given:
        createTestPayment(sourceAccountName, destinationAccountName, new BigDecimal("100.00"))
        createTestPayment(sourceAccountName, destinationAccountName, new BigDecimal("200.00"))

        when:
        def payments = queryController.payments()
        def scoped = payments.findAll { it.sourceAccount == sourceAccountName && it.destinationAccount == destinationAccountName }

        then:
        scoped.size() >= 2
        scoped.any { it.amount == new BigDecimal("100.00") }
        scoped.any { it.amount == new BigDecimal("200.00") }
    }

    def "fetch payment by ID via query controller"() {
        given:
        def savedPayment = createTestPayment(sourceAccountName, destinationAccountName, new BigDecimal("150.00"))

        when:
        def result = queryController.payment(savedPayment.paymentId)

        then:
        result != null
        result.paymentId == savedPayment.paymentId
        result.sourceAccount == sourceAccountName
        result.destinationAccount == destinationAccountName
        result.amount == new BigDecimal("150.00")
    }

    def "handle payment not found via query controller"() {
        expect:
        queryController.payment(999L) == null
    }

    private Payment createTestPayment(String sourceAccount, String destinationAccount, BigDecimal amount) {
        Payment payment = new Payment()
        payment.sourceAccount = sourceAccount
        payment.destinationAccount = destinationAccount
        payment.transactionDate = LocalDate.parse("2024-01-01")
        payment.amount = amount
        payment.guidSource = UUID.randomUUID().toString()
        payment.guidDestination = UUID.randomUUID().toString()
        payment.activeStatus = true
        paymentService.insertPayment(payment)
    }
}
