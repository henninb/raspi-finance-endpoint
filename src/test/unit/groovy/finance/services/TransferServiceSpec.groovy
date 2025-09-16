package finance.services

import finance.domain.*
import finance.repositories.TransferRepository
import spock.lang.Specification

import jakarta.validation.ConstraintViolation
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.util.*

class TransferServiceSpec extends BaseServiceSpec {

    TransferRepository transferRepositoryMock = GroovyMock(TransferRepository)
    TransactionService transactionServiceMock = GroovyMock(TransactionService)
    AccountService accountServiceMock = GroovyMock(AccountService)

    TransferService transferService

    def setup() {
        transferService = new TransferService(transferRepositoryMock, transactionServiceMock, accountServiceMock)
        transferService.meterService = meterService
        transferService.validator = validatorMock
    }

    def "findAllTransfers - success"() {
        given:
        def transfer1 = new Transfer(transferId: 1L, sourceAccount: "test_source", destinationAccount: "test_dest",
                                   amount: new BigDecimal("100.00"), transactionDate: Date.valueOf("2023-01-01"))
        def transfer2 = new Transfer(transferId: 2L, sourceAccount: "test_source2", destinationAccount: "test_dest2",
                                   amount: new BigDecimal("200.00"), transactionDate: Date.valueOf("2023-01-02"))
        def transfers = [transfer1, transfer2]

        when:
        def result = transferService.findAllTransfers()

        then:
        1 * transferRepositoryMock.findAll() >> transfers
        result.size() == 2
        result[0].transferId == 2L // sorted by date descending
        result[1].transferId == 1L
    }

    def "insertTransfer - success"() {
        given:
        def transfer = new Transfer(sourceAccount: "test_source", destinationAccount: "test_dest",
                                  amount: new BigDecimal("100.00"), transactionDate: Date.valueOf("2023-01-01"))
        def sourceAccount = new Account(accountNameOwner: "test_source")
        def destAccount = new Account(accountNameOwner: "test_dest")
        def savedTransfer = new Transfer(transferId: 1L, sourceAccount: "test_source", destinationAccount: "test_dest",
                                       amount: new BigDecimal("100.00"), transactionDate: Date.valueOf("2023-01-01"))

        when:
        def result = transferService.insertTransfer(transfer)

        then:
        1 * validatorMock.validate(transfer) >> new HashSet<ConstraintViolation<Transfer>>()
        1 * accountServiceMock.account("test_source") >> Optional.of(sourceAccount)
        1 * accountServiceMock.account("test_dest") >> Optional.of(destAccount)
        2 * transactionServiceMock.insertTransaction(_ as Transaction)
        1 * transferRepositoryMock.saveAndFlush(transfer) >> savedTransfer
        result.transferId == 1L
        transfer.guidSource != null
        transfer.guidDestination != null
        transfer.dateAdded != null
        transfer.dateUpdated != null
    }

    def "insertTransfer - source account not found"() {
        given:
        def transfer = new Transfer(sourceAccount: "nonexistent", destinationAccount: "test_dest",
                                  amount: new BigDecimal("100.00"), transactionDate: Date.valueOf("2023-01-01"))

        when:
        transferService.insertTransfer(transfer)

        then:
        1 * validatorMock.validate(transfer) >> new HashSet<ConstraintViolation<Transfer>>()
        1 * accountServiceMock.account("nonexistent") >> Optional.empty()
        thrown(RuntimeException)
    }

    def "insertTransfer - destination account not found"() {
        given:
        def transfer = new Transfer(sourceAccount: "test_source", destinationAccount: "nonexistent",
                                  amount: new BigDecimal("100.00"), transactionDate: Date.valueOf("2023-01-01"))
        def sourceAccount = new Account(accountNameOwner: "test_source")

        when:
        transferService.insertTransfer(transfer)

        then:
        1 * validatorMock.validate(transfer) >> new HashSet<ConstraintViolation<Transfer>>()
        1 * accountServiceMock.account("test_source") >> Optional.of(sourceAccount)
        1 * accountServiceMock.account("nonexistent") >> Optional.empty()
        thrown(RuntimeException)
    }

    def "deleteByTransferId - success"() {
        given:
        def transferId = 1L
        def transfer = new Transfer(transferId: transferId)

        when:
        def result = transferService.deleteByTransferId(transferId)

        then:
        1 * transferRepositoryMock.findByTransferId(transferId) >> Optional.of(transfer)
        1 * transferRepositoryMock.delete(transfer)
        result == true
    }

    def "deleteByTransferId - transfer not found"() {
        given:
        def transferId = 1L

        when:
        def result = transferService.deleteByTransferId(transferId)

        then:
        1 * transferRepositoryMock.findByTransferId(transferId) >> Optional.empty()
        0 * transferRepositoryMock.delete(*_)
        result == false
    }

    def "findByTransferId - success"() {
        given:
        def transferId = 1L
        def transfer = new Transfer(transferId: transferId)

        when:
        def result = transferService.findByTransferId(transferId)

        then:
        1 * transferRepositoryMock.findByTransferId(transferId) >> Optional.of(transfer)
        result.isPresent()
        result.get().transferId == transferId
    }

    def "findByTransferId - not found"() {
        given:
        def transferId = 1L

        when:
        def result = transferService.findByTransferId(transferId)

        then:
        1 * transferRepositoryMock.findByTransferId(transferId) >> Optional.empty()
        !result.isPresent()
    }
}
