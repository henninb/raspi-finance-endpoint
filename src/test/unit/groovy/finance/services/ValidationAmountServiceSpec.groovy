package finance.services

import finance.domain.*
import finance.repositories.ValidationAmountRepository
import finance.repositories.AccountRepository
import spock.lang.Specification

import jakarta.validation.ConstraintViolation
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.util.*

class ValidationAmountServiceSpec extends BaseServiceSpec {

    ValidationAmountRepository validationAmountRepositoryMock = GroovyMock(ValidationAmountRepository)
    AccountRepository accountRepositoryMock = GroovyMock(AccountRepository)

    ValidationAmountService validationAmountService

    def setup() {
        validationAmountService = new ValidationAmountService(validationAmountRepositoryMock, accountRepositoryMock)
        validationAmountService.meterService = meterService
        validationAmountService.validator = validatorMock
    }

    def "insertValidationAmount - success with existing account"() {
        given:
        def accountNameOwner = "test_account"
        def validationAmount = new ValidationAmount(
            0L, 0L, 
            Timestamp.valueOf("2023-01-01 00:00:00"),
            true,
            TransactionState.Outstanding,
            new BigDecimal("1000.00")
        )
        def account = new Account(accountId: 1L, accountNameOwner: accountNameOwner)
        def savedValidationAmount = new ValidationAmount(
            1L, 1L, 
            Timestamp.valueOf("2023-01-01 00:00:00"),
            true,
            TransactionState.Outstanding,
            new BigDecimal("1000.00")
        )

        when:
        def result = validationAmountService.insertValidationAmount(accountNameOwner, validationAmount)

        then:
        1 * accountRepositoryMock.findByAccountNameOwner(accountNameOwner) >> Optional.of(account)
        1 * validatorMock.validate(validationAmount) >> new HashSet<ConstraintViolation<ValidationAmount>>()
        1 * validationAmountRepositoryMock.saveAndFlush(validationAmount) >> savedValidationAmount
        1 * accountRepositoryMock.saveAndFlush(account)
        
        result.validationId == 1L
        validationAmount.accountId == 1L
        validationAmount.dateAdded != null
        validationAmount.dateUpdated != null
        account.validationDate != null
        account.dateUpdated != null
    }

    def "insertValidationAmount - success with non-existing account"() {
        given:
        def accountNameOwner = "nonexistent_account"
        def validationAmount = new ValidationAmount(
            0L, 0L, 
            Timestamp.valueOf("2023-01-01 00:00:00"),
            true,
            TransactionState.Outstanding,
            new BigDecimal("1000.00")
        )
        def savedValidationAmount = new ValidationAmount(
            1L, 0L, 
            Timestamp.valueOf("2023-01-01 00:00:00"),
            true,
            TransactionState.Outstanding,
            new BigDecimal("1000.00")
        )

        when:
        def result = validationAmountService.insertValidationAmount(accountNameOwner, validationAmount)

        then:
        1 * accountRepositoryMock.findByAccountNameOwner(accountNameOwner) >> Optional.empty()
        1 * validatorMock.validate(validationAmount) >> new HashSet<ConstraintViolation<ValidationAmount>>()
        1 * validationAmountRepositoryMock.saveAndFlush(validationAmount) >> savedValidationAmount
        0 * accountRepositoryMock.saveAndFlush(*_)
        
        result.validationId == 1L
        validationAmount.accountId == 0L
        validationAmount.dateAdded != null
        validationAmount.dateUpdated != null
    }

    def "findValidationAmountByAccountNameOwner - success with existing validation"() {
        given:
        def accountNameOwner = "test_account"
        def transactionState = TransactionState.Outstanding
        def account = new Account(accountId: 1L, accountNameOwner: accountNameOwner)
        def validationAmount1 = new ValidationAmount(
            1L, 1L, 
            Timestamp.valueOf("2023-01-01 00:00:00"),
            true,
            TransactionState.Outstanding,
            new BigDecimal("1000.00")
        )
        def validationAmount2 = new ValidationAmount(
            2L, 1L, 
            Timestamp.valueOf("2023-01-02 00:00:00"),
            true,
            TransactionState.Outstanding,
            new BigDecimal("1100.00")
        )
        def validationAmounts = [validationAmount1, validationAmount2]

        when:
        def result = validationAmountService.findValidationAmountByAccountNameOwner(accountNameOwner, transactionState)

        then:
        1 * accountRepositoryMock.findByAccountNameOwner(accountNameOwner) >> Optional.of(account)
        1 * validationAmountRepositoryMock.findByTransactionStateAndAccountId(transactionState, 1L) >> validationAmounts
        
        result.validationId == 2L // should return the latest one
        result.amount == new BigDecimal("1100.00")
    }

    def "findValidationAmountByAccountNameOwner - no validation amounts found"() {
        given:
        def accountNameOwner = "test_account"
        def transactionState = TransactionState.Outstanding
        def account = new Account(accountId: 1L, accountNameOwner: accountNameOwner)

        when:
        def result = validationAmountService.findValidationAmountByAccountNameOwner(accountNameOwner, transactionState)

        then:
        1 * accountRepositoryMock.findByAccountNameOwner(accountNameOwner) >> Optional.of(account)
        1 * validationAmountRepositoryMock.findByTransactionStateAndAccountId(transactionState, 1L) >> []
        
        result != null
        result.validationId == 0L // should return empty ValidationAmount
    }

    def "findValidationAmountByAccountNameOwner - account not found"() {
        given:
        def accountNameOwner = "nonexistent_account"
        def transactionState = TransactionState.Outstanding

        when:
        def result = validationAmountService.findValidationAmountByAccountNameOwner(accountNameOwner, transactionState)

        then:
        1 * accountRepositoryMock.findByAccountNameOwner(accountNameOwner) >> Optional.empty()
        0 * validationAmountRepositoryMock.findByTransactionStateAndAccountId(*_)
        
        result != null
        result.validationId == 0L // should return empty ValidationAmount
    }
}