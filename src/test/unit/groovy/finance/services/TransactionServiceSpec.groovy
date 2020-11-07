package finance.services

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Category
import finance.domain.Transaction
import finance.helpers.AccountBuilder
import finance.helpers.CategoryBuilder
import finance.repositories.AccountRepository
import finance.repositories.CategoryRepository
import finance.repositories.TransactionRepository
import org.hibernate.NonUniqueResultException
import spock.lang.Specification

import javax.validation.Validator

class TransactionServiceSpec extends Specification {
    TransactionRepository mockTransactionRepository = Mock(TransactionRepository)
    AccountRepository mockAccountRepository = Mock(AccountRepository)
    Validator mockValidator = Mock(Validator)
    AccountService accountService = new AccountService(mockAccountRepository, mockValidator)
    CategoryRepository mockCategoryRepository = Mock(CategoryRepository)
    CategoryService categoryService = new CategoryService(mockCategoryRepository)
    TransactionService transactionService = new TransactionService(mockTransactionRepository, accountService, categoryService, mockValidator)
    Category category = CategoryBuilder.builder().build()

    def "test transactionService - deleteByGuid"() {
        given:
        def guid = '123'
        Transaction transaction = new Transaction()
        Optional<Transaction> transactionOptional = Optional.of(transaction)

        when:
        def isDeleted = transactionService.deleteTransactionByGuid(guid)

        then:
        isDeleted
        1 * mockTransactionRepository.deleteByGuid(guid)
        1 * mockTransactionRepository.findByGuid(guid) >> transactionOptional
        0 * _
    }

    def "test transactionService - deleteByGuid - no record returned because of invalid guid"() {
        given:
        def guid = '123'
        Optional<Transaction> transactionOptional = Optional.empty()

        when:
        def isDeleted = transactionService.deleteTransactionByGuid(guid)

        then:
        !isDeleted
        1 * mockTransactionRepository.findByGuid(guid) >> transactionOptional
        0 * _
    }

    def "test transactionService - findByGuid"() {
        given:
        def guid = '123'
        Transaction transaction = new Transaction()
        Optional<Transaction> transactionOptional = Optional.of(transaction)

        when:
        transactionService.findTransactionByGuid(guid)

        then:
        1 * mockTransactionRepository.findByGuid(guid) >> transactionOptional
        0 * _
    }

    def "test transactionService - findByGuid - duplicates returned"() {
        given:
        def guid = '123'

        when:
        transactionService.findTransactionByGuid(guid)

        then:
        NonUniqueResultException ex = thrown()
        ex.message.contains("query did not return a unique result")
        1 * mockTransactionRepository.findByGuid(guid) >> { throw new NonUniqueResultException(2) }
        0 * _
    }

    def "test transactionService - insert valid transaction"() {
        given:
        def categoryName = 'my-category'
        def accountName = 'my-account-name'
        def guid = '123'
        Transaction transaction = new Transaction()
        Account account = new Account()
        Category category = new Category()
        Optional<Account> accountOptional = Optional.of(account)
        Optional<Category> categoryOptional = Optional.of(category)
        transaction.guid = guid
        transaction.accountNameOwner = accountName
        transaction.category = categoryName

        when:
        def isInserted = transactionService.insertTransaction(transaction)

        then:
        isInserted.is(true)
        1 * mockTransactionRepository.findByGuid(guid) >> Optional.empty()
        1 * mockValidator.validate(transaction) >> new HashSet()
        1 * mockAccountRepository.findByAccountNameOwner(accountName) >> accountOptional
        1 * mockCategoryRepository.findByCategory(categoryName) >> categoryOptional
        1 * mockTransactionRepository.saveAndFlush(transaction) >> true
        0 * _
    }

    def "test transactionService - attempt to insert duplicate transaction"() {
        given:
        def categoryName = 'my-category'
        def accountName = 'my-account-name'
        def guid = '123'
        Transaction transaction = new Transaction()
        Optional<Transaction> transactionOptional = Optional.of(transaction)
        transaction.guid = guid
        transaction.accountNameOwner = accountName
        transaction.category = categoryName

        when:
        def isInserted = transactionService.insertTransaction(transaction)

        then:
        !isInserted.is(true)
        1 * mockValidator.validate(transaction) >> new HashSet()
        1 * mockTransactionRepository.findByGuid(guid) >> transactionOptional
        0 * _
    }

    def "test transactionService - insert valid transaction where account name does exist"() {
        given:
        def categoryName = 'my-category'
        def accountName = 'my-account-name'
        def guid = '123'
        Transaction transaction = new Transaction()
        Account account = new Account()
        Category category = new Category()
        Optional<Account> accountOptional = Optional.of(account)
        Optional<Category> categoryOptional = Optional.of(category)
        transaction.guid = guid
        transaction.accountNameOwner = accountName
        transaction.category = categoryName

        when:
        def isInserted = transactionService.insertTransaction(transaction)

        then:
        isInserted.is(true)
        1 * mockTransactionRepository.findByGuid(guid) >> Optional.empty()
        1 * mockAccountRepository.findByAccountNameOwner(accountName) >> accountOptional
        1 * mockValidator.validate(transaction) >> new HashSet()
        1 * mockCategoryRepository.findByCategory(categoryName) >> categoryOptional
        1 * mockTransactionRepository.saveAndFlush(transaction) >> true
        0 * _
    }

    def "test transactionService - insert valid transaction where account name does not exist"() {
        given:
        def categoryName = 'my-category'
        def accountName = 'my-account-name'
        def guid = '123'
        Transaction transaction = new Transaction()
        Account account = transactionService.createDefaultAccount(accountName, AccountType.Undefined)
        Category category = CategoryBuilder.builder().build()
        category.category = categoryName
        Optional<Account> accountOptional = Optional.of(account)
        Optional<Category> categoryOptional = Optional.of(category)
        transaction.guid = guid
        transaction.accountNameOwner = accountName
        transaction.category = categoryName

        when:
        def isInserted = transactionService.insertTransaction(transaction)

        then:
        isInserted
        1 * mockTransactionRepository.findByGuid(guid) >> Optional.empty()
        1 * mockAccountRepository.findByAccountNameOwner(accountName) >> Optional.empty()
        1 * mockAccountRepository.saveAndFlush(account) >> true
        1 * mockValidator.validate(transaction) >> new HashSet()
        1 * mockAccountRepository.findByAccountNameOwner(accountName) >> Optional.empty()
        1 * mockAccountRepository.findByAccountNameOwner(accountName) >> accountOptional
        1 * mockValidator.validate(account) >> new HashSet()
        1 * mockCategoryRepository.findByCategory(categoryName) >> categoryOptional
        1 * mockTransactionRepository.saveAndFlush(transaction) >> true
        0 * _
    }

    def "test transactionService - insert a valid transaction where category name does not exist"() {
        given:
        def categoryName = 'my-category'
        def accountName = 'my-account-name'
        def guid = '123'
        Transaction transaction = new Transaction()
        Account account = new Account()
        Optional<Account> accountOptional = Optional.of(account)
        transaction.guid = guid
        transaction.accountNameOwner = accountName
        transaction.category = categoryName
        category.category = categoryName
        category.categoryId = 0

        when:
        def isInserted = transactionService.insertTransaction(transaction)

        then:
        isInserted
        1 * mockTransactionRepository.findByGuid(guid) >> Optional.empty()
        1 * mockValidator.validate(transaction) >> new HashSet()
        1 * mockAccountRepository.findByAccountNameOwner(accountName) >> accountOptional
        1 * mockCategoryRepository.findByCategory(categoryName) >> Optional.empty()
        1 * mockCategoryRepository.saveAndFlush(category)
        1 * mockTransactionRepository.saveAndFlush(transaction) >> true
        0 * _
    }
}
