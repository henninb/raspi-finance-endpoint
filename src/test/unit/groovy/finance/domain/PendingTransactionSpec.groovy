package finance.domain

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class PendingTransactionSpec extends Specification {

    def "PendingTransaction - default constructor"() {
        when:
        def pendingTransaction = new PendingTransaction()

        then:
        pendingTransaction.pendingTransactionId == 0L
        pendingTransaction.accountNameOwner == ""
        pendingTransaction.transactionDate == new Date(0)
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
        def transactionDate = Date.valueOf("2023-01-01")
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

    def "PendingTransaction - toString returns valid JSON"() {
        given:
        def pendingTransaction = new PendingTransaction(1L, "test_account", 
                                                      Date.valueOf("2023-01-01"), 
                                                      "test description", 
                                                      new BigDecimal("100.00"), 
                                                      "approved", "test_owner", null)

        when:
        def json = pendingTransaction.toString()
        def mapper = new ObjectMapper()
        def parsedTransaction = mapper.readValue(json, Map.class)

        then:
        json != null
        parsedTransaction.pendingTransactionId == 1
        parsedTransaction.accountNameOwner == "test_account"
        parsedTransaction.description == "test description"
        parsedTransaction.amount == 100.00
        parsedTransaction.reviewStatus == "approved"
        parsedTransaction.owner == "test_owner"
    }

    def "PendingTransaction - jsonSetterPaymentDate parses date correctly"() {
        given:
        def pendingTransaction = new PendingTransaction()
        def dateString = "2023-12-25"

        when:
        pendingTransaction.jsonSetterPaymentDate(dateString)

        then:
        pendingTransaction.transactionDate == Date.valueOf("2023-12-25")
    }

    def "PendingTransaction - jsonSetterPaymentDate handles invalid date format"() {
        given:
        def pendingTransaction = new PendingTransaction()
        def invalidDateString = "invalid-date"

        when:
        pendingTransaction.jsonSetterPaymentDate(invalidDateString)

        then:
        thrown(Exception) // Should throw parsing exception
    }

    def "PendingTransaction - properties can be modified"() {
        given:
        def pendingTransaction = new PendingTransaction()

        when:
        pendingTransaction.pendingTransactionId = 5L
        pendingTransaction.accountNameOwner = "new_account"
        pendingTransaction.transactionDate = Date.valueOf("2023-06-15")
        pendingTransaction.description = "new description"
        pendingTransaction.amount = new BigDecimal("250.75")
        pendingTransaction.reviewStatus = "rejected"
        pendingTransaction.owner = "new_owner"

        then:
        pendingTransaction.pendingTransactionId == 5L
        pendingTransaction.accountNameOwner == "new_account"
        pendingTransaction.transactionDate == Date.valueOf("2023-06-15")
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