package finance.helpers

import finance.domain.Transfer
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate

class TransferBuilder {

    Long transferId = 0L
    String owner = 'test_owner'
    String sourceAccount = 'test_source'
    String destinationAccount = 'test_destination'
    BigDecimal amount = new BigDecimal("100.00")
    LocalDate transactionDate = LocalDate.parse('2024-01-01')
    String guidSource = 'source-guid-123'
    String guidDestination = 'dest-guid-456'
    Boolean activeStatus = true
    Timestamp dateAdded = new Timestamp(System.currentTimeMillis())
    Timestamp dateUpdated = new Timestamp(System.currentTimeMillis())

    static TransferBuilder builder() {
        new TransferBuilder()
    }

    Transfer build() {
        Transfer transfer = new Transfer().with {
            transferId = this.transferId
            owner = this.owner
            sourceAccount = this.sourceAccount
            destinationAccount = this.destinationAccount
            amount = this.amount
            transactionDate = this.transactionDate
            guidSource = this.guidSource
            guidDestination = this.guidDestination
            activeStatus = this.activeStatus
            dateAdded = this.dateAdded
            dateUpdated = this.dateUpdated
            it
        }
        transfer
    }

    TransferBuilder withOwner(String owner) {
        this.owner = owner
        this
    }

    TransferBuilder withTransferId(Long transferId) {
        this.transferId = transferId
        this
    }

    TransferBuilder withSourceAccount(String sourceAccount) {
        this.sourceAccount = sourceAccount
        this
    }

    TransferBuilder withDestinationAccount(String destinationAccount) {
        this.destinationAccount = destinationAccount
        this
    }

    TransferBuilder withAmount(BigDecimal amount) {
        this.amount = amount
        this
    }

    TransferBuilder withTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate
        this
    }

    TransferBuilder withGuidSource(String guidSource) {
        this.guidSource = guidSource
        this
    }

    TransferBuilder withGuidDestination(String guidDestination) {
        this.guidDestination = guidDestination
        this
    }

    TransferBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        this
    }

    TransferBuilder withDateAdded(Timestamp dateAdded) {
        this.dateAdded = dateAdded
        this
    }

    TransferBuilder withDateUpdated(Timestamp dateUpdated) {
        this.dateUpdated = dateUpdated
        this
    }
}
