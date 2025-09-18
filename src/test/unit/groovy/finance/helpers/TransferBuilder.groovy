package finance.helpers

import finance.domain.Transfer
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp

class TransferBuilder {

    Long transferId = 0L
    String sourceAccount = 'test_source'
    String destinationAccount = 'test_destination'
    BigDecimal amount = new BigDecimal("100.00")
    Date transactionDate = Date.valueOf('2024-01-01')
    String guidSource = 'source-guid-123'
    String guidDestination = 'dest-guid-456'
    Boolean activeStatus = true
    Timestamp dateAdded = new Timestamp(System.currentTimeMillis())
    Timestamp dateUpdated = new Timestamp(System.currentTimeMillis())

    static TransferBuilder builder() {
        return new TransferBuilder()
    }

    Transfer build() {
        Transfer transfer = new Transfer().with {
            transferId = this.transferId
            sourceAccount = this.sourceAccount
            destinationAccount = this.destinationAccount
            amount = this.amount
            transactionDate = this.transactionDate
            guidSource = this.guidSource
            guidDestination = this.guidDestination
            activeStatus = this.activeStatus
            dateAdded = this.dateAdded
            dateUpdated = this.dateUpdated
            return it
        }
        return transfer
    }

    TransferBuilder withTransferId(Long transferId) {
        this.transferId = transferId
        return this
    }

    TransferBuilder withSourceAccount(String sourceAccount) {
        this.sourceAccount = sourceAccount
        return this
    }

    TransferBuilder withDestinationAccount(String destinationAccount) {
        this.destinationAccount = destinationAccount
        return this
    }

    TransferBuilder withAmount(BigDecimal amount) {
        this.amount = amount
        return this
    }

    TransferBuilder withTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate
        return this
    }

    TransferBuilder withGuidSource(String guidSource) {
        this.guidSource = guidSource
        return this
    }

    TransferBuilder withGuidDestination(String guidDestination) {
        this.guidDestination = guidDestination
        return this
    }

    TransferBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }

    TransferBuilder withDateAdded(Timestamp dateAdded) {
        this.dateAdded = dateAdded
        return this
    }

    TransferBuilder withDateUpdated(Timestamp dateUpdated) {
        this.dateUpdated = dateUpdated
        return this
    }
}