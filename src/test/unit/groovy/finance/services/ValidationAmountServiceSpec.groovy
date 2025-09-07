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

import finance.config.TestAsyncConfig
import org.springframework.test.context.ContextConfiguration


@ContextConfiguration(classes = [TestAsyncConfig])
class ValidationAmountServiceSpec extends BaseServiceSpec {

    def "insertValidationAmount - success with existing account"() {
        given:
        def accountNameOwner = "test_account"
        def validationAmount = new ValidationAmount(
                0L, 0L, null,
                Timestamp.valueOf("2023-01-01 00:00:00"),
                true,
                TransactionState.Outstanding,
                new BigDecimal("1000.00")
        )
        def account = new Account(accountId: 1L, accountNameOwner: accountNameOwner)
        def savedValidationAmount = new ValidationAmount(
                1L, 1L, null,
                Timestamp.valueOf("2023-01-01 00:00:00"),
                true,
                TransactionState.Outstanding,
                new BigDecimal("1000.00")
        )
        accountRepositoryMock.findByAccountNameOwner(accountNameOwner) >> Optional.of(account)
        accountRepositoryMock.findByAccountId(1L) >> Optional.of(account)
        validatorMock.validate(validationAmount) >> new HashSet<ConstraintViolation<ValidationAmount>>()
        validationAmountRepositoryMock.saveAndFlush(_) >> savedValidationAmount
        accountRepositoryMock.saveAndFlush(account) >> account

        when:
        def result = validationAmountService.insertValidationAmount(accountNameOwner, validationAmount)

        then:
        result.validationId == 1L
        result.accountId == 1L
    }

    def "insertValidationAmount - success with non-existing account"() {
        given:
        def accountNameOwner = "nonexistent_account"
        def validationAmount = new ValidationAmount(
                0L, 0L, null,
                Timestamp.valueOf("2023-01-01 00:00:00"),
                true,
                TransactionState.Outstanding,
                new BigDecimal("1000.00")
        )
        accountRepositoryMock.findByAccountNameOwner(accountNameOwner) >> Optional.empty()
        validatorMock.validate(validationAmount) >> new HashSet<ConstraintViolation<ValidationAmount>>()

        when:
        validationAmountService.insertValidationAmount(accountNameOwner, validationAmount)

        then:
        thrown(org.springframework.web.server.ResponseStatusException)
    }

    def "findValidationAmountByAccountNameOwner - success with existing validation"() {
        given:
        def accountNameOwner = "test_account"
        def transactionState = TransactionState.Outstanding
        def account = new Account(accountId: 1L, accountNameOwner: accountNameOwner)
        def validationAmount1 = new ValidationAmount(
            1L, 1L, null,
            Timestamp.valueOf("2023-01-01 00:00:00"),
            true,
            TransactionState.Outstanding,
            new BigDecimal("1000.00")
        )
        def validationAmount2 = new ValidationAmount(
            2L, 1L, null,
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