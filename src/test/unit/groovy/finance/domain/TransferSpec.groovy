package finance.domain

import spock.lang.Specification

import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate

class TransferSpec extends Specification {

    def "Transfer - default constructor"() {
        when:
        def transfer = new Transfer()

        then:
        transfer.transferId == 0L
        transfer.sourceAccount == ""
        transfer.destinationAccount == ""
        transfer.transactionDate == LocalDate.ofEpochDay(0)
        transfer.amount == new BigDecimal("0.00")
        transfer.guidSource == ""
        transfer.guidDestination == ""
        transfer.activeStatus == true
        transfer.dateAdded instanceof Timestamp
        transfer.dateUpdated instanceof Timestamp
    }

    def "Transfer - parameterized constructor"() {
        given:
        def transferId = 1L
        def sourceAccount = "test_source"
        def destinationAccount = "test_dest"
        def transactionDate = LocalDate.of(2023, 1, 1)
        def amount = new BigDecimal("100.00")
        def guidSource = "source-guid"
        def guidDestination = "dest-guid"

        when:
        def transfer = new Transfer(transferId, "test_owner", sourceAccount, destinationAccount,
                                  transactionDate, amount, guidSource, guidDestination, true)

        then:
        transfer.transferId == transferId
        transfer.sourceAccount == sourceAccount
        transfer.destinationAccount == destinationAccount
        transfer.transactionDate == transactionDate
        transfer.amount == amount
        transfer.guidSource == guidSource
        transfer.guidDestination == guidDestination
        transfer.activeStatus == true
        transfer.dateAdded instanceof Timestamp
        transfer.dateUpdated instanceof Timestamp
    }

    def "Transfer - properties can be modified"() {
        given:
        def transfer = new Transfer()

        when:
        transfer.transferId = 5L
        transfer.sourceAccount = "new_source"
        transfer.destinationAccount = "new_dest"
        transfer.transactionDate = LocalDate.of(2023, 6, 15)
        transfer.amount = new BigDecimal("250.75")
        transfer.guidSource = "new-source-guid"
        transfer.guidDestination = "new-dest-guid"
        transfer.activeStatus = false

        then:
        transfer.transferId == 5L
        transfer.sourceAccount == "new_source"
        transfer.destinationAccount == "new_dest"
        transfer.transactionDate == LocalDate.of(2023, 6, 15)
        transfer.amount == new BigDecimal("250.75")
        transfer.guidSource == "new-source-guid"
        transfer.guidDestination == "new-dest-guid"
        transfer.activeStatus == false
    }

    def "Transfer - timestamps are automatically set"() {
        given:
        def beforeCreation = System.currentTimeMillis()

        when:
        def transfer = new Transfer()
        def afterCreation = System.currentTimeMillis()

        then:
        transfer.dateAdded.time >= beforeCreation
        transfer.dateAdded.time <= afterCreation
        transfer.dateUpdated.time >= beforeCreation
        transfer.dateUpdated.time <= afterCreation
    }
}
