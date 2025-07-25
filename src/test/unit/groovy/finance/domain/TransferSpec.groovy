package finance.domain

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class TransferSpec extends Specification {

    def "Transfer - default constructor"() {
        when:
        def transfer = new Transfer()

        then:
        transfer.transferId == 0L
        transfer.sourceAccount == ""
        transfer.destinationAccount == ""
        transfer.transactionDate == new Date(0)
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
        def transactionDate = Date.valueOf("2023-01-01")
        def amount = new BigDecimal("100.00")
        def guidSource = "source-guid"
        def guidDestination = "dest-guid"

        when:
        def transfer = new Transfer(transferId, sourceAccount, destinationAccount, 
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

    def "Transfer - toString returns valid JSON"() {
        given:
        def transfer = new Transfer(1L, "test_source", "test_dest", 
                                  Date.valueOf("2023-01-01"), new BigDecimal("100.00"), 
                                  "source-guid", "dest-guid", true)

        when:
        def json = transfer.toString()
        def mapper = new ObjectMapper()
        def parsedTransfer = mapper.readValue(json, Map.class)

        then:
        json != null
        parsedTransfer.transferId == 1
        parsedTransfer.sourceAccount == "test_source"
        parsedTransfer.destinationAccount == "test_dest"
        parsedTransfer.transactionDate == "2023-01-01"
        parsedTransfer.amount == 100.00
        parsedTransfer.guidSource == "source-guid"
        parsedTransfer.guidDestination == "dest-guid"
        parsedTransfer.activeStatus == true
    }

    def "Transfer - jsonGetterTransferDate formats date correctly"() {
        given:
        def transfer = new Transfer()
        transfer.transactionDate = Date.valueOf("2023-12-25")

        when:
        def formattedDate = transfer.jsonGetterTransferDate()

        then:
        formattedDate == "2023-12-25"
    }

    def "Transfer - jsonSetterTransfertDate parses date correctly"() {
        given:
        def transfer = new Transfer()
        def dateString = "2023-12-25"

        when:
        transfer.jsonSetterTransfertDate(dateString)

        then:
        transfer.transactionDate == Date.valueOf("2023-12-25")
    }

    def "Transfer - jsonSetterTransfertDate handles invalid date format"() {
        given:
        def transfer = new Transfer()
        def invalidDateString = "invalid-date"

        when:
        transfer.jsonSetterTransfertDate(invalidDateString)

        then:
        thrown(Exception) // Should throw parsing exception
    }

    def "Transfer - properties can be modified"() {
        given:
        def transfer = new Transfer()

        when:
        transfer.transferId = 5L
        transfer.sourceAccount = "new_source"
        transfer.destinationAccount = "new_dest"
        transfer.transactionDate = Date.valueOf("2023-06-15")
        transfer.amount = new BigDecimal("250.75")
        transfer.guidSource = "new-source-guid"
        transfer.guidDestination = "new-dest-guid"
        transfer.activeStatus = false

        then:
        transfer.transferId == 5L
        transfer.sourceAccount == "new_source"
        transfer.destinationAccount == "new_dest"
        transfer.transactionDate == Date.valueOf("2023-06-15")
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