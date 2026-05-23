package finance.controllers

import finance.controllers.dto.AccountInputDto
import finance.controllers.dto.CategoryInputDto
import finance.controllers.dto.DescriptionInputDto
import finance.controllers.dto.MedicalExpenseInputDto
import finance.controllers.dto.ParameterInputDto
import finance.controllers.dto.PaymentInputDto
import finance.controllers.dto.TransactionInputDto
import finance.controllers.dto.TransferInputDto
import finance.controllers.dto.ValidationAmountInputDto
import finance.domain.AccountType
import finance.domain.ClaimStatus
import finance.domain.ReoccurringType
import finance.domain.TransactionState
import finance.domain.TransactionType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

import java.sql.Timestamp
import java.time.LocalDate

class GraphQLControllerFunctionalSpec extends BaseControllerFunctionalSpec {

    @Autowired
    finance.controllers.graphql.GraphQLQueryController queryController

    @Autowired
    finance.controllers.graphql.GraphQLMutationController mutationController

    private void withUserRole(String name = 'test-user', List<String> roles = ['USER']) {
        def authorities = roles.collect { new SimpleGrantedAuthority(it) }
        def auth = new UsernamePasswordAuthenticationToken(name, 'N/A', authorities)
        SecurityContextHolder.getContext().setAuthentication(auth)
    }

    private String shortOwner() {
        String clean = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        if (clean.isEmpty()) clean = 'testowner'
        return clean.length() > 8 ? clean[0..7] : clean
    }

    // ===========================
    // QUERY CONTROLLER - accounts
    // ===========================

    void 'accounts with null type returns all accounts'() {
        when:
        withUserRole(testOwner)
        def result = queryController.accounts(null)

        then:
        result != null
    }

    void 'accounts filtered by account type returns list'() {
        when:
        withUserRole(testOwner)
        def result = queryController.accounts(AccountType.Debit)

        then:
        result != null
    }

    void 'account by name returns account when it exists'() {
        given:
        String accountName = testDataManager.createAccountFor(testOwner, 'gqlqa', 'debit', true)

        when:
        withUserRole(testOwner)
        def result = queryController.account(accountName)

        then:
        result != null
        result.accountNameOwner == accountName
    }

    void 'account by name returns null when not found'() {
        when:
        withUserRole(testOwner)
        def result = queryController.account('nonexistent_account')

        then:
        result == null
    }

    // ============================
    // QUERY CONTROLLER - categories
    // ============================

    void 'categories returns list of categories'() {
        when:
        withUserRole(testOwner)
        def result = queryController.categories()

        then:
        result != null
    }

    void 'category by name returns category when it exists'() {
        given:
        String categoryName = testDataManager.createCategoryFor(testOwner, 'gqlcat')

        when:
        withUserRole(testOwner)
        def result = queryController.category(categoryName)

        then:
        result != null
        result.categoryName == categoryName
    }

    void 'category by name returns null when not found'() {
        when:
        withUserRole(testOwner)
        def result = queryController.category('nonexistent-category-xyz')

        then:
        result == null
    }

    // ==============================
    // QUERY CONTROLLER - descriptions
    // ==============================

    void 'description by name returns description when it exists'() {
        given:
        String descriptionName = testDataManager.createDescriptionFor(testOwner, 'gqldesc')

        when:
        withUserRole(testOwner)
        def result = queryController.description(descriptionName)

        then:
        result != null
        result.descriptionName == descriptionName
    }

    void 'description by name returns null when not found'() {
        when:
        withUserRole(testOwner)
        def result = queryController.description('nonexistent-description-xyz')

        then:
        result == null
    }

    // ==========================
    // QUERY CONTROLLER - payments
    // ==========================

    void 'payments returns list'() {
        when:
        withUserRole(testOwner)
        def result = queryController.payments()

        then:
        result != null
    }

    void 'payment by id returns null when not found'() {
        when:
        withUserRole(testOwner)
        def result = queryController.payment(999999L)

        then:
        result == null
    }

    // ===========================
    // QUERY CONTROLLER - transfers
    // ===========================

    void 'transfers returns list'() {
        when:
        withUserRole(testOwner)
        def result = queryController.transfers()

        then:
        result != null
    }

    void 'transfer by id returns null when not found'() {
        when:
        withUserRole(testOwner)
        def result = queryController.transfer(999999L)

        then:
        result == null
    }

    // ==============================
    // QUERY CONTROLLER - transactions
    // ==============================

    void 'transactions for account returns list'() {
        given:
        String accountName = testDataManager.createAccountFor(testOwner, 'gqltxacct', 'debit', true)

        when:
        withUserRole(testOwner)
        def result = queryController.transactions(accountName)

        then:
        result != null
    }

    void 'transaction by guid returns null when not found'() {
        when:
        withUserRole(testOwner)
        def result = queryController.transaction('00000000-0000-0000-0000-000000000000')

        then:
        result == null
    }

    // ============================
    // QUERY CONTROLLER - parameters
    // ============================

    void 'parameters returns list'() {
        when:
        withUserRole(testOwner)
        def result = queryController.parameters()

        then:
        result != null
    }

    void 'parameter by id returns null when not found'() {
        when:
        withUserRole(testOwner)
        def result = queryController.parameter(999999L)

        then:
        result == null
    }

    // =================================
    // QUERY CONTROLLER - validationAmounts
    // =================================

    void 'validationAmounts returns list'() {
        when:
        withUserRole(testOwner)
        def result = queryController.validationAmounts()

        then:
        result != null
    }

    void 'validationAmount by id returns null when not found'() {
        when:
        withUserRole(testOwner)
        def result = queryController.validationAmount(999999L)

        then:
        result == null
    }

    // ==============================
    // QUERY CONTROLLER - receiptImages
    // ==============================

    void 'receiptImages returns list'() {
        when:
        withUserRole(testOwner)
        def result = queryController.receiptImages()

        then:
        result != null
    }

    void 'receiptImage by id returns null when not found'() {
        when:
        withUserRole(testOwner)
        def result = queryController.receiptImage(999999L)

        then:
        result == null
    }

    // ================================
    // QUERY CONTROLLER - medicalExpenses
    // ================================

    void 'medicalExpenses returns list'() {
        when:
        withUserRole(testOwner)
        def result = queryController.medicalExpenses()

        then:
        result != null
    }

    void 'medicalExpense by id returns null when not found'() {
        when:
        withUserRole(testOwner)
        def result = queryController.medicalExpense(999999L)

        then:
        result == null
    }

    void 'medicalExpensesByClaimStatus returns list for given status'() {
        when:
        withUserRole(testOwner)
        def result = queryController.medicalExpensesByClaimStatus(ClaimStatus.Submitted)

        then:
        result != null
    }

    // =================================
    // MUTATION CONTROLLER - categories
    // =================================

    void 'createCategory succeeds and deleteCategory removes it'() {
        given:
        String owner = shortOwner()
        String categoryName = "gqlcreate${owner}"
        def dto = new CategoryInputDto(null, categoryName, true)

        when:
        withUserRole(testOwner)
        def created = mutationController.createCategory(dto)

        then:
        created != null
        created.categoryName == categoryName

        when:
        def deleted = mutationController.deleteCategory(categoryName)

        then:
        deleted == true
    }

    void 'deleteCategory returns false when category not found'() {
        when:
        withUserRole(testOwner)
        def result = mutationController.deleteCategory('nonexistentcategory')

        then:
        result == false
    }

    void 'updateCategory with categoryId succeeds'() {
        given:
        String owner = shortOwner()
        String catName = "gqlupdc${owner}"
        def createDto = new CategoryInputDto(null, catName, true)
        withUserRole(testOwner)
        def created = mutationController.createCategory(createDto)
        def updateDto = new CategoryInputDto(created.categoryId, catName, true)

        when:
        def result = mutationController.updateCategory(updateDto, null)

        then:
        result != null
        result.categoryName == catName
    }

    // ==================================
    // MUTATION CONTROLLER - descriptions
    // ==================================

    void 'createDescription succeeds and deleteDescription removes it'() {
        given:
        String owner = shortOwner()
        String descName = "gqldescnew${owner}"
        def dto = new DescriptionInputDto(null, descName, true)

        when:
        withUserRole(testOwner)
        def created = mutationController.createDescription(dto)

        then:
        created != null
        created.descriptionName == descName

        when:
        def deleted = mutationController.deleteDescription(descName)

        then:
        deleted == true
    }

    void 'deleteDescription returns false when description not found'() {
        when:
        withUserRole(testOwner)
        def result = mutationController.deleteDescription('nonexistentdescription')

        then:
        result == false
    }

    void 'updateDescription with descriptionId succeeds'() {
        given:
        String owner = shortOwner()
        String descName = "gqlupdesc${owner}"
        def createDto = new DescriptionInputDto(null, descName, true)
        withUserRole(testOwner)
        def created = mutationController.createDescription(createDto)
        def updateDto = new DescriptionInputDto(created.descriptionId, descName, true)

        when:
        def result = mutationController.updateDescription(updateDto, null)

        then:
        result != null
        result.descriptionName == descName
    }

    // =================================
    // MUTATION CONTROLLER - parameters
    // =================================

    void 'createParameter succeeds and deleteParameter removes it'() {
        given:
        String owner = shortOwner()
        String paramName = "gqlparam${owner}"
        String paramValue = "gqlvalue${owner}"
        def dto = new ParameterInputDto(null, paramName, paramValue, true)

        when:
        withUserRole(testOwner)
        def created = mutationController.createParameter(dto)

        then:
        created != null
        created.parameterName == paramName

        when:
        def deleted = mutationController.deleteParameter(created.parameterId)

        then:
        deleted == true
    }

    void 'deleteParameter returns false when not found'() {
        when:
        withUserRole(testOwner)
        def result = mutationController.deleteParameter(999999L)

        then:
        result == false
    }

    // ================================
    // MUTATION CONTROLLER - payments
    // ================================

    void 'createPayment succeeds and deletePayment removes it'() {
        given:
        String srcAccount = testDataManager.createAccountFor(testOwner, 'gqlpaysrc', 'debit', true)
        String dstAccount = testDataManager.createAccountFor(testOwner, 'gqlpaydst', 'credit', true)
        def dto = new PaymentInputDto(
            null, srcAccount, dstAccount,
            LocalDate.of(2024, 1, 15),
            new BigDecimal('50.00'),
            null, null, true
        )

        when:
        withUserRole(testOwner)
        def created = mutationController.createPayment(dto)

        then:
        created != null
        created.sourceAccount == srcAccount
        created.destinationAccount == dstAccount

        when:
        def deleted = mutationController.deletePayment(created.paymentId)

        then:
        deleted == true
    }

    void 'deletePayment returns false when not found'() {
        when:
        withUserRole(testOwner)
        def result = mutationController.deletePayment(999999L)

        then:
        result == false
    }

    // ================================
    // MUTATION CONTROLLER - transfers
    // ================================

    void 'deleteTransfer returns false when not found'() {
        when:
        withUserRole(testOwner)
        def result = mutationController.deleteTransfer(999999L)

        then:
        result == false
    }

    // ================================
    // MUTATION CONTROLLER - accounts
    // ================================

    void 'createAccount succeeds and deleteAccount removes it'() {
        given:
        String owner = shortOwner()
        String accountName = "gqlnew_${owner}"
        def dto = new AccountInputDto(
            null, accountName, AccountType.Debit,
            true, '1234',
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            null, null
        )

        when:
        withUserRole(testOwner)
        def created = mutationController.createAccount(dto)

        then:
        created != null
        created.accountNameOwner == accountName

        when:
        def deleted = mutationController.deleteAccount(accountName)

        then:
        deleted == true
    }

    void 'deleteAccount returns false when not found'() {
        when:
        withUserRole(testOwner)
        def result = mutationController.deleteAccount('nonexistent_account')

        then:
        result == false
    }

    // ==================================
    // MUTATION CONTROLLER - transactions
    // ==================================

    void 'createTransaction succeeds with valid account and category'() {
        given:
        String accountName = testDataManager.createAccountFor(testOwner, 'gqltxnew', 'debit', true)
        String ownerClean = testOwner.replaceAll(/[^a-z]/, '').toLowerCase() ?: 'testowner'
        String categoryName = "test_category_${ownerClean}"

        def dto = new TransactionInputDto(
            null, null, null,
            AccountType.Debit,
            TransactionType.Expense,
            accountName,
            LocalDate.of(2024, 1, 15),
            'test transaction description',
            categoryName,
            new BigDecimal('25.00'),
            TransactionState.Cleared,
            true, null, null, null, null
        )

        when:
        withUserRole(testOwner)
        def created = mutationController.createTransaction(dto)

        then:
        created != null
        created.accountNameOwner == accountName
    }

    void 'deleteTransaction returns false when not found'() {
        when:
        withUserRole(testOwner)
        def result = mutationController.deleteTransaction('00000000-0000-0000-0000-000000000000')

        then:
        result == false
    }

    void 'updateTransaction succeeds with valid guid'() {
        given:
        String accountName = testDataManager.createAccountFor(testOwner, 'gqltxupd', 'debit', true)
        String ownerClean = testOwner.replaceAll(/[^a-z]/, '').toLowerCase() ?: 'testowner'
        String categoryName = "test_category_${ownerClean}"
        def createDto = new TransactionInputDto(
            null, null, null,
            AccountType.Debit,
            TransactionType.Expense,
            accountName,
            LocalDate.of(2024, 1, 15),
            'test transaction one',
            categoryName,
            new BigDecimal('30.00'),
            TransactionState.Cleared,
            true, null, null, null, null
        )
        withUserRole(testOwner)
        def created = mutationController.createTransaction(createDto)

        def updateDto = new TransactionInputDto(
            created.transactionId, created.guid, created.accountId,
            AccountType.Debit,
            TransactionType.Expense,
            accountName,
            LocalDate.of(2024, 2, 1),
            'updated transaction description',
            categoryName,
            new BigDecimal('35.00'),
            TransactionState.Outstanding,
            true, ReoccurringType.Undefined, 'some notes', null, null
        )

        when:
        def updated = mutationController.updateTransaction(updateDto)

        then:
        updated != null
        updated.guid == created.guid
    }

    // ====================================
    // UPDATE WITH oldName / byName BRANCHES
    // ====================================

    void 'updateCategory with oldCategoryName lookup succeeds'() {
        given:
        String owner = shortOwner()
        String catName = "gqlocat${owner}"
        def createDto = new CategoryInputDto(null, catName, true)
        withUserRole(testOwner)
        mutationController.createCategory(createDto)
        def updateDto = new CategoryInputDto(null, catName, true)

        when:
        def result = mutationController.updateCategory(updateDto, catName)

        then:
        result != null
        result.categoryName == catName
    }

    void 'updateCategory by categoryName lookup when no id or oldName provided'() {
        given:
        String owner = shortOwner()
        String catName = "gqlbcat${owner}"
        def createDto = new CategoryInputDto(null, catName, true)
        withUserRole(testOwner)
        mutationController.createCategory(createDto)
        def updateDto = new CategoryInputDto(null, catName, true)

        when:
        def result = mutationController.updateCategory(updateDto, null)

        then:
        result != null
        result.categoryName == catName
    }

    void 'updateDescription with oldDescriptionName lookup succeeds'() {
        given:
        String owner = shortOwner()
        String descName = "gqlodesn${owner}"
        def createDto = new DescriptionInputDto(null, descName, true)
        withUserRole(testOwner)
        mutationController.createDescription(createDto)
        def updateDto = new DescriptionInputDto(null, descName, true)

        when:
        def result = mutationController.updateDescription(updateDto, descName)

        then:
        result != null
        result.descriptionName == descName
    }

    void 'updateDescription by descriptionName lookup when no id or oldName provided'() {
        given:
        String owner = shortOwner()
        String descName = "gqlbdesn${owner}"
        def createDto = new DescriptionInputDto(null, descName, true)
        withUserRole(testOwner)
        mutationController.createDescription(createDto)
        def updateDto = new DescriptionInputDto(null, descName, true)

        when:
        def result = mutationController.updateDescription(updateDto, null)

        then:
        result != null
        result.descriptionName == descName
    }

    void 'updateAccount with oldAccountNameOwner lookup succeeds'() {
        given:
        String owner = shortOwner()
        String accountName = "gqloact_${owner}"
        def createDto = new AccountInputDto(
            null, accountName, AccountType.Debit,
            true, '5678',
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            null, null
        )
        withUserRole(testOwner)
        mutationController.createAccount(createDto)
        def updateDto = new AccountInputDto(
            null, accountName, AccountType.Debit,
            true, '5678',
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            null, null
        )

        when:
        def result = mutationController.updateAccount(updateDto, accountName)

        then:
        result != null
        result.accountNameOwner == accountName
    }

    void 'updateAccount by accountNameOwner lookup when no id or oldName provided'() {
        given:
        String owner = shortOwner()
        String accountName = "gqlbact_${owner}"
        def createDto = new AccountInputDto(
            null, accountName, AccountType.Debit,
            true, '7890',
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            null, null
        )
        withUserRole(testOwner)
        mutationController.createAccount(createDto)
        def updateDto = new AccountInputDto(
            null, accountName, AccountType.Debit,
            true, '7890',
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            null, null
        )

        when:
        def result = mutationController.updateAccount(updateDto, null)

        then:
        result != null
        result.accountNameOwner == accountName
    }

    // ===================================
    // MUTATION CONTROLLER - validationAmounts
    // ===================================

    void 'createValidationAmount succeeds and deleteValidationAmount removes it'() {
        given:
        String accountName = testDataManager.createAccountFor(testOwner, 'gqlvaacct', 'debit', true)
        withUserRole(testOwner)
        def account = queryController.account(accountName)
        def dto = new ValidationAmountInputDto(
            null,
            account.accountId,
            new Timestamp(System.currentTimeMillis()),
            true,
            TransactionState.Cleared,
            new BigDecimal('200.00')
        )

        when:
        def created = mutationController.createValidationAmount(dto)

        then:
        created != null
        created.amount == new BigDecimal('200.00')

        when:
        def deleted = mutationController.deleteValidationAmount(created.validationId)

        then:
        deleted == true
    }

    void 'updateValidationAmount succeeds with valid id'() {
        given:
        String accountName = testDataManager.createAccountFor(testOwner, 'gqlvuacct', 'debit', true)
        withUserRole(testOwner)
        def account = queryController.account(accountName)
        def createDto = new ValidationAmountInputDto(
            null,
            account.accountId,
            new Timestamp(System.currentTimeMillis()),
            true,
            TransactionState.Cleared,
            new BigDecimal('150.00')
        )
        def created = mutationController.createValidationAmount(createDto)
        def updateDto = new ValidationAmountInputDto(
            created.validationId,
            account.accountId,
            new Timestamp(System.currentTimeMillis()),
            true,
            TransactionState.Outstanding,
            new BigDecimal('175.00')
        )

        when:
        def updated = mutationController.updateValidationAmount(updateDto)

        then:
        updated != null
        updated.validationId == created.validationId
    }

    void 'updateParameter succeeds with valid id'() {
        given:
        String owner = shortOwner()
        String paramName = "gqlpupd${owner}"
        String paramValue = "gqlvupd${owner}"
        def createDto = new ParameterInputDto(null, paramName, paramValue, true)
        withUserRole(testOwner)
        def created = mutationController.createParameter(createDto)
        def updateDto = new ParameterInputDto(
            created.parameterId,
            paramName,
            'updatedvalue',
            true
        )

        when:
        def updated = mutationController.updateParameter(updateDto)

        then:
        updated != null
        updated.parameterId == created.parameterId
    }

    void 'createAccount with all nullable fields null uses defaults'() {
        given:
        String owner = shortOwner()
        String accountName = "gqldeflt_${owner}"
        def dto = new AccountInputDto(
            null, accountName, AccountType.Credit,
            null, null,
            null, null, null,
            null, null
        )

        when:
        withUserRole(testOwner)
        def result = mutationController.createAccount(dto)

        then:
        result != null
        result.accountNameOwner == accountName
        result.moniker == '0000'
    }

    // =====================================================
    // NULL activeStatus BRANCHES - cover ?: default paths
    // =====================================================

    void 'createPayment with null activeStatus uses default true'() {
        given:
        String srcAccount = testDataManager.createAccountFor(testOwner, 'gqlpnasrc', 'debit', true)
        String dstAccount = testDataManager.createAccountFor(testOwner, 'gqlpnaDst', 'credit', true)
        def dto = new PaymentInputDto(
            null, srcAccount, dstAccount,
            LocalDate.of(2024, 3, 1),
            new BigDecimal('40.00'),
            null, null, null
        )

        when:
        withUserRole(testOwner)
        def result = mutationController.createPayment(dto)

        then:
        result != null
        result.activeStatus == true
    }

    void 'createTransfer with null activeStatus uses default true'() {
        given:
        String srcAccount = testDataManager.createAccountFor(testOwner, 'gqltnasrc', 'debit', true)
        String dstAccount = testDataManager.createAccountFor(testOwner, 'gqltnaDst', 'credit', true)
        def dto = new TransferInputDto(
            null, srcAccount, dstAccount,
            LocalDate.of(2024, 3, 1),
            new BigDecimal('50.00'),
            null, null, null
        )

        when:
        withUserRole(testOwner)
        def result = mutationController.createTransfer(dto)

        then:
        result != null
        result.activeStatus == true
    }

    void 'createParameter with null activeStatus uses default true'() {
        given:
        String owner = shortOwner()
        def dto = new ParameterInputDto(null, "gqlparmnull${owner}", "gqlvalnull${owner}", null)

        when:
        withUserRole(testOwner)
        def result = mutationController.createParameter(dto)

        then:
        result != null
        result.activeStatus == true
    }

    void 'createCategory with null activeStatus uses default true'() {
        given:
        String owner = shortOwner()
        def dto = new CategoryInputDto(null, "gqlcatnull${owner}", null)

        when:
        withUserRole(testOwner)
        def result = mutationController.createCategory(dto)

        then:
        result != null
        result.activeStatus == true
    }

    void 'createDescription with null activeStatus uses default true'() {
        given:
        String owner = shortOwner()
        def dto = new DescriptionInputDto(null, "gqldscnull${owner}", null)

        when:
        withUserRole(testOwner)
        def result = mutationController.createDescription(dto)

        then:
        result != null
        result.activeStatus == true
    }

    void 'createMedicalExpense with null activeStatus uses default true'() {
        given:
        def dto = new MedicalExpenseInputDto(
            null, null, null, null,
            LocalDate.of(2024, 4, 1),
            'Null activeStatus test', null, null,
            new BigDecimal('100.00'),
            new BigDecimal('20.00'),
            new BigDecimal('60.00'),
            new BigDecimal('20.00'),
            null,
            false,
            'CLMNULL01',
            ClaimStatus.Submitted,
            null,
            new BigDecimal('20.00')
        )

        when:
        withUserRole(testOwner)
        def result = mutationController.createMedicalExpense(dto)

        then:
        result != null
        result.activeStatus == true
    }

    void 'createValidationAmount with null activeStatus uses default true'() {
        given:
        String accountName = testDataManager.createAccountFor(testOwner, 'gqlvanactl', 'debit', true)
        withUserRole(testOwner)
        def account = queryController.account(accountName)
        def dto = new ValidationAmountInputDto(
            null,
            account.accountId,
            new Timestamp(System.currentTimeMillis()),
            null,
            TransactionState.Cleared,
            new BigDecimal('300.00')
        )

        when:
        def result = mutationController.createValidationAmount(dto)

        then:
        result != null
        result.activeStatus == true
    }

    void 'createTransaction with null activeStatus and null optional fields uses defaults'() {
        given:
        String accountName = testDataManager.createAccountFor(testOwner, 'gqltxnull', 'debit', true)
        String ownerClean = testOwner.replaceAll(/[^a-z]/, '').toLowerCase() ?: 'testowner'
        String categoryName = "test_category_${ownerClean}"
        def dto = new TransactionInputDto(
            null, null, null,
            AccountType.Debit,
            TransactionType.Expense,
            accountName,
            LocalDate.of(2024, 4, 15),
            'null defaults test transaction',
            categoryName,
            new BigDecimal('15.00'),
            TransactionState.Outstanding,
            null, null, null, null, null
        )

        when:
        withUserRole(testOwner)
        def result = mutationController.createTransaction(dto)

        then:
        result != null
        result.activeStatus == true
        result.notes == ''
    }

    void 'updateAccount with all null optional fields uses defaults'() {
        given:
        String owner = shortOwner()
        String accountName = "gqlactnull_${owner}"
        def createDto = new AccountInputDto(
            null, accountName, AccountType.Debit,
            true, '1111',
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            null, null
        )
        withUserRole(testOwner)
        mutationController.createAccount(createDto)
        def updateDto = new AccountInputDto(
            null, accountName, AccountType.Debit,
            null, null,
            null, null, null,
            null, null
        )

        when:
        def result = mutationController.updateAccount(updateDto, null)

        then:
        result != null
        result.accountNameOwner == accountName
        result.activeStatus == true
        result.moniker == '0000'
    }

    void 'updateTransaction with null optional fields uses defaults'() {
        given:
        String accountName = testDataManager.createAccountFor(testOwner, 'gqltxnuUpd', 'debit', true)
        String ownerClean = testOwner.replaceAll(/[^a-z]/, '').toLowerCase() ?: 'testowner'
        String categoryName = "test_category_${ownerClean}"
        def createDto = new TransactionInputDto(
            null, null, null,
            AccountType.Debit,
            TransactionType.Expense,
            accountName,
            LocalDate.of(2024, 4, 15),
            'null update test transaction',
            categoryName,
            new BigDecimal('20.00'),
            TransactionState.Outstanding,
            true, ReoccurringType.Undefined, 'some notes', null, null
        )
        withUserRole(testOwner)
        def created = mutationController.createTransaction(createDto)
        def updateDto = new TransactionInputDto(
            created.transactionId, created.guid, created.accountId,
            AccountType.Debit,
            TransactionType.Expense,
            accountName,
            LocalDate.of(2024, 5, 1),
            'null update test transaction',
            categoryName,
            new BigDecimal('22.00'),
            TransactionState.Cleared,
            null, null, null, null, null
        )

        when:
        def result = mutationController.updateTransaction(updateDto)

        then:
        result != null
        result.guid == created.guid
        result.activeStatus == true
        result.notes == ''
    }

    void 'updatePayment with null activeStatus uses default'() {
        given:
        String srcAccount = testDataManager.createAccountFor(testOwner, 'gqlupnasc', 'debit', true)
        String dstAccount = testDataManager.createAccountFor(testOwner, 'gqlupnaDst', 'credit', true)
        def createDto = new PaymentInputDto(
            null, srcAccount, dstAccount,
            LocalDate.of(2024, 3, 5),
            new BigDecimal('55.00'),
            null, null, true
        )
        withUserRole(testOwner)
        def created = mutationController.createPayment(createDto)
        def updateDto = new PaymentInputDto(
            null, srcAccount, dstAccount,
            LocalDate.of(2024, 3, 6),
            new BigDecimal('60.00'),
            null, null, null
        )

        when:
        def updated = mutationController.updatePayment(created.paymentId, updateDto)

        then:
        updated != null
        updated.activeStatus == true
    }

    void 'updateTransfer with null activeStatus uses default'() {
        given:
        String srcAccount = testDataManager.createAccountFor(testOwner, 'gqlutnasc', 'debit', true)
        String dstAccount = testDataManager.createAccountFor(testOwner, 'gqlutnaDst', 'credit', true)
        def createDto = new TransferInputDto(
            null, srcAccount, dstAccount,
            LocalDate.of(2024, 3, 5),
            new BigDecimal('80.00'),
            null, null, true
        )
        withUserRole(testOwner)
        def created = mutationController.createTransfer(createDto)
        def updateDto = new TransferInputDto(
            null, srcAccount, dstAccount,
            LocalDate.of(2024, 3, 6),
            new BigDecimal('85.00'),
            null, null, null
        )

        when:
        def updated = mutationController.updateTransfer(created.transferId, updateDto)

        then:
        updated != null
        updated.activeStatus == true
    }

    void 'updateValidationAmount with null activeStatus uses default'() {
        given:
        String accountName = testDataManager.createAccountFor(testOwner, 'gqlvunaact', 'debit', true)
        withUserRole(testOwner)
        def account = queryController.account(accountName)
        def createDto = new ValidationAmountInputDto(
            null,
            account.accountId,
            new Timestamp(System.currentTimeMillis()),
            true,
            TransactionState.Cleared,
            new BigDecimal('250.00')
        )
        def created = mutationController.createValidationAmount(createDto)
        def updateDto = new ValidationAmountInputDto(
            created.validationId,
            account.accountId,
            new Timestamp(System.currentTimeMillis()),
            null,
            TransactionState.Outstanding,
            new BigDecimal('260.00')
        )

        when:
        def updated = mutationController.updateValidationAmount(updateDto)

        then:
        updated != null
        updated.activeStatus == true
    }

    void 'updateCategory with null activeStatus uses default'() {
        given:
        String owner = shortOwner()
        String catName = "gqlcatnuup${owner}"
        def createDto = new CategoryInputDto(null, catName, true)
        withUserRole(testOwner)
        def created = mutationController.createCategory(createDto)
        def updateDto = new CategoryInputDto(created.categoryId, catName, null)

        when:
        def result = mutationController.updateCategory(updateDto, null)

        then:
        result != null
        result.activeStatus == true
    }

    void 'updateDescription with null activeStatus uses default'() {
        given:
        String owner = shortOwner()
        String descName = "gqldscnuup${owner}"
        def createDto = new DescriptionInputDto(null, descName, true)
        withUserRole(testOwner)
        def created = mutationController.createDescription(createDto)
        def updateDto = new DescriptionInputDto(created.descriptionId, descName, null)

        when:
        def result = mutationController.updateDescription(updateDto, null)

        then:
        result != null
        result.activeStatus == true
    }

    void 'updateMedicalExpense with null activeStatus uses default'() {
        given:
        def createDto = new MedicalExpenseInputDto(
            null, null, null, null,
            LocalDate.of(2024, 5, 1),
            'Update null activeStatus', null, null,
            new BigDecimal('400.00'),
            new BigDecimal('100.00'),
            new BigDecimal('200.00'),
            new BigDecimal('100.00'),
            null,
            false,
            'CLMNULL02',
            ClaimStatus.Submitted,
            true,
            new BigDecimal('100.00')
        )
        withUserRole(testOwner)
        def created = mutationController.createMedicalExpense(createDto)
        def updateDto = new MedicalExpenseInputDto(
            created.medicalExpenseId, null, null, null,
            LocalDate.of(2024, 5, 1),
            'Update null activeStatus', null, null,
            new BigDecimal('400.00'),
            new BigDecimal('100.00'),
            new BigDecimal('200.00'),
            new BigDecimal('100.00'),
            null,
            false,
            'CLMNULL02',
            ClaimStatus.Paid,
            null,
            new BigDecimal('100.00')
        )

        when:
        def result = mutationController.updateMedicalExpense(updateDto)

        then:
        result != null
        result.activeStatus == true
    }

    void 'updateParameter with null activeStatus uses default'() {
        given:
        String owner = shortOwner()
        String paramName = "gqlparmnuup${owner}"
        def createDto = new ParameterInputDto(null, paramName, 'val', true)
        withUserRole(testOwner)
        def created = mutationController.createParameter(createDto)
        def updateDto = new ParameterInputDto(created.parameterId, paramName, 'updval', null)

        when:
        def result = mutationController.updateParameter(updateDto)

        then:
        result != null
        result.activeStatus == true
    }

    // ================================
    // MUTATION CONTROLLER - updatePayment
    // ================================

    void 'updatePayment succeeds with valid id'() {
        given:
        String srcAccount = testDataManager.createAccountFor(testOwner, 'gqlupaysrc', 'debit', true)
        String dstAccount = testDataManager.createAccountFor(testOwner, 'gqlupayDst', 'credit', true)
        def createDto = new PaymentInputDto(
            null, srcAccount, dstAccount,
            LocalDate.of(2024, 1, 15),
            new BigDecimal('60.00'),
            null, null, true
        )
        withUserRole(testOwner)
        def created = mutationController.createPayment(createDto)
        def updateDto = new PaymentInputDto(
            null, srcAccount, dstAccount,
            LocalDate.of(2024, 2, 20),
            new BigDecimal('75.00'),
            null, null, true
        )

        when:
        def updated = mutationController.updatePayment(created.paymentId, updateDto)

        then:
        updated != null
        updated.paymentId == created.paymentId
        updated.amount == new BigDecimal('75.00')
    }

    // ================================
    // MUTATION CONTROLLER - updateTransfer
    // ================================

    void 'updateTransfer succeeds with valid id'() {
        given:
        String srcAccount = testDataManager.createAccountFor(testOwner, 'gqlutxfsrc', 'debit', true)
        String dstAccount = testDataManager.createAccountFor(testOwner, 'gqlutxfDst', 'credit', true)
        def createDto = new TransferInputDto(
            null, srcAccount, dstAccount,
            LocalDate.of(2024, 1, 15),
            new BigDecimal('100.00'),
            null, null, true
        )
        withUserRole(testOwner)
        def created = mutationController.createTransfer(createDto)
        def updateDto = new TransferInputDto(
            null, srcAccount, dstAccount,
            LocalDate.of(2024, 3, 10),
            new BigDecimal('125.00'),
            null, null, true
        )

        when:
        def updated = mutationController.updateTransfer(created.transferId, updateDto)

        then:
        updated != null
        updated.transferId == created.transferId
        updated.amount == new BigDecimal('125.00')
    }

    // ================================
    // MUTATION CONTROLLER - medicalExpenses
    // ================================

    void 'createMedicalExpense succeeds and deleteMedicalExpense removes it'() {
        given:
        def dto = new MedicalExpenseInputDto(
            null, null, null, null,
            LocalDate.of(2024, 1, 10),
            'Annual checkup', null, null,
            new BigDecimal('200.00'),
            new BigDecimal('50.00'),
            new BigDecimal('100.00'),
            new BigDecimal('50.00'),
            null,
            false,
            'CLM-001',
            ClaimStatus.Submitted,
            true,
            new BigDecimal('50.00')
        )

        when:
        withUserRole(testOwner)
        def created = mutationController.createMedicalExpense(dto)

        then:
        created != null
        created.claimNumber == 'CLM-001'

        when:
        def deleted = mutationController.deleteMedicalExpense(created.medicalExpenseId)

        then:
        deleted == true
    }

    void 'deleteMedicalExpense returns false when not found'() {
        when:
        withUserRole(testOwner)
        def result = mutationController.deleteMedicalExpense(999999L)

        then:
        result == false
    }

    void 'updateMedicalExpense succeeds with valid id'() {
        given:
        def createDto = new MedicalExpenseInputDto(
            null, null, null, null,
            LocalDate.of(2024, 2, 15),
            'Specialist visit', null, null,
            new BigDecimal('300.00'),
            new BigDecimal('75.00'),
            new BigDecimal('150.00'),
            new BigDecimal('75.00'),
            null,
            false,
            'CLM-002',
            ClaimStatus.Submitted,
            true,
            new BigDecimal('75.00')
        )
        withUserRole(testOwner)
        def created = mutationController.createMedicalExpense(createDto)
        def updateDto = new MedicalExpenseInputDto(
            created.medicalExpenseId, null, null, null,
            LocalDate.of(2024, 2, 15),
            'Specialist visit updated', null, null,
            new BigDecimal('350.00'),
            new BigDecimal('80.00'),
            new BigDecimal('170.00'),
            new BigDecimal('100.00'),
            LocalDate.of(2024, 3, 1),
            false,
            'CLM-002',
            ClaimStatus.Paid,
            true,
            new BigDecimal('100.00')
        )

        when:
        def updated = mutationController.updateMedicalExpense(updateDto)

        then:
        updated != null
        updated.medicalExpenseId == created.medicalExpenseId
        updated.claimStatus == ClaimStatus.Paid
    }
}
