package finance.repositories

import finance.Application
import finance.domain.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification
import java.math.BigDecimal
import java.sql.Date

@SpringBootTest
@ActiveProfiles("int")
@ContextConfiguration(classes = Application)
@Transactional
class MedicalExpenseRepositoryIntSpec extends Specification {

    @Autowired
    MedicalExpenseRepository medicalExpenseRepository

    @Autowired
    TransactionRepository transactionRepository

    @Autowired
    AccountRepository accountRepository

    @Autowired
    CategoryRepository categoryRepository




    def "MINIMAL TEST - check if test context loads"() {
        expect: "repositories to be injected"
        medicalExpenseRepository != null
        accountRepository != null
    }

    def "TDD Step 1: Test Transaction entity creation independently"() {
        given: "a valid account for the transaction"
        Account account = new Account(
            accountId: 0L,
            accountNameOwner: "transaction_account",
            accountType: AccountType.Credit,
            activeStatus: true,
            moniker: "0000",
            outstanding: new BigDecimal("0.00"),
            future: new BigDecimal("0.00"),
            cleared: new BigDecimal("0.00"),
            dateClosed: new java.sql.Timestamp(System.currentTimeMillis()),
            validationDate: new java.sql.Timestamp(System.currentTimeMillis())
        )
        Account savedAccount = null
        try {
            savedAccount = accountRepository.save(account)
        } catch (jakarta.validation.ConstraintViolationException e) {
            println "Account save constraint violations:"
            e.constraintViolations.each { violation ->
                println "  ${violation.propertyPath}: ${violation.message} (invalid value: ${violation.invalidValue})"
            }
            throw e
        }

        and: "a valid category for the transaction"
        Category category = new Category(
            categoryId: 0L,
            activeStatus: true,
            categoryName: "testcategory"
        )
        Category savedCategory = categoryRepository.save(category)

        and: "a transaction with all required fields"
        Transaction transaction = new Transaction(
            transactionId: 0L,
            guid: "550e8400-e29b-41d4-a716-446655440000",
            accountId: savedAccount.accountId,
            accountType: savedAccount.accountType,
            transactionType: TransactionType.Income,
            accountNameOwner: savedAccount.accountNameOwner,
            transactionDate: Date.valueOf("2024-01-15"),
            description: "test transaction",
            category: savedCategory.categoryName,
            amount: new BigDecimal("100.00"),
            transactionState: TransactionState.Cleared,
            activeStatus: true,
            reoccurringType: ReoccurringType.Undefined,
            notes: ""
        )

        when: "saving the transaction"
        Transaction savedTransaction = null
        try {
            savedTransaction = transactionRepository.save(transaction)
        } catch (jakarta.validation.ConstraintViolationException e) {
            println "Transaction save constraint violations:"
            e.constraintViolations.each { violation ->
                println "  ${violation.propertyPath}: ${violation.message} (invalid value: ${violation.invalidValue})"
            }
            throw e
        }

        then: "transaction should be saved successfully"
        savedTransaction.transactionId != null
        savedTransaction.guid == "550e8400-e29b-41d4-a716-446655440000"
        savedTransaction.accountId == savedAccount.accountId
        savedTransaction.amount == new BigDecimal("100.00")
    }

    def "TDD Step 2: Test MedicalExpense entity creation"() {
        given: "a valid transaction (using validated approach from Step 1)"
        // Create Account
        Account account = new Account(
            accountId: 0L,
            accountNameOwner: "medical_account",
            accountType: AccountType.Credit,
            activeStatus: true,
            moniker: "0000",
            outstanding: new BigDecimal("0.00"),
            future: new BigDecimal("0.00"),
            cleared: new BigDecimal("0.00"),
            dateClosed: new java.sql.Timestamp(System.currentTimeMillis()),
            validationDate: new java.sql.Timestamp(System.currentTimeMillis())
        )
        Account savedAccount = accountRepository.save(account)

        // Create Category
        Category category = new Category(
            categoryId: 0L,
            activeStatus: true,
            categoryName: "medical"
        )
        Category savedCategory = categoryRepository.save(category)

        // Create Transaction
        Transaction transaction = new Transaction(
            transactionId: 0L,
            guid: "123e4567-e89b-12d3-a456-426614174000",
            accountId: savedAccount.accountId,
            accountType: savedAccount.accountType,
            transactionType: TransactionType.Expense,
            accountNameOwner: savedAccount.accountNameOwner,
            transactionDate: Date.valueOf("2024-01-15"),
            description: "medical expense",
            category: savedCategory.categoryName,
            amount: new BigDecimal("350.00"),
            transactionState: TransactionState.Cleared,
            activeStatus: true,
            reoccurringType: ReoccurringType.Undefined,
            notes: ""
        )
        Transaction savedTransaction = transactionRepository.save(transaction)

        and: "a medical expense with all required fields"
        MedicalExpense medicalExpense = new MedicalExpense(
            medicalExpenseId: 0L,
            transactionId: savedTransaction.transactionId,
            providerId: null,
            familyMemberId: null,
            serviceDate: Date.valueOf("2024-01-15"),
            serviceDescription: "Test medical service",
            procedureCode: "99396",
            diagnosisCode: "Z00.00",
            billedAmount: new BigDecimal("350.00"),
            insuranceDiscount: new BigDecimal("50.00"),
            insurancePaid: new BigDecimal("250.00"),
            patientResponsibility: new BigDecimal("50.00"),
            isOutOfNetwork: false,
            claimNumber: "CLM-2024-001",
            claimStatus: ClaimStatus.Approved,
            activeStatus: true,
            dateAdded: new java.sql.Timestamp(System.currentTimeMillis()),
            dateUpdated: new java.sql.Timestamp(System.currentTimeMillis())
        )

        when: "saving the medical expense"
        MedicalExpense savedMedicalExpense = null
        try {
            savedMedicalExpense = medicalExpenseRepository.save(medicalExpense)
        } catch (jakarta.validation.ConstraintViolationException e) {
            println "MedicalExpense save constraint violations:"
            e.constraintViolations.each { violation ->
                println "  ${violation.propertyPath}: ${violation.message} (invalid value: ${violation.invalidValue})"
            }
            throw e
        }

        then: "medical expense should be saved successfully"
        savedMedicalExpense.medicalExpenseId != null
        savedMedicalExpense.transactionId == savedTransaction.transactionId
        savedMedicalExpense.billedAmount == new BigDecimal("350.00")
        savedMedicalExpense.claimStatus == ClaimStatus.Approved
    }

    def "should find medical expense by transaction ID"() {
        given: "a saved medical expense"
        MedicalExpense medicalExpense = createTestMedicalExpense(12345L)
        MedicalExpense savedMedicalExpense = medicalExpenseRepository.save(medicalExpense)

        when: "finding by transaction ID"
        MedicalExpense found = medicalExpenseRepository.findByTransactionId(savedMedicalExpense.transactionId)

        then: "should return the medical expense"
        found != null
        found.transactionId == savedMedicalExpense.transactionId
        found.medicalExpenseId == savedMedicalExpense.medicalExpenseId
    }

    def "should find medical expenses by service date range"() {
        given: "multiple medical expenses with different service dates"
        MedicalExpense expense1 = createTestMedicalExpense(11111L)
        expense1.serviceDate = Date.valueOf("2024-01-15")
        medicalExpenseRepository.save(expense1)

        MedicalExpense expense2 = createTestMedicalExpense(22222L)
        expense2.serviceDate = Date.valueOf("2024-02-15")
        medicalExpenseRepository.save(expense2)

        MedicalExpense expense3 = createTestMedicalExpense(33333L)
        expense3.serviceDate = Date.valueOf("2024-03-15")
        medicalExpenseRepository.save(expense3)

        when: "finding expenses in date range"
        List<MedicalExpense> found = medicalExpenseRepository.findByServiceDateBetweenAndActiveStatusTrue(
            Date.valueOf("2024-01-01"), Date.valueOf("2024-02-28")
        )

        then: "should return expenses within date range"
        found.size() == 2
        found.any { it.serviceDate == Date.valueOf("2024-01-15") }
        found.any { it.serviceDate == Date.valueOf("2024-02-15") }
        !found.any { it.serviceDate == Date.valueOf("2024-03-15") }
    }

    def "should find medical expenses by provider ID"() {
        given: "multiple medical expenses with different providers"
        MedicalExpense expense1 = createTestMedicalExpense(11111L)
        medicalExpenseRepository.save(expense1)

        MedicalExpense expense2 = createTestMedicalExpense(22222L)
        medicalExpenseRepository.save(expense2)

        MedicalExpense expense3 = createTestMedicalExpense(33333L)
        medicalExpenseRepository.save(expense3)

        when: "finding expenses by provider ID"
        List<MedicalExpense> found = medicalExpenseRepository.findByProviderIdAndActiveStatusTrue(null)

        then: "should return expenses with null provider ID"
        found.size() == 3
        found.every { it.providerId == null }
    }

    def "should find medical expenses by family member ID"() {
        given: "multiple medical expenses for different family members"
        MedicalExpense expense1 = createTestMedicalExpense(11111L)
        medicalExpenseRepository.save(expense1)

        MedicalExpense expense2 = createTestMedicalExpense(22222L)
        medicalExpenseRepository.save(expense2)

        MedicalExpense expense3 = createTestMedicalExpense(33333L)
        medicalExpenseRepository.save(expense3)

        when: "finding expenses by family member ID"
        List<MedicalExpense> found = medicalExpenseRepository.findByFamilyMemberIdAndActiveStatusTrue(null)

        then: "should return expenses with null family member ID"
        found.size() == 3
        found.every { it.familyMemberId == null }
    }

    def "should find medical expenses by claim status"() {
        given: "multiple medical expenses with different claim statuses"
        MedicalExpense expense1 = createTestMedicalExpense(11111L)
        expense1.claimStatus = ClaimStatus.Submitted
        medicalExpenseRepository.save(expense1)

        MedicalExpense expense2 = createTestMedicalExpense(22222L)
        expense2.claimStatus = ClaimStatus.Approved
        medicalExpenseRepository.save(expense2)

        MedicalExpense expense3 = createTestMedicalExpense(33333L)
        expense3.claimStatus = ClaimStatus.Approved
        medicalExpenseRepository.save(expense3)

        when: "finding expenses by claim status"
        List<MedicalExpense> found = medicalExpenseRepository.findByClaimStatusAndActiveStatusTrue(ClaimStatus.Approved)

        then: "should return expenses with that status"
        found.size() == 2
        found.every { it.claimStatus == ClaimStatus.Approved }
    }

    def "should find out-of-network expenses"() {
        given: "medical expenses with different network statuses"
        MedicalExpense expense1 = createTestMedicalExpense(11111L)
        expense1.isOutOfNetwork = true
        medicalExpenseRepository.save(expense1)

        MedicalExpense expense2 = createTestMedicalExpense(22222L)
        expense2.isOutOfNetwork = false
        medicalExpenseRepository.save(expense2)

        when: "finding out-of-network expenses"
        List<MedicalExpense> found = medicalExpenseRepository.findByIsOutOfNetworkAndActiveStatusTrue(true)

        then: "should return only out-of-network expenses"
        found.size() == 1
        found[0].isOutOfNetwork == true
    }

    def "should find medical expense by claim number"() {
        given: "a medical expense with claim number"
        MedicalExpense medicalExpense = createTestMedicalExpense(12345L)
        medicalExpense.claimNumber = "CLM-2024-TEST"
        medicalExpenseRepository.save(medicalExpense)

        when: "finding by claim number"
        MedicalExpense found = medicalExpenseRepository.findByClaimNumberAndActiveStatusTrue("CLM-2024-TEST")

        then: "should return the medical expense"
        found != null
        found.claimNumber == "CLM-2024-TEST"
    }

    def "should calculate total billed amount by year"() {
        given: "medical expenses from different years"
        MedicalExpense expense2024_1 = createTestMedicalExpense(11111L)
        expense2024_1.serviceDate = Date.valueOf("2024-01-15")
        expense2024_1.billedAmount = new BigDecimal("500.00")
        expense2024_1.insurancePaid = new BigDecimal("400.00")
        medicalExpenseRepository.save(expense2024_1)

        MedicalExpense expense2024_2 = createTestMedicalExpense(22222L)
        expense2024_2.serviceDate = Date.valueOf("2024-06-15")
        medicalExpenseRepository.save(expense2024_2)

        MedicalExpense expense2023 = createTestMedicalExpense(33333L)
        expense2023.serviceDate = Date.valueOf("2023-12-15")
        expense2023.billedAmount = new BigDecimal("200.00")
        expense2023.insurancePaid = new BigDecimal("100.00")
        medicalExpenseRepository.save(expense2023)

        when: "calculating total billed amount for 2024"
        BigDecimal total = medicalExpenseRepository.getTotalBilledAmountByYear(2024)

        then: "should return correct total for 2024"
        total == new BigDecimal("800.00")
    }

    def "should calculate total patient responsibility by year"() {
        given: "medical expenses with patient responsibilities"
        MedicalExpense expense1 = createTestMedicalExpense(11111L)
        expense1.serviceDate = Date.valueOf("2024-01-15")
        expense1.patientResponsibility = new BigDecimal("100.00")
        expense1.insurancePaid = new BigDecimal("150.00")
        medicalExpenseRepository.save(expense1)

        MedicalExpense expense2 = createTestMedicalExpense(22222L)
        expense2.serviceDate = Date.valueOf("2024-06-15")
        expense2.patientResponsibility = new BigDecimal("150.00")
        expense2.insurancePaid = new BigDecimal("100.00")
        medicalExpenseRepository.save(expense2)

        when: "calculating total patient responsibility for 2024"
        BigDecimal total = medicalExpenseRepository.getTotalPatientResponsibilityByYear(2024)

        then: "should return correct total"
        total == new BigDecimal("250.00")
    }

    def "should find outstanding patient balances"() {
        given: "medical expenses with various payment statuses"
        MedicalExpense paidExpense = createTestMedicalExpense(11111L)
        paidExpense.billedAmount = new BigDecimal("350.00")
        paidExpense.patientResponsibility = new BigDecimal("100.00")
        paidExpense.paidDate = Date.valueOf("2024-02-01")
        medicalExpenseRepository.save(paidExpense)

        MedicalExpense unpaidExpense = createTestMedicalExpense(22222L)
        unpaidExpense.billedAmount = new BigDecimal("400.00")
        unpaidExpense.patientResponsibility = new BigDecimal("150.00")
        unpaidExpense.paidDate = null
        medicalExpenseRepository.save(unpaidExpense)

        MedicalExpense zeroBalance = createTestMedicalExpense(33333L)
        zeroBalance.patientResponsibility = BigDecimal.ZERO
        zeroBalance.paidDate = null
        medicalExpenseRepository.save(zeroBalance)

        when: "finding outstanding patient balances"
        List<MedicalExpense> outstanding = medicalExpenseRepository.findOutstandingPatientBalances()

        then: "should return only unpaid expenses with balance"
        outstanding.size() == 1
        outstanding[0].patientResponsibility == new BigDecimal("150.00")
        outstanding[0].paidDate == null
    }

    def "should find active open claims"() {
        given: "medical expenses with various claim statuses"
        MedicalExpense submittedClaim = createTestMedicalExpense(11111L)
        submittedClaim.claimStatus = ClaimStatus.Submitted
        medicalExpenseRepository.save(submittedClaim)

        MedicalExpense processingClaim = createTestMedicalExpense(22222L)
        processingClaim.claimStatus = ClaimStatus.Processing
        medicalExpenseRepository.save(processingClaim)

        MedicalExpense paidClaim = createTestMedicalExpense(33333L)
        paidClaim.claimStatus = ClaimStatus.Paid
        medicalExpenseRepository.save(paidClaim)

        MedicalExpense closedClaim = createTestMedicalExpense(44444L)
        closedClaim.claimStatus = ClaimStatus.Closed
        medicalExpenseRepository.save(closedClaim)

        when: "finding active open claims"
        List<MedicalExpense> openClaims = medicalExpenseRepository.findActiveOpenClaims()

        then: "should return claims that are not paid, closed, or denied"
        openClaims.size() == 2
        openClaims.any { it.claimStatus == ClaimStatus.Submitted }
        openClaims.any { it.claimStatus == ClaimStatus.Processing }
    }

    def "should update claim status"() {
        given: "a saved medical expense"
        MedicalExpense medicalExpense = createTestMedicalExpense()
        medicalExpense.claimStatus = ClaimStatus.Submitted
        MedicalExpense saved = medicalExpenseRepository.save(medicalExpense)

        when: "updating claim status"
        int updatedRows = medicalExpenseRepository.updateClaimStatus(saved.medicalExpenseId, ClaimStatus.Approved)

        then: "should update the status"
        updatedRows == 1

        and: "expense should have updated status"
        MedicalExpense updated = medicalExpenseRepository.findById(saved.medicalExpenseId).orElse(null)
        updated.claimStatus == ClaimStatus.Approved
    }

    def "should soft delete medical expense"() {
        given: "a saved medical expense"
        MedicalExpense medicalExpense = createTestMedicalExpense()
        MedicalExpense saved = medicalExpenseRepository.save(medicalExpense)

        when: "soft deleting the expense"
        int updatedRows = medicalExpenseRepository.softDeleteByMedicalExpenseId(saved.medicalExpenseId)

        then: "should update active status"
        updatedRows == 1

        and: "expense should be marked inactive"
        MedicalExpense updated = medicalExpenseRepository.findById(saved.medicalExpenseId).orElse(null)
        updated.activeStatus == false
    }

    def "should find medical expenses by procedure code"() {
        given: "medical expenses with different procedure codes"
        MedicalExpense expense1 = createTestMedicalExpense(11111L)
        expense1.procedureCode = "99396"
        medicalExpenseRepository.save(expense1)

        MedicalExpense expense2 = createTestMedicalExpense(22222L)
        expense2.procedureCode = "99396"
        medicalExpenseRepository.save(expense2)

        MedicalExpense expense3 = createTestMedicalExpense(33333L)
        expense3.procedureCode = "99213"
        medicalExpenseRepository.save(expense3)

        when: "finding by procedure code"
        List<MedicalExpense> found = medicalExpenseRepository.findByProcedureCodeAndActiveStatusTrue("99396")

        then: "should return expenses with that procedure code"
        found.size() == 2
        found.every { it.procedureCode == "99396" }
    }

    def "should count medical expenses by claim status"() {
        given: "multiple medical expenses with various statuses"
        MedicalExpense expense1 = createTestMedicalExpense(11111L)
        expense1.claimStatus = ClaimStatus.Approved
        medicalExpenseRepository.save(expense1)

        MedicalExpense expense2 = createTestMedicalExpense(22222L)
        expense2.claimStatus = ClaimStatus.Approved
        medicalExpenseRepository.save(expense2)

        MedicalExpense expense3 = createTestMedicalExpense(33333L)
        expense3.claimStatus = ClaimStatus.Submitted
        medicalExpenseRepository.save(expense3)

        when: "counting by claim status"
        Long count = medicalExpenseRepository.countByClaimStatusAndActiveStatusTrue(ClaimStatus.Approved)

        then: "should return correct count"
        count == 2L
    }

    private MedicalExpense createTestMedicalExpense(Long transactionId = 1001L) {
        Transaction savedTransaction = createTestTransaction(transactionId)
        return new MedicalExpense(
            medicalExpenseId: 0L,
            transactionId: savedTransaction.transactionId,
            providerId: null,
            familyMemberId: null,
            serviceDate: Date.valueOf("2024-01-15"),
            serviceDescription: "Test medical service",
            procedureCode: "99396",
            diagnosisCode: "Z00.00",
            billedAmount: new BigDecimal("300.00"),
            insuranceDiscount: new BigDecimal("50.00"),
            insurancePaid: new BigDecimal("200.00"),
            patientResponsibility: new BigDecimal("50.00"),
            isOutOfNetwork: false,
            claimNumber: "CLM-2024-001",
            claimStatus: ClaimStatus.Approved,
            activeStatus: true,
            dateAdded: new java.sql.Timestamp(System.currentTimeMillis()),
            dateUpdated: new java.sql.Timestamp(System.currentTimeMillis())
        )
    }

    private Transaction createTestTransaction(Long transactionId) {
        // Check if transaction already exists
        Optional<Transaction> existingTransaction = transactionRepository.findById(transactionId)
        if (existingTransaction.isPresent()) {
            return existingTransaction.get()  // Return existing transaction
        }

        // Create required test data with unique names (following validation patterns)
        // Pattern: ^[a-z-]*_[a-z]*$ (letters/dashes, exactly one underscore, letters)
        def accountName = "testaccount_${transactionId < 20000 ? 'a' : (transactionId < 30000 ? 'b' : 'c')}"
        def categoryName = "testcategory"

        // Check if account already exists
        Account account = accountRepository.findByAccountNameOwner(accountName).orElse(null)
        if (!account) {
            account = new Account(
                accountId: 0L,  // Will be generated by database
                accountNameOwner: accountName,
                accountType: AccountType.Credit,
                activeStatus: true,
                moniker: "0000",
                outstanding: new BigDecimal("0.00"),
                future: new BigDecimal("0.00"),
                cleared: new BigDecimal("0.00"),
                dateClosed: new java.sql.Timestamp(System.currentTimeMillis()),
                validationDate: new java.sql.Timestamp(System.currentTimeMillis())
            )
            account = accountRepository.save(account)
        }

        // Check if category already exists
        Category category = categoryRepository.findByCategoryName(categoryName).orElse(null)
        if (!category) {
            category = new Category(
                categoryId: 0L,
                activeStatus: true,
                categoryName: categoryName
            )
            category = categoryRepository.save(category)
        }

        def transaction = new Transaction(
            transactionId: 0L,  // Let database generate the ID
            guid: UUID.randomUUID().toString(),
            accountId: account.accountId,
            accountType: account.accountType,
            transactionType: TransactionType.Expense,
            accountNameOwner: account.accountNameOwner,
            transactionDate: Date.valueOf("2024-01-15"),
            description: "test transaction for medical expense",
            category: category.categoryName,
            amount: new BigDecimal("350.00"),
            transactionState: TransactionState.Cleared,
            reoccurringType: ReoccurringType.Undefined,
            notes: "",
            activeStatus: true
        )
        return transactionRepository.save(transaction)
    }
}