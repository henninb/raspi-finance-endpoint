package finance.helpers

import finance.domain.Payment

import java.sql.Date
import java.sql.Timestamp
import java.util.*

class PaymentBuilder {

    Long paymentId = 0L
    String sourceAccount = 'source_brian'
    String destinationAccount = 'dest_brian'
    BigDecimal amount = new BigDecimal("100.00")
    Date transactionDate = Date.valueOf('2020-12-11')
    String guidSource = UUID.randomUUID().toString()
    String guidDestination = UUID.randomUUID().toString()
    Boolean activeStatus = true
    Timestamp dateAdded = new Timestamp(Calendar.getInstance().time.time)
    Timestamp dateUpdated = new Timestamp(Calendar.getInstance().time.time)

    static PaymentBuilder builder() {
        return new PaymentBuilder()
    }

    Payment build() {
        Payment payment = new Payment().with {
            paymentId = this.paymentId
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
        return payment
    }


    PaymentBuilder withAmount(BigDecimal amount) {
        this.amount = amount
        return this
    }

    PaymentBuilder withTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate
        return this
    }

    PaymentBuilder withGuidSource(String guidSource) {
        this.guidSource = guidSource
        return this
    }

    PaymentBuilder withGuidDestination(String guidDestination) {
        this.guidDestination = guidDestination
        return this
    }

    PaymentBuilder withSourceAccount(String sourceAccount) {
        this.sourceAccount = sourceAccount
        return this
    }

    PaymentBuilder withDestinationAccount(String destinationAccount) {
        this.destinationAccount = destinationAccount
        return this
    }

    PaymentBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }

    PaymentBuilder withPaymentId(Long paymentId) {
        this.paymentId = paymentId
        return this
    }

    PaymentBuilder withDateAdded(Timestamp dateAdded) {
        this.dateAdded = dateAdded
        return this
    }

    PaymentBuilder withDateUpdated(Timestamp dateUpdated) {
        this.dateUpdated = dateUpdated
        return this
    }
}
