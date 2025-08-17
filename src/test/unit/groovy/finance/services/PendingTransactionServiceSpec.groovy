package finance.services

import finance.domain.*
import finance.repositories.PendingTransactionRepository
import spock.lang.Specification

import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.util.*

class PendingTransactionServiceSpec extends BaseServiceSpec {

    PendingTransactionRepository pendingTransactionRepositoryMock = GroovyMock(PendingTransactionRepository)

    PendingTransactionService pendingTransactionService

    def setup() {
        pendingTransactionService = new PendingTransactionService(pendingTransactionRepositoryMock)
        pendingTransactionService.meterService = meterService
    }

    def "insertPendingTransaction - success"() {
        given:
        def pendingTransaction = new PendingTransaction(
            0L, "test_account",
            Date.valueOf("2023-01-01"),
            "test description",
            new BigDecimal("100.00"),
            "pending", null, null
        )
        def savedPendingTransaction = new PendingTransaction(
            1L, "test_account",
            Date.valueOf("2023-01-01"),
            "test description",
            new BigDecimal("100.00"),
            "pending", null, null
        )

        when:
        def result = pendingTransactionService.insertPendingTransaction(pendingTransaction)

        then:
        1 * pendingTransactionRepositoryMock.saveAndFlush(pendingTransaction) >> savedPendingTransaction

        result.pendingTransactionId == 1L
        pendingTransaction.dateAdded != null
    }

    def "deletePendingTransaction - success"() {
        given:
        def pendingTransactionId = 1L
        def pendingTransaction = new PendingTransaction(
            pendingTransactionId, "test_account",
            Date.valueOf("2023-01-01"), "test desc",
            new BigDecimal("100.00"), "pending", null, null
        )

        when:
        def result = pendingTransactionService.deletePendingTransaction(pendingTransactionId)

        then:
        1 * pendingTransactionRepositoryMock.findByPendingTransactionIdOrderByTransactionDateDesc(pendingTransactionId) >> Optional.of(pendingTransaction)
        1 * pendingTransactionRepositoryMock.delete(pendingTransaction)

        result == true
    }

    def "getAllPendingTransactions - success"() {
        given:
        def pendingTransaction1 = new PendingTransaction(
            1L, "test_account1",
            Date.valueOf("2023-01-01"), "test desc 1",
            new BigDecimal("100.00"), "pending", null, null
        )
        def pendingTransaction2 = new PendingTransaction(
            2L, "test_account2",
            Date.valueOf("2023-01-02"), "test desc 2",
            new BigDecimal("200.00"), "pending", null, null
        )
        def pendingTransactions = [pendingTransaction1, pendingTransaction2]

        when:
        def result = pendingTransactionService.getAllPendingTransactions()

        then:
        1 * pendingTransactionRepositoryMock.findAll() >> pendingTransactions

        result.size() == 2
        result[0].pendingTransactionId == 1L
        result[1].pendingTransactionId == 2L
    }

    def "getAllPendingTransactions - empty list"() {
        given:
        def pendingTransactions = []

        when:
        def result = pendingTransactionService.getAllPendingTransactions()

        then:
        1 * pendingTransactionRepositoryMock.findAll() >> pendingTransactions

        result.size() == 0
    }

    def "deleteAllPendingTransactions - success"() {
        when:
        def result = pendingTransactionService.deleteAllPendingTransactions()

        then:
        1 * pendingTransactionRepositoryMock.deleteAll()

        result == true
    }
}