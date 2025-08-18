package finance.helpers

import finance.domain.Payment

import java.sql.Date

class PaymentBuilder {

    String sourceAccount = 'source_brian'
    String destinationAccount = 'dest_brian'
    BigDecimal amount = 0.00G
    Date transactionDate = Date.valueOf('2020-12-11')
    String guidSource = UUID.randomUUID()
    String guidDestination = UUID.randomUUID()
    Boolean activeStatus = true

    static PaymentBuilder builder() {
        return new PaymentBuilder()
    }

    //TODO: setting the guids below does not set them as they are set automatically
    Payment build() {
        Payment payment = new Payment().with {
            sourceAccount = this.sourceAccount
            destinationAccount = this.destinationAccount
            amount = this.amount
            transactionDate = this.transactionDate
            guidSource = this.guidSource
            guidDestination = this.guidDestination
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
}
