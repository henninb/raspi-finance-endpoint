package finance.services

import finance.domain.Account
import finance.domain.TransactionState
import finance.domain.ValidationAmount
import finance.helpers.ValidationAmountBuilder

import java.sql.Timestamp

class ValidationAmountServiceAdditionalSpec extends BaseServiceSpec {

    void setup() {
        validationAmountService.validator = validatorMock
        validationAmountService.meterService = meterService
    }

    void "standard insertValidationAmount saves entity and updates account"() {
        given:
        def va = ValidationAmountBuilder.builder().withAccountId(10L).withAmount(new BigDecimal('12.34')).build()
        def saved = new ValidationAmount(validationId: 99L, accountId: 10L, amount: va.amount, activeStatus: true, transactionState: va.transactionState,
                validationDate: new Timestamp(System.currentTimeMillis()))
        def acct = new Account(accountId: 10L)

        when:
        def result = validationAmountService.insertValidationAmount(va)

        then:
        1 * validatorMock.validate(va) >> ([] as Set)
        1 * validationAmountRepositoryMock.saveAndFlush(va) >> saved
        1 * accountRepositoryMock.findByAccountId(10L) >> Optional.of(acct)
        1 * accountRepositoryMock.saveAndFlush(_ as Account) >> { args -> args[0] }
        result.validationId == 99L
        result.accountId == 10L
    }

    void "updateValidationAmount saves and updates account"() {
        given:
        def va = new ValidationAmount(validationId: 5L, accountId: 11L, amount: new BigDecimal('22.22'), activeStatus: true,
                transactionState: TransactionState.Cleared, validationDate: new Timestamp(System.currentTimeMillis()))
        def saved = new ValidationAmount(validationId: 5L, accountId: 11L, amount: va.amount, activeStatus: true,
                transactionState: va.transactionState, validationDate: va.validationDate)
        def acct = new Account(accountId: 11L)

        when:
        def result = validationAmountService.updateValidationAmount(va)

        then:
        1 * validatorMock.validate(va) >> ([] as Set)
        1 * validationAmountRepositoryMock.saveAndFlush(va) >> saved
        1 * accountRepositoryMock.findByAccountId(11L) >> Optional.of(acct)
        1 * accountRepositoryMock.saveAndFlush(_ as Account) >> { args -> args[0] }
        result.validationId == 5L
    }

    void "insertValidationAmount by owner prefers payload accountId when provided"() {
        given:
        def va = ValidationAmountBuilder.builder().withAccountId(7L).withAmount(new BigDecimal('1.00')).build()
        def saved = new ValidationAmount(validationId: 101L, accountId: 7L, amount: va.amount, activeStatus: true,
                transactionState: va.transactionState, validationDate: va.validationDate)
        def acct = new Account(accountId: 7L)

        when:
        def result = validationAmountService.insertValidationAmount('ignored_owner', va)

        then:
        0 * accountRepositoryMock.findByAccountNameOwner(*_)
        1 * validatorMock.validate(va) >> ([] as Set)
        1 * validationAmountRepositoryMock.saveAndFlush(va) >> saved
        1 * accountRepositoryMock.findByAccountId(7L) >> Optional.of(acct)
        1 * accountRepositoryMock.saveAndFlush(_ as Account) >> { args -> args[0] }
        result.validationId == 101L
        result.accountId == 7L
    }

    void "insertValidationAmount by owner with payload accountId and missing account still succeeds"() {
        given:
        def va = ValidationAmountBuilder.builder().withAccountId(15L).withAmount(new BigDecimal('2.00')).build()
        def saved = new ValidationAmount(validationId: 202L, accountId: 15L, amount: va.amount, activeStatus: true,
                transactionState: va.transactionState, validationDate: va.validationDate)

        when:
        def result = validationAmountService.insertValidationAmount('ignored', va)

        then:
        1 * validatorMock.validate(va) >> ([] as Set)
        1 * validationAmountRepositoryMock.saveAndFlush(va) >> saved
        1 * accountRepositoryMock.findByAccountId(15L) >> Optional.empty()
        0 * accountRepositoryMock.saveAndFlush(*_)
        result.validationId == 202L
        result.accountId == 15L
    }
}

