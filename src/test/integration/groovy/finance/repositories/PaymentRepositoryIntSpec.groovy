package finance.repositories

import finance.BaseIntegrationSpec
import finance.domain.Payment
import finance.helpers.SmartPaymentBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Shared

import java.math.BigDecimal
import java.sql.Date

/**
 * INTEGRATION TEST - PaymentRepository using robust, isolated architecture
 *
 * This integration test demonstrates the new architecture:
 * ✅ No hardcoded account names - all use testOwner for uniqueness
 * ✅ SmartBuilder pattern with constraint validation
 * ✅ Test isolation - each test gets its own test data
 * ✅ Constraint-aware test data creation
 * ✅ Build-time validation prevents invalid data
 * ✅ Financial domain specific testing for payment transactions
 */
class PaymentRepositoryIntSpec extends BaseIntegrationSpec {

    @Autowired
    PaymentRepository paymentRepository

    @Shared
    def repositoryContext

    def setupSpec() {
        repositoryContext = testFixtures.createRepositoryTestContext(testOwner)
    }

    void 'test payment repository basic CRUD operations'() {
        given:
        Payment payment = SmartPaymentBuilder.builderForOwner(testOwner)
                .withTestDataAccounts()
                .withAmount(new BigDecimal("250.75"))
                .withTransactionDate(Date.valueOf("2024-01-15"))
                .buildAndValidate()

        when:
        Payment savedPayment = paymentRepository.save(payment)

        then:
        savedPayment.paymentId != null
        savedPayment.paymentId > 0
        savedPayment.sourceAccount.contains(testOwner.replaceAll(/[^a-z]/, ''))
        savedPayment.destinationAccount.contains(testOwner.replaceAll(/[^a-z]/, ''))
        savedPayment.amount == new BigDecimal("250.75")
        savedPayment.transactionDate == Date.valueOf("2024-01-15")
        savedPayment.activeStatus == true
        savedPayment.guidSource != null
        savedPayment.guidDestination != null
        savedPayment.guidSource != savedPayment.guidDestination

        when:
        Optional<Payment> foundPayment = paymentRepository.findByPaymentId(savedPayment.paymentId)

        then:
        foundPayment.isPresent()
        foundPayment.get().sourceAccount == savedPayment.sourceAccount
        foundPayment.get().destinationAccount == savedPayment.destinationAccount
        foundPayment.get().amount == savedPayment.amount
    }

    void 'test find by payment ID with non-existent ID'() {
        when:
        Optional<Payment> foundPayment = paymentRepository.findByPaymentId(99999L)

        then:
        !foundPayment.isPresent()
    }

    void 'test payment unique constraint on destination, date, and amount'() {
        given:
        String cleanOwner = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        String uniqueDestination = "unique_dest_${cleanOwner}"
        Date transactionDate = Date.valueOf("2024-02-20")
        BigDecimal amount = new BigDecimal("100.50")

        Payment payment1 = SmartPaymentBuilder.builderForOwner(testOwner.toString())
                .withUniqueAccounts("source1", "uniquedest")
                .withAmount(amount)
                .withTransactionDate(transactionDate)
                .buildAndValidate()

        // Manually set destination to ensure exact match for constraint test
        payment1.destinationAccount = uniqueDestination

        when:
        Payment savedPayment1 = paymentRepository.save(payment1)

        then:
        savedPayment1.paymentId != null

        when: "trying to save payment with same destination, date, and amount"
        Payment payment2 = SmartPaymentBuilder.builderForOwner(testOwner.toString())
                .withUniqueAccounts("source2", "uniquedest")
                .withAmount(amount)
                .withTransactionDate(transactionDate)
                .buildAndValidate()

        // Set same destination to trigger constraint
        payment2.destinationAccount = uniqueDestination

        paymentRepository.save(payment2)

        then:
        thrown(DataIntegrityViolationException)
    }

    void 'test payment with different amounts allows duplicate destinations and dates'() {
        given:
        String cleanOwner = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        String sharedDestination = "shared_dest_${cleanOwner}"
        Date sharedDate = Date.valueOf("2024-03-15")

        Payment payment1 = SmartPaymentBuilder.builderForOwner(testOwner.toString())
                .withUniqueAccounts("src1", "sharedest")
                .withAmount(new BigDecimal("100.00"))
                .withTransactionDate(sharedDate)
                .buildAndValidate()

        payment1.destinationAccount = sharedDestination

        Payment payment2 = SmartPaymentBuilder.builderForOwner(testOwner.toString())
                .withUniqueAccounts("src2", "sharedest")
                .withAmount(new BigDecimal("200.00"))  // Different amount
                .withTransactionDate(sharedDate)
                .buildAndValidate()

        payment2.destinationAccount = sharedDestination

        when:
        Payment savedPayment1 = paymentRepository.save(payment1)
        Payment savedPayment2 = paymentRepository.save(payment2)

        then:
        savedPayment1.paymentId != null
        savedPayment2.paymentId != null
        savedPayment1.paymentId != savedPayment2.paymentId
        savedPayment1.destinationAccount == savedPayment2.destinationAccount
        savedPayment1.transactionDate == savedPayment2.transactionDate
        savedPayment1.amount != savedPayment2.amount
    }

    void 'test find duplicate payment excluding current payment ID'() {
        given:
        String cleanOwner = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        String destination = "duplicate_test_${cleanOwner}"
        Date transactionDate = Date.valueOf("2024-04-10")
        BigDecimal amount = new BigDecimal("75.25")

        Payment originalPayment = SmartPaymentBuilder.builderForOwner(testOwner.toString())
                .withUniqueAccounts("original", "duplicatetest")
                .withAmount(amount)
                .withTransactionDate(transactionDate)
                .buildAndValidate()

        originalPayment.destinationAccount = destination

        when:
        Payment savedPayment = paymentRepository.save(originalPayment)

        then:
        savedPayment.paymentId != null

        when: "checking for duplicates excluding the saved payment itself"
        Optional<Payment> duplicate = paymentRepository.findByDestinationAccountAndTransactionDateAndAmountAndPaymentIdNot(
                destination, transactionDate, amount, savedPayment.paymentId)

        then:
        !duplicate.isPresent()

        when: "creating another payment with same details"
        Payment anotherPayment = SmartPaymentBuilder.builderForOwner(testOwner.toString())
                .withUniqueAccounts("another", "duplicatetest")
                .withAmount(amount)
                .withTransactionDate(transactionDate)
                .buildAndValidate()

        anotherPayment.destinationAccount = destination

        // This will fail due to unique constraint, but let's test the finder method behavior
        // by checking if it would find a duplicate before attempting to save
        Optional<Payment> wouldBeDuplicate = paymentRepository.findByDestinationAccountAndTransactionDateAndAmountAndPaymentIdNot(
                destination, transactionDate, amount, 0L) // Use 0L as non-existent ID

        then:
        wouldBeDuplicate.isPresent()
        wouldBeDuplicate.get().paymentId == savedPayment.paymentId
    }

    void 'test payment with different transaction dates allows same destination and amount'() {
        given:
        String cleanOwner = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        String destination = "date_test_${cleanOwner}"
        BigDecimal amount = new BigDecimal("150.75")

        Payment payment1 = SmartPaymentBuilder.builderForOwner(testOwner.toString())
                .withUniqueAccounts("date1", "datetest")
                .withAmount(amount)
                .withTransactionDate(Date.valueOf("2024-05-01"))
                .buildAndValidate()

        payment1.destinationAccount = destination

        Payment payment2 = SmartPaymentBuilder.builderForOwner(testOwner.toString())
                .withUniqueAccounts("date2", "datetest")
                .withAmount(amount)
                .withTransactionDate(Date.valueOf("2024-05-02"))  // Different date
                .buildAndValidate()

        payment2.destinationAccount = destination

        when:
        Payment savedPayment1 = paymentRepository.save(payment1)
        Payment savedPayment2 = paymentRepository.save(payment2)

        then:
        savedPayment1.paymentId != null
        savedPayment2.paymentId != null
        savedPayment1.destinationAccount == savedPayment2.destinationAccount
        savedPayment1.amount == savedPayment2.amount
        savedPayment1.transactionDate != savedPayment2.transactionDate
    }

    void 'test payment constraint validation through SmartBuilder'() {
        when: "creating payment with invalid amount precision"
        SmartPaymentBuilder.builderForOwner(testOwner)
                .withTestDataAccounts()
                .withAmount(new BigDecimal("100.123"))  // Too many decimal places
                .buildAndValidate()

        then:
        IllegalStateException ex = thrown()
        ex.message.contains("decimal places")

        when: "creating payment with negative amount"
        SmartPaymentBuilder.builderForOwner(testOwner)
                .withTestDataAccounts()
                .withAmount(new BigDecimal("-50.00"))
                .buildAndValidate()

        then:
        IllegalStateException ex2 = thrown()
        ex2.message.contains("non-negative")

        when: "creating payment with invalid account pattern"
        SmartPaymentBuilder.builderForOwner(testOwner)
                .withSourceAccount("invalid123")  // Contains digits
                .withDestinationAccount("valid_account")
                .withAmount(new BigDecimal("100.00"))
                .buildAndValidate()

        then:
        IllegalStateException ex3 = thrown()
        ex3.message.contains("alpha_underscore pattern")
    }

    void 'test payment with maximum allowed precision'() {
        given:
        Payment payment = SmartPaymentBuilder.builderForOwner(testOwner)
                .withTestDataAccounts()
                .withAmount(new BigDecimal("999999.99"))  // Maximum allowed precision (8,2) - 6 integer digits + 2 decimal = 8 total
                .withTransactionDate(Date.valueOf("2024-06-15"))
                .buildAndValidate()

        when:
        Payment savedPayment = paymentRepository.save(payment)

        then:
        savedPayment.paymentId != null
        savedPayment.amount == new BigDecimal("999999.99")
    }

    void 'test payment active status functionality'() {
        given:
        Payment activePayment = SmartPaymentBuilder.builderForOwner(testOwner)
                .withTestDataAccounts()
                .withAmount(new BigDecimal("50.00"))
                .withTransactionDate(Date.valueOf("2024-07-01"))
                .asActive()
                .buildAndValidate()

        Payment inactivePayment = SmartPaymentBuilder.builderForOwner(testOwner.toString())
                .withUniqueAccounts("inactivesrc", "inactivedest")
                .withAmount(new BigDecimal("75.00"))
                .withTransactionDate(Date.valueOf("2024-07-02"))
                .asInactive()
                .buildAndValidate()

        when:
        Payment savedActive = paymentRepository.save(activePayment)
        Payment savedInactive = paymentRepository.save(inactivePayment)

        then:
        savedActive.activeStatus == true
        savedInactive.activeStatus == false

        when:
        List<Payment> allPayments = paymentRepository.findAll()

        then:
        allPayments.any { it.paymentId == savedActive.paymentId && it.activeStatus == true }
        allPayments.any { it.paymentId == savedInactive.paymentId && it.activeStatus == false }
    }
}
