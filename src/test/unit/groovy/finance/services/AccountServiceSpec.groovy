package finance.services

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.ServiceResult
import finance.domain.TransactionState
import finance.helpers.AccountBuilder
import finance.repositories.AccountRepository
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import jakarta.persistence.EntityNotFoundException
import java.math.BigDecimal
import java.sql.Timestamp

/**
 * TDD Specification for AccountService
 * Tests the Account service using new ServiceResult pattern with comprehensive error handling
 */
class StandardizedAccountServiceSpec extends BaseServiceSpec {

    def accountRepositoryMock = Mock(AccountRepository)
    def validationAmountRepositoryMock = Mock(finance.repositories.ValidationAmountRepository)
    def transactionRepositoryMock = Mock(finance.repositories.TransactionRepository)
    def standardizedAccountService = new AccountService(accountRepositoryMock, validationAmountRepositoryMock, transactionRepositoryMock)

    void setup() {
        standardizedAccountService.meterService = meterService
        standardizedAccountService.validator = validatorMock
    }

    // ===== TDD Tests for findAllActive() =====

    def "findAllActive should return Success with accounts when found"() {
        given: "existing active accounts"
        def accounts = [
            AccountBuilder.builder().withAccountId(1L).withAccountNameOwner("account1").build(),
            AccountBuilder.builder().withAccountId(2L).withAccountNameOwner("account2").build()
        ]

        when: "finding all active accounts"
        def result = standardizedAccountService.findAllActive()

        then: "should return Success with accounts"
        1 * accountRepositoryMock.findByActiveStatusOrderByAccountNameOwner(true) >> accounts
        result instanceof ServiceResult.Success
        result.data.size() == 2
        result.data[0].accountId == 1L
        result.data[0].accountNameOwner == "account1"
        result.data[1].accountId == 2L
        result.data[1].accountNameOwner == "account2"
        0 * _
    }

    def "findAllActive should return Success with empty list when no accounts found"() {
        when: "finding all active accounts with none existing"
        def result = standardizedAccountService.findAllActive()

        then: "should return Success with empty list"
        1 * accountRepositoryMock.findByActiveStatusOrderByAccountNameOwner(true) >> []
        result instanceof ServiceResult.Success
        result.data.isEmpty()
        0 * _
    }

    // ===== TDD Tests for findById() =====

    def "findById should return Success with account when found by accountNameOwner"() {
        given: "existing account"
        def account = AccountBuilder.builder().withAccountNameOwner("test_account").build()

        when: "finding by valid accountNameOwner"
        def result = standardizedAccountService.findById("test_account")

        then: "should return Success with account"
        1 * accountRepositoryMock.findByAccountNameOwner("test_account") >> Optional.of(account)
        result instanceof ServiceResult.Success
        result.data.accountNameOwner == "test_account"
        0 * _
    }

    def "findById should return NotFound when account does not exist"() {
        when: "finding by non-existent accountNameOwner"
        def result = standardizedAccountService.findById("non_existent")

        then: "should return NotFound result"
        1 * accountRepositoryMock.findByAccountNameOwner("non_existent") >> Optional.empty()
        result instanceof ServiceResult.NotFound
        result.message.contains("Account not found: non_existent")
        0 * _
    }

    // ===== TDD Tests for save() =====

    def "save should return Success with saved account when valid"() {
        given: "valid account"
        def account = AccountBuilder.builder().build()
        def savedAccount = AccountBuilder.builder().withAccountId(1L).build()
        Set<ConstraintViolation<Account>> noViolations = [] as Set

        when: "saving account"
        def result = standardizedAccountService.save(account)

        then: "should return Success with saved account"
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.empty()
        1 * validatorMock.validate(account) >> noViolations
        1 * accountRepositoryMock.saveAndFlush(account) >> savedAccount
        result instanceof ServiceResult.Success
        result.data.accountId == 1L
        0 * _
    }

    def "save should return ValidationError when account has constraint violations"() {
        given: "invalid account"
        def account = AccountBuilder.builder().withAccountNameOwner("").build()
        ConstraintViolation<Account> violation = Mock(ConstraintViolation)
        def mockPath = Mock(javax.validation.Path)
        mockPath.toString() >> "accountNameOwner"
        violation.propertyPath >> mockPath
        violation.message >> "must not be blank"
        Set<ConstraintViolation<Account>> violations = [violation] as Set

        when: "saving invalid account"
        def result = standardizedAccountService.save(account)

        then: "should return ValidationError result"
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.empty()
        1 * validatorMock.validate(account) >> { throw new ConstraintViolationException("Validation failed", violations) }
        result instanceof ServiceResult.ValidationError
        result.errors.size() == 1
        result.errors.values().contains("must not be blank")
    }

    def "save should return BusinessError when account already exists"() {
        given: "account that already exists"
        def account = AccountBuilder.builder().build()
        def existingAccount = AccountBuilder.builder().withAccountId(1L).build()

        when: "saving duplicate account"
        def result = standardizedAccountService.save(account)

        then: "should return BusinessError result"
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.of(existingAccount)
        result instanceof ServiceResult.BusinessError
        result.message.toLowerCase().contains("account already exists")
        result.errorCode == "DATA_INTEGRITY_VIOLATION"
        0 * _
    }

    // ===== TDD Tests for update() =====

    def "update should return Success with updated account when exists"() {
        given: "existing account to update"
        def existingAccount = AccountBuilder.builder().withAccountId(1L).withAccountNameOwner("test_account").build()
        def updatedAccount = AccountBuilder.builder().withAccountId(1L).withAccountNameOwner("test_account").withActiveStatus(false).build()

        when: "updating existing account"
        def result = standardizedAccountService.update(updatedAccount)

        then: "should return Success with updated account"
        1 * accountRepositoryMock.findByAccountNameOwner("test_account") >> Optional.of(existingAccount)
        1 * accountRepositoryMock.saveAndFlush(_ as Account) >> { Account account ->
            assert account.activeStatus == false
            return account
        }
        result instanceof ServiceResult.Success
        result.data.activeStatus == false
        0 * _
    }

    def "update should return NotFound when account does not exist"() {
        given: "account with non-existent accountNameOwner"
        def account = AccountBuilder.builder().withAccountId(999L).withAccountNameOwner("foo_brian").build()

        when: "updating non-existent account"
        def result = standardizedAccountService.update(account)

        then: "should return NotFound result"
        1 * accountRepositoryMock.findByAccountNameOwner("foo_brian") >> Optional.empty()
        result instanceof ServiceResult.NotFound
        result.message.contains("Account not found: foo_brian")
        0 * _
    }

    // ===== TDD Tests for deleteById() =====

    def "deleteById should return Success when account exists"() {
        given: "existing account"
        def account = AccountBuilder.builder().withAccountId(100L).withAccountNameOwner("test_account").build()

        when: "deleting existing account"
        def result = standardizedAccountService.deleteById("test_account")

        then: "should return Success"
        1 * accountRepositoryMock.findByAccountNameOwner("test_account") >> Optional.of(account)
        1 * validationAmountRepositoryMock.findByAccountId(100L) >> []
        1 * accountRepositoryMock.delete(account)
        result instanceof ServiceResult.Success
        result.data == true
        0 * _
    }

    def "deleteById should return NotFound when account does not exist"() {
        when: "deleting non-existent account"
        def result = standardizedAccountService.deleteById("non_existent")

        then: "should return NotFound result"
        1 * accountRepositoryMock.findByAccountNameOwner("non_existent") >> Optional.empty()
        result instanceof ServiceResult.NotFound
        result.message.contains("Account not found: non_existent")
        0 * _
    }

    // ===== TDD Tests for Legacy Method Support =====

    def "accounts should delegate to findAllActive and return data"() {
        given: "existing accounts"
        def accounts = [AccountBuilder.builder().build()]

        when: "calling legacy accounts method"
        def result = standardizedAccountService.accounts()

        then: "should return account list"
        1 * accountRepositoryMock.findByActiveStatusOrderByAccountNameOwner(true) >> accounts
        result.size() == 1
        0 * _
    }

    def "account should delegate to findById and return Optional"() {
        given: "existing account"
        def account = AccountBuilder.builder().withAccountNameOwner("test_account").build()

        when: "calling legacy account method"
        def result = standardizedAccountService.account("test_account")

        then: "should return account optional"
        1 * accountRepositoryMock.findByAccountNameOwner("test_account") >> Optional.of(account)
        result.isPresent()
        result.get().accountNameOwner == "test_account"
        0 * _
    }

    def "insertAccount should delegate to save and return data"() {
        given: "valid account"
        def account = AccountBuilder.builder().build()
        def savedAccount = AccountBuilder.builder().withAccountId(1L).build()
        Set<ConstraintViolation<Account>> noViolations = [] as Set

        when: "calling legacy insertAccount method"
        def result = standardizedAccountService.insertAccount(account)

        then: "should return saved account"
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.empty()
        1 * validatorMock.validate(account) >> noViolations
        1 * accountRepositoryMock.saveAndFlush(account) >> savedAccount
        result.accountId == 1L
        0 * _
    }



    // ===== TDD Tests for Complex Business Operations =====

    def "sumOfAllTransactionsByTransactionState should return calculated sum"() {
        given: "transaction state"
        def transactionState = TransactionState.Cleared
        def expectedSum = new BigDecimal("100.50")

        when: "calculating sum by transaction state"
        def result = standardizedAccountService.sumOfAllTransactionsByTransactionState(transactionState)

        then: "should return calculated sum"
        1 * accountRepositoryMock.sumOfAllTransactionsByTransactionState(transactionState.toString()) >> expectedSum
        result == expectedSum
        0 * _
    }

    def "findAccountsThatRequirePayment should return accounts needing payment"() {
        given: "accounts that require payment"
        def accounts = [AccountBuilder.builder().withOutstanding(new BigDecimal("100.00")).build()]

        when: "finding accounts that require payment"
        def result = standardizedAccountService.findAccountsThatRequirePayment()

        then: "should return accounts"
        1 * accountRepositoryMock.updateTotalsForAllAccounts()
        // New behavior: refresh validation dates prior to computing payment required list
        1 * accountRepositoryMock.updateValidationDateForAllAccounts()
        1 * accountRepositoryMock.findAccountsThatRequirePayment(_, _) >> accounts
        result.size() == 1
        0 * _
    }

    def "updateTotalsForAllAccounts should update totals and return true"() {
        when: "updating totals for all accounts"
        def result = standardizedAccountService.updateTotalsForAllAccounts()

        then: "should update totals and return true"
        1 * accountRepositoryMock.updateTotalsForAllAccounts()
        result == true
        0 * _
    }

    // ===== TDD Tests for Account Status Operations =====

    def "deactivateAccount should return deactivated account when exists"() {
        given: "existing active account"
        def account = AccountBuilder.builder().withAccountNameOwner("test_account").withActiveStatus(true).build()
        def deactivatedAccount = AccountBuilder.builder().withAccountNameOwner("test_account").withActiveStatus(false).build()

        when: "deactivating account"
        def result = standardizedAccountService.deactivateAccount("test_account")

        then: "should return deactivated account and deactivate all transactions"
        1 * accountRepositoryMock.findByAccountNameOwner("test_account") >> Optional.of(account)
        1 * transactionRepositoryMock.deactivateAllTransactionsByAccountNameOwner("test_account") >> 5
        1 * accountRepositoryMock.saveAndFlush(_ as Account) >> { Account acc ->
            assert acc.activeStatus == false
            assert acc.dateClosed != null
            return deactivatedAccount
        }
        result.activeStatus == false
        0 * _
    }

    def "deactivateAccount should throw EntityNotFoundException when account does not exist"() {
        when: "deactivating non-existent account"
        standardizedAccountService.deactivateAccount("non_existent")

        then: "should throw EntityNotFoundException"
        1 * accountRepositoryMock.findByAccountNameOwner("non_existent") >> Optional.empty()
        thrown(EntityNotFoundException)
        0 * _
    }

    def "activateAccount should return activated account when exists"() {
        given: "existing inactive account"
        def account = AccountBuilder.builder().withAccountNameOwner("test_account").withActiveStatus(false).build()
        def activatedAccount = AccountBuilder.builder().withAccountNameOwner("test_account").withActiveStatus(true).build()

        when: "activating account"
        def result = standardizedAccountService.activateAccount("test_account")

        then: "should return activated account"
        1 * accountRepositoryMock.findByAccountNameOwner("test_account") >> Optional.of(account)
        1 * accountRepositoryMock.saveAndFlush(_ as Account) >> { Account acc ->
            assert acc.activeStatus == true
            return activatedAccount
        }
        result.activeStatus == true
        0 * _
    }

    def "activateAccount should throw EntityNotFoundException when account does not exist"() {
        when: "activating non-existent account"
        standardizedAccountService.activateAccount("non_existent")

        then: "should throw EntityNotFoundException"
        1 * accountRepositoryMock.findByAccountNameOwner("non_existent") >> Optional.empty()
        thrown(EntityNotFoundException)
        0 * _
    }

    def "renameAccountNameOwner should return renamed account when exists"() {
        given: "existing account"
        def account = AccountBuilder.builder().withAccountNameOwner("old_name").build()
        def renamedAccount = AccountBuilder.builder().withAccountNameOwner("new_name").build()

        when: "renaming account"
        def result = standardizedAccountService.renameAccountNameOwner("old_name", "new_name")

        then: "should return renamed account"
        1 * accountRepositoryMock.findByAccountNameOwner("old_name") >> Optional.of(account)
        1 * transactionRepositoryMock.updateAccountNameOwnerForAllTransactions("old_name", "new_name") >> 5 // Returns number of updated transactions
        1 * accountRepositoryMock.saveAndFlush(_ as Account) >> { Account acc ->
            assert acc.accountNameOwner == "new_name"
            return renamedAccount
        }
        result.accountNameOwner == "new_name"
        0 * _
    }

    def "renameAccountNameOwner should throw EntityNotFoundException when account does not exist"() {
        when: "renaming non-existent account"
        standardizedAccountService.renameAccountNameOwner("non_existent", "new_name")

        then: "should throw EntityNotFoundException"
        1 * accountRepositoryMock.findByAccountNameOwner("non_existent") >> Optional.empty()
        thrown(EntityNotFoundException)
        0 * _
    }

    // ===== TDD Tests for Error Handling in Legacy Methods =====

    def "insertAccount should throw DataIntegrityViolationException for duplicate account"() {
        given: "account that already exists"
        def account = AccountBuilder.builder().build()
        def existingAccount = AccountBuilder.builder().withAccountId(1L).build()

        when: "calling legacy insertAccount with duplicate data"
        standardizedAccountService.insertAccount(account)

        then: "should throw DataIntegrityViolationException"
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.of(existingAccount)
        thrown(DataIntegrityViolationException)
    }

    def "insertAccount should throw ValidationException for invalid account"() {
        given: "invalid account"
        def account = AccountBuilder.builder().withAccountNameOwner("").build()
        ConstraintViolation<Account> violation = Mock(ConstraintViolation)
        violation.invalidValue >> ""
        violation.message >> "must not be blank"
        Set<ConstraintViolation<Account>> violations = [violation] as Set

        when: "calling legacy insertAccount with invalid data"
        standardizedAccountService.insertAccount(account)

        then: "should throw ValidationException"
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.empty()
        1 * validatorMock.validate(account) >> { throw new ConstraintViolationException("Validation failed", violations) }
        thrown(jakarta.validation.ValidationException)
    }

}
