package finance.repositories

import finance.BaseIntegrationSpec
import finance.domain.MedicalExpense
import finance.domain.Transaction
import finance.domain.Account
import finance.domain.AccountType
import finance.domain.TransactionType
import finance.domain.TransactionState
import finance.domain.ClaimStatus
import finance.helpers.SmartMedicalExpenseBuilder
import finance.helpers.SmartTransactionBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import spock.lang.Shared

import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

/**
 * MIGRATED INTEGRATION TEST - Medical Expense Repository with robust, isolated architecture
 *
 * This is the migrated version of MedicalExpenseRepositoryIntSpec showing:
 * ✅ No hardcoded entity names - all use testOwner for uniqueness
 * ✅ SmartBuilder pattern with constraint validation
 * ✅ Test isolation - each test gets its own test data
 * ✅ Constraint-aware test data creation using SmartMedicalExpenseBuilder
 * ✅ Proper FK relationship management with TestDataManager
 * ✅ Financial amount validation and consistency
 */
class MedicalExpenseRepositoryIntSpec extends BaseIntegrationSpec {

    @Autowired
    MedicalExpenseRepository medicalExpenseRepository

    @Autowired
    TransactionRepository transactionRepository

    @Autowired
    JdbcTemplate jdbcTemplate

    @Shared
    @Autowired
    AccountRepository accountRepository

    @Shared
    def repositoryContext

    @Shared
    Long primaryAccountId

    @Shared
    String ownerAccountName

    def setupSpec() {
        repositoryContext = testFixtures.createRepositoryTestContext(testOwner)
        ownerAccountName = repositoryContext.primaryAccountName
        // Ensure account exists using repository; create via SmartBuilder if missing
        Optional<Account> acc = accountRepository.findByOwnerAndAccountNameOwner(testOwner, ownerAccountName)
        if (!acc.isPresent()) {
            def acct = finance.helpers.SmartAccountBuilder.builderForOwner(testOwner)
                    .withAccountNameOwner(ownerAccountName)
                    .asCredit()  // Medical expenses typically use credit accounts
                    .buildAndValidate()
            acct = accountRepository.save(acct)
            primaryAccountId = acct.accountId
        } else {
            primaryAccountId = acc.get().accountId
        }
    }

    def "should save and retrieve medical expense successfully"() {
        given:
        Long transactionId = createTestTransaction()
        MedicalExpense medicalExpense = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId)
                .withBilledAmount(new BigDecimal("350.00"))
                .withClaimStatus(ClaimStatus.Submitted)
                .buildAndValidate()

        when:
        MedicalExpense savedExpense = medicalExpenseRepository.save(medicalExpense)

        then:
        savedExpense.medicalExpenseId != null
        savedExpense.transactionId == transactionId
        savedExpense.billedAmount == new BigDecimal("350.00")
        savedExpense.claimStatus == ClaimStatus.Submitted
        savedExpense.serviceDescription.contains(testOwner.replaceAll(/[^a-zA-Z0-9]/, ''))
    }

    def "should find medical expense by transaction ID"() {
        given:
        Long transactionId = createTestTransaction()
        MedicalExpense savedExpense = createTestMedicalExpense(transactionId)

        when:
        MedicalExpense found = medicalExpenseRepository.findByOwnerAndTransactionId(testOwner,transactionId)

        then:
        found != null
        found.transactionId == savedExpense.transactionId
        found.medicalExpenseId == savedExpense.medicalExpenseId
        found.serviceDescription.contains(testOwner.replaceAll(/[^a-zA-Z0-9]/, ''))
    }

    def "should find medical expenses by service date range"() {
        given:
        Long transactionId1 = createTestTransaction("expense1")
        Long transactionId2 = createTestTransaction("expense2")
        Long transactionId3 = createTestTransaction("expense3")

        MedicalExpense expense1 = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId1)
                .withServiceDate(LocalDate.parse("2024-01-15"))
                .buildAndValidate()
        medicalExpenseRepository.save(expense1)

        MedicalExpense expense2 = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId2)
                .withServiceDate(LocalDate.parse("2024-02-15"))
                .buildAndValidate()
        medicalExpenseRepository.save(expense2)

        MedicalExpense expense3 = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId3)
                .withServiceDate(LocalDate.parse("2024-03-15"))
                .buildAndValidate()
        medicalExpenseRepository.save(expense3)

        when:
        List<MedicalExpense> found = medicalExpenseRepository.findByOwnerAndServiceDateBetweenAndActiveStatusTrue(testOwner,
            LocalDate.parse("2024-01-01"), LocalDate.parse("2024-02-28")
        )

        then:
        found.size() >= 2
        found.any { it.serviceDate == LocalDate.parse("2024-01-15") && it.transactionId == transactionId1 }
        found.any { it.serviceDate == LocalDate.parse("2024-02-15") && it.transactionId == transactionId2 }
        !found.any { it.serviceDate == LocalDate.parse("2024-03-15") && it.transactionId == transactionId3 }
    }

    def "should find medical expenses by provider ID"() {
        given:
        Long transactionId1 = createTestTransaction("provider1")
        Long transactionId2 = createTestTransaction("provider2")

        MedicalExpense expense1 = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId1)
                .withProviderId(null)
                .buildAndValidate()
        medicalExpenseRepository.save(expense1)

        MedicalExpense expense2 = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId2)
                .withProviderId(null)
                .buildAndValidate()
        medicalExpenseRepository.save(expense2)

        when:
        List<MedicalExpense> found = medicalExpenseRepository.findByOwnerAndProviderIdAndActiveStatusTrue(testOwner,null)

        then:
        found.size() >= 2
        found.every { it.providerId == null }
        found.any { it.transactionId == transactionId1 }
        found.any { it.transactionId == transactionId2 }
    }

    def "should find medical expenses by claim status"() {
        given:
        Long transactionId1 = createTestTransaction("approved1")
        Long transactionId2 = createTestTransaction("approved2")
        Long transactionId3 = createTestTransaction("submitted")

        MedicalExpense approved1 = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId1)
                .withClaimStatus(ClaimStatus.Approved)
                .buildAndValidate()
        medicalExpenseRepository.save(approved1)

        MedicalExpense approved2 = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId2)
                .withClaimStatus(ClaimStatus.Approved)
                .buildAndValidate()
        medicalExpenseRepository.save(approved2)

        MedicalExpense submitted = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId3)
                .withClaimStatus(ClaimStatus.Submitted)
                .buildAndValidate()
        medicalExpenseRepository.save(submitted)

        when:
        List<MedicalExpense> approvedExpenses = medicalExpenseRepository.findByOwnerAndClaimStatusAndActiveStatusTrue(testOwner,ClaimStatus.Approved)

        then:
        approvedExpenses.size() >= 2
        approvedExpenses.every { it.claimStatus == ClaimStatus.Approved }
        approvedExpenses.any { it.transactionId == transactionId1 }
        approvedExpenses.any { it.transactionId == transactionId2 }
    }

    def "should find out-of-network expenses"() {
        given:
        Long transactionId1 = createTestTransaction("network1")
        Long transactionId2 = createTestTransaction("network2")

        MedicalExpense outOfNetwork = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId1)
                .withOutOfNetwork(true)
                .buildAndValidate()
        medicalExpenseRepository.save(outOfNetwork)

        MedicalExpense inNetwork = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId2)
                .withOutOfNetwork(false)
                .buildAndValidate()
        medicalExpenseRepository.save(inNetwork)

        when:
        List<MedicalExpense> found = medicalExpenseRepository.findByOwnerAndIsOutOfNetworkAndActiveStatusTrue(testOwner,true)

        then:
        found.size() >= 1
        found.any { it.transactionId == transactionId1 && it.isOutOfNetwork == true }
        !found.any { it.transactionId == transactionId2 }
    }

    def "should find medical expense by claim number"() {
        given:
        Long transactionId = createTestTransaction("claimtest")
        String uniqueClaimNumber = "CLM-${testOwner.replaceAll(/[^a-zA-Z0-9]/, '').toUpperCase()}-TEST"

        MedicalExpense expense = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId)
                .build()  // Use build() instead of buildAndValidate() to allow customization
        // Override claim number to be unique for test
        expense.claimNumber = uniqueClaimNumber
        medicalExpenseRepository.save(expense)

        when:
        MedicalExpense found = medicalExpenseRepository.findByOwnerAndClaimNumberAndActiveStatusTrue(testOwner,uniqueClaimNumber)

        then:
        found != null
        found.claimNumber == uniqueClaimNumber
        found.transactionId == transactionId
    }

    def "should calculate total billed amount by year"() {
        given:
        Long transactionId1 = createTestTransaction("year1")
        Long transactionId2 = createTestTransaction("year2")

        MedicalExpense expense2024_1 = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId1)
                .withServiceDate(LocalDate.parse("2024-01-15"))
                .withBilledAmount(new BigDecimal("500.00"))
                .buildAndValidate()
        medicalExpenseRepository.save(expense2024_1)

        MedicalExpense expense2024_2 = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId2)
                .withServiceDate(LocalDate.parse("2024-06-15"))
                .withBilledAmount(new BigDecimal("300.00"))
                .buildAndValidate()
        medicalExpenseRepository.save(expense2024_2)

        when:
        BigDecimal total = medicalExpenseRepository.getTotalBilledAmountByOwnerAndYear(testOwner,2024)

        then:
        total >= new BigDecimal("800.00")  // At least our test data
    }

    def "should calculate total patient responsibility by year"() {
        given:
        Long transactionId1 = createTestTransaction("patient1")
        Long transactionId2 = createTestTransaction("patient2")

        MedicalExpense expense1 = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId1)
                .withServiceDate(LocalDate.parse("2024-01-15"))
                .withBilledAmount(new BigDecimal("300.00"))  // Set billed amount first
                .build()  // Use build() to allow manual financial setup
        expense1.patientResponsibility = new BigDecimal("100.00")  // Then set patient responsibility
        expense1.insurancePaid = new BigDecimal("200.00")         // And adjust insurance paid
        medicalExpenseRepository.save(expense1)

        MedicalExpense expense2 = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId2)
                .withServiceDate(LocalDate.parse("2024-06-15"))
                .withBilledAmount(new BigDecimal("400.00"))  // Set billed amount first
                .build()  // Use build() to allow manual financial setup
        expense2.patientResponsibility = new BigDecimal("150.00")  // Then set patient responsibility
        expense2.insurancePaid = new BigDecimal("250.00")          // And adjust insurance paid
        medicalExpenseRepository.save(expense2)

        when:
        BigDecimal total = medicalExpenseRepository.getTotalPatientResponsibilityByOwnerAndYear(testOwner,2024)

        then:
        total >= new BigDecimal("250.00")  // At least our test data
    }

    def "should find outstanding patient balances"() {
        given:
        Long transactionId1 = createTestTransaction("paid")
        Long transactionId2 = createTestTransaction("unpaid")
        Long transactionId3 = createTestTransaction("zero")

        // Paid expense with paid date
        MedicalExpense paidExpense = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId1)
                .withBilledAmount(new BigDecimal("300.00"))
                .build()  // Use build() to allow manual setup
        paidExpense.patientResponsibility = new BigDecimal("100.00")
        paidExpense.insurancePaid = new BigDecimal("200.00")
        paidExpense.paidDate = LocalDate.parse("2024-02-01")
        medicalExpenseRepository.save(paidExpense)

        // Unpaid expense with balance
        MedicalExpense unpaidExpense = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId2)
                .withBilledAmount(new BigDecimal("400.00"))
                .build()  // Use build() to allow manual setup
        unpaidExpense.patientResponsibility = new BigDecimal("150.00")
        unpaidExpense.insurancePaid = new BigDecimal("250.00")
        unpaidExpense.paidDate = null
        medicalExpenseRepository.save(unpaidExpense)

        // Zero balance expense
        MedicalExpense zeroBalance = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId3)
                .withBilledAmount(new BigDecimal("200.00"))
                .build()  // Use build() to allow manual setup
        zeroBalance.patientResponsibility = BigDecimal.ZERO
        zeroBalance.insurancePaid = new BigDecimal("200.00")  // Insurance pays full amount
        zeroBalance.paidDate = null
        medicalExpenseRepository.save(zeroBalance)

        when:
        List<MedicalExpense> outstanding = medicalExpenseRepository.findOutstandingPatientBalancesByOwner(testOwner)

        then:
        outstanding.size() >= 1
        outstanding.any {
            it.transactionId == transactionId2 &&
            it.patientResponsibility > BigDecimal.ZERO &&
            it.paidDate == null
        }
    }

    def "should find active open claims"() {
        given:
        Long transactionId1 = createTestTransaction("submitted")
        Long transactionId2 = createTestTransaction("processing")
        Long transactionId3 = createTestTransaction("paid")

        MedicalExpense submittedClaim = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId1)
                .withClaimStatus(ClaimStatus.Submitted)
                .buildAndValidate()
        medicalExpenseRepository.save(submittedClaim)

        MedicalExpense processingClaim = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId2)
                .withClaimStatus(ClaimStatus.Processing)
                .buildAndValidate()
        medicalExpenseRepository.save(processingClaim)

        MedicalExpense paidClaim = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId3)
                .withClaimStatus(ClaimStatus.Paid)
                .buildAndValidate()
        medicalExpenseRepository.save(paidClaim)

        when:
        List<MedicalExpense> openClaims = medicalExpenseRepository.findActiveOpenClaimsByOwner(testOwner)

        then:
        openClaims.size() >= 2
        openClaims.any { it.transactionId == transactionId1 && it.claimStatus == ClaimStatus.Submitted }
        openClaims.any { it.transactionId == transactionId2 && it.claimStatus == ClaimStatus.Processing }
        !openClaims.any { it.transactionId == transactionId3 && it.claimStatus == ClaimStatus.Paid }
    }

    def "should update claim status"() {
        given:
        Long transactionId = createTestTransaction("update")
        MedicalExpense expense = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId)
                .withClaimStatus(ClaimStatus.Submitted)
                .buildAndValidate()
        MedicalExpense saved = medicalExpenseRepository.save(expense)

        when:
        int updatedRows = medicalExpenseRepository.updateClaimStatusByOwner(testOwner, saved.medicalExpenseId, ClaimStatus.Approved)

        then:
        updatedRows == 1

        and:
        MedicalExpense updated = medicalExpenseRepository.findById(saved.medicalExpenseId).orElse(null)
        updated.claimStatus == ClaimStatus.Approved
    }

    def "should soft delete medical expense"() {
        given:
        Long transactionId = createTestTransaction("delete")
        MedicalExpense expense = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId)
                .buildAndValidate()
        MedicalExpense saved = medicalExpenseRepository.save(expense)

        when:
        int updatedRows = medicalExpenseRepository.softDeleteByOwnerAndMedicalExpenseId(testOwner, saved.medicalExpenseId)

        then:
        updatedRows == 1

        and:
        MedicalExpense updated = medicalExpenseRepository.findById(saved.medicalExpenseId).orElse(null)
        updated.activeStatus == false
    }

    def "should find medical expenses by procedure code"() {
        given:
        Long transactionId1 = createTestTransaction("procedure1")
        Long transactionId2 = createTestTransaction("procedure2")
        Long transactionId3 = createTestTransaction("procedure3")

        MedicalExpense expense1 = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId1)
                .buildAndValidate()
        expense1.procedureCode = "99396"
        medicalExpenseRepository.save(expense1)

        MedicalExpense expense2 = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId2)
                .buildAndValidate()
        expense2.procedureCode = "99396"
        medicalExpenseRepository.save(expense2)

        MedicalExpense expense3 = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId3)
                .buildAndValidate()
        expense3.procedureCode = "99213"
        medicalExpenseRepository.save(expense3)

        when:
        List<MedicalExpense> found = medicalExpenseRepository.findByOwnerAndProcedureCodeAndActiveStatusTrue(testOwner,"99396")

        then:
        found.size() >= 2
        found.every { it.procedureCode == "99396" }
        found.any { it.transactionId == transactionId1 }
        found.any { it.transactionId == transactionId2 }
    }

    def "should count medical expenses by claim status"() {
        given:
        Long transactionId1 = createTestTransaction("count1")
        Long transactionId2 = createTestTransaction("count2")
        Long transactionId3 = createTestTransaction("count3")

        MedicalExpense expense1 = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId1)
                .withClaimStatus(ClaimStatus.Approved)
                .buildAndValidate()
        medicalExpenseRepository.save(expense1)

        MedicalExpense expense2 = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId2)
                .withClaimStatus(ClaimStatus.Approved)
                .buildAndValidate()
        medicalExpenseRepository.save(expense2)

        MedicalExpense expense3 = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId3)
                .withClaimStatus(ClaimStatus.Submitted)
                .buildAndValidate()
        medicalExpenseRepository.save(expense3)

        when:
        Long count = medicalExpenseRepository.countByOwnerAndClaimStatusAndActiveStatusTrue(testOwner,ClaimStatus.Approved)

        then:
        count >= 2L
    }

    // Helper methods using new architecture

    private Long createTestTransaction(String suffix = "medical") {
        // Create transaction using SmartBuilder pattern (like other migrated tests)
        String descriptionLower = "medical test transaction ${suffix}".toLowerCase()
        Transaction transaction = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(primaryAccountId)
                .withAccountType(AccountType.Credit)
                .withTransactionType(TransactionType.Expense)
                .withAccountNameOwner(ownerAccountName)
                .withTransactionDate(LocalDate.parse("2024-01-15"))
                .withDescription(descriptionLower)
                .withCategory(repositoryContext.categoryName)
                .withAmount(new BigDecimal("350.00"))
                .asCleared()
                .buildAndValidate()

        // Ensure category and description exist before JPA save (required by FKs)
        testDataManager.ensureCategoryAndDescriptionExist(testOwner, repositoryContext.categoryName, descriptionLower)

        Transaction savedTransaction = transactionRepository.save(transaction)
        return savedTransaction.transactionId
    }

    private MedicalExpense createTestMedicalExpense(Long transactionId) {
        MedicalExpense expense = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId)
                .buildAndValidate()
        return medicalExpenseRepository.save(expense)
    }
}
