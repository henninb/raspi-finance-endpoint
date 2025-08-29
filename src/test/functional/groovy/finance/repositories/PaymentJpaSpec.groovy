package finance.repositories

import finance.Application
import finance.domain.Payment
import finance.helpers.SmartPaymentBuilder
import finance.helpers.TestDataManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

import java.sql.Date

@ActiveProfiles("func")
@DataJpaTest
@ContextConfiguration(classes = [Application])
@Import(TestDataManager)
class PaymentJpaSpec extends Specification {

    @Autowired
    protected PaymentRepository paymentRepository

    @Autowired
    protected TestEntityManager entityManager

    @Autowired
    protected TestDataManager testDataManager

    @Shared
    protected String testOwner = "jpa_${UUID.randomUUID().toString().replace('-'
, '')[0..7]}"

    void 'payment insert via JPA with SmartBuilder'() {

        given:
        // Prepare minimal accounts and two backing transactions to satisfy FK constraints
        testDataManager.createMinimalAccountsFor(testOwner)
        String sourceGuid = testDataManager.createTransactionFor(testOwner, 'primary', 'payment_source', new BigDecimal('12.99'))
        String destGuid = testDataManager.createTransactionFor(testOwner, 'secondary', 'payment_dest', new BigDecimal('12.99'))

        // Build a valid Payment using SmartBuilder (no JSON)
        Payment payment = SmartPaymentBuilder.builderForOwner(testOwner)
                .withTestDataAccounts() // primary_${owner} and secondary_${owner}
                .withAmount(12.99G)
                .withTransactionDate(Date.valueOf('2020-12-30'))
                .withGuidSource(sourceGuid)
                .withGuidDestination(destGuid)
                .asActive()
                .buildAndValidate()

        long before = paymentRepository.count()

        when:
        Payment result = entityManager.persist(payment)
        entityManager.flush()

        then:
        paymentRepository.count() == before + 1
        result.paymentId > 0L
        result.sourceAccount.startsWith('primary_')
        result.destinationAccount.startsWith('secondary_')
    }

    void 'payment insert and delete via repository'() {

        given:
        // Prepare minimal accounts and backing transactions for FK constraints
        testDataManager.createMinimalAccountsFor(testOwner)
        String sourceGuid = testDataManager.createTransactionFor(testOwner, 'primary', 'payment_source', new BigDecimal('15.50'))
        String destGuid = testDataManager.createTransactionFor(testOwner, 'secondary', 'payment_dest', new BigDecimal('15.50'))

        Payment payment = SmartPaymentBuilder.builderForOwner(testOwner)
                .withTestDataAccounts()
                .withAmount(15.50G)
                .withTransactionDate(Date.valueOf('2020-12-30'))
                .withGuidSource(sourceGuid)
                .withGuidDestination(destGuid)
                .asActive()
                .buildAndValidate()

        long before = paymentRepository.count()
        Payment saved = entityManager.persist(payment)
        entityManager.flush()

        when:
        paymentRepository.delete(saved)

        then:
        paymentRepository.count() == before
    }
}
