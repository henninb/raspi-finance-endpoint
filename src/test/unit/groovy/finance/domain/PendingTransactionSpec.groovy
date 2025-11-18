package finance.domain

import spock.lang.Specification

import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate

class PendingTransactionSpec extends Specification {

    def "PendingTransaction - default constructor"() {
        when:
        def pendingTransaction = new PendingTransaction()

        then:
        pendingTransaction.pendingTransactionId == 0L
        pendingTransaction.accountNameOwner == ""
        pendingTransaction.transactionDate == LocalDate.of(1970, 1, 1)
        pendingTransaction.description == ""
        pendingTransaction.amount == new BigDecimal("0.00")
        pendingTransaction.reviewStatus == "pending"
        pendingTransaction.owner == ""
        pendingTransaction.account == null
        pendingTransaction.dateAdded instanceof Timestamp
    }

    def "PendingTransaction - parameterized constructor"() {
        given:
        def pendingTransactionId = 1L
        def accountNameOwner = "test_account"
        def transactionDate = LocalDate.parse("2023-01-01")
        def description = "test description"
        def amount = new BigDecimal("100.00")
        def reviewStatus = "approved"
        def owner = "test_owner"

        when:
        def pendingTransaction = new PendingTransaction(pendingTransactionId, accountNameOwner,
                                                      transactionDate, description, amount,
                                                      reviewStatus, owner, null)

        then:
        pendingTransaction.pendingTransactionId == pendingTransactionId
        pendingTransaction.accountNameOwner == accountNameOwner
        pendingTransaction.transactionDate == transactionDate
        pendingTransaction.description == description
        pendingTransaction.amount == amount
        pendingTransaction.reviewStatus == reviewStatus
        pendingTransaction.owner == owner
        pendingTransaction.account == null
        pendingTransaction.dateAdded instanceof Timestamp
    }

    def "PendingTransaction - properties can be modified"() {
        given:
        def pendingTransaction = new PendingTransaction()

        when:
        pendingTransaction.pendingTransactionId = 5L
        pendingTransaction.accountNameOwner = "new_account"
        pendingTransaction.transactionDate = LocalDate.parse("2023-06-15")
        pendingTransaction.description = "new description"
        pendingTransaction.amount = new BigDecimal("250.75")
        pendingTransaction.reviewStatus = "rejected"
        pendingTransaction.owner = "new_owner"

        then:
        pendingTransaction.pendingTransactionId == 5L
        pendingTransaction.accountNameOwner == "new_account"
        pendingTransaction.transactionDate == LocalDate.parse("2023-06-15")
        pendingTransaction.description == "new description"
        pendingTransaction.amount == new BigDecimal("250.75")
        pendingTransaction.reviewStatus == "rejected"
        pendingTransaction.owner == "new_owner"
    }

    def "PendingTransaction - timestamps are automatically set"() {
        given:
        def beforeCreation = System.currentTimeMillis()

        when:
        def pendingTransaction = new PendingTransaction()
        def afterCreation = System.currentTimeMillis()

        then:
        pendingTransaction.dateAdded.time >= beforeCreation
        pendingTransaction.dateAdded.time <= afterCreation
    }

    def "PendingTransaction - account relationship can be set"() {
        given:
        def pendingTransaction = new PendingTransaction()
        def account = new Account()
        account.accountNameOwner = "test_account"

        when:
        pendingTransaction.account = account

        then:
        pendingTransaction.account != null
        pendingTransaction.account.accountNameOwner == "test_account"
    }
}
