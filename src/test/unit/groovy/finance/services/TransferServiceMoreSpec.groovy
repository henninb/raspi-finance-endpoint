package finance.services

import finance.domain.*
import finance.repositories.TransferRepository

class TransferServiceMoreSpec extends BaseServiceSpec {

    TransferRepository transferRepositoryMock = GroovyMock(TransferRepository)
    TransactionService transactionServiceMock = GroovyMock(TransactionService)
    AccountService accountServiceMock = GroovyMock(AccountService)

    TransferService transferService

    void setup() {
        transferService = new TransferService(transferRepositoryMock, transactionServiceMock, accountServiceMock)
        transferService.meterService = meterService
        transferService.validator = validatorMock
    }

    void "findAllTransfers returns empty when repository empty"() {
        when:
        def list = transferService.findAllTransfers()

        then:
        1 * transferRepositoryMock.findAll() >> []
        list.isEmpty()
    }

    void "insertTransfer builds correct source and destination transactions"() {
        given:
        def transfer = new Transfer(sourceAccount: 'src', destinationAccount: 'dst', amount: new java.math.BigDecimal('12.34'), transactionDate: java.sql.Date.valueOf('2024-01-01'))
        def srcAcct = new Account(accountNameOwner: 'src')
        def dstAcct = new Account(accountNameOwner: 'dst')

        when:
        transferService.insertTransfer(transfer)

        then:
        1 * validatorMock.validate(transfer) >> ([] as Set)
        1 * accountServiceMock.account('src') >> Optional.of(srcAcct)
        1 * accountServiceMock.account('dst') >> Optional.of(dstAcct)
        1 * transactionServiceMock.insertTransaction({ Transaction t ->
            t.description == 'transfer withdrawal' &&
            t.category == 'transfer' &&
            t.notes == 'Transfer to dst' &&
            t.amount == new java.math.BigDecimal('-12.34') &&
            t.transactionState == TransactionState.Outstanding &&
            t.reoccurringType == ReoccurringType.Onetime &&
            t.accountType == AccountType.Debit &&
            t.accountNameOwner == 'src'
        })
        1 * transactionServiceMock.insertTransaction({ Transaction t ->
            t.description == 'transfer deposit' &&
            t.category == 'transfer' &&
            t.notes == 'Transfer from src' &&
            t.amount == new java.math.BigDecimal('12.34') &&
            t.transactionState == TransactionState.Outstanding &&
            t.reoccurringType == ReoccurringType.Onetime &&
            t.accountType == AccountType.Debit &&
            t.accountNameOwner == 'dst'
        })
        1 * transferRepositoryMock.saveAndFlush(_ as Transfer) >> { it[0] }
    }

    void "updateTransfer success updates timestamp and saves"() {
        given:
        def existing = new Transfer(transferId: 10L, sourceAccount: 'a', destinationAccount: 'b', amount: new java.math.BigDecimal('1.00'), transactionDate: new java.sql.Date(System.currentTimeMillis()))
        def patch = new Transfer(transferId: 10L, sourceAccount: 'a', destinationAccount: 'b', amount: new java.math.BigDecimal('2.00'), transactionDate: existing.transactionDate)

        when:
        def result = transferService.updateTransfer(patch)

        then:
        1 * validatorMock.validate(patch) >> ([] as Set)
        1 * transferRepositoryMock.findByTransferId(10L) >> Optional.of(existing)
        1 * transferRepositoryMock.saveAndFlush(patch) >> { it[0] }
        result.transferId == 10L
        result.dateUpdated != null
    }

    void "updateTransfer throws when not found"() {
        given:
        def patch = new Transfer(transferId: 99L)

        when:
        transferService.updateTransfer(patch)

        then:
        1 * validatorMock.validate(patch) >> ([] as Set)
        1 * transferRepositoryMock.findByTransferId(99L) >> Optional.empty()
        thrown(RuntimeException)
    }

    void "updateTransfer throws on validation failure"() {
        given:
        def patch = new Transfer(transferId: 7L)

        when:
        transferService.updateTransfer(patch)

        then:
        1 * validatorMock.validate(patch) >> ([Mock(jakarta.validation.ConstraintViolation)] as Set)
        thrown(jakarta.validation.ValidationException)
    }
}

