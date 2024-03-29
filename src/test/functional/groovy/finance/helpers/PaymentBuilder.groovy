package finance.helpers

import finance.domain.Payment

import java.sql.Date

class PaymentBuilder {

    String accountNameOwner = 'foo_brian'
    BigDecimal amount = 0.00G
    Date transactionDate = Date.valueOf('2022-12-10')
    String guidSource = UUID.randomUUID()
    String guidDestination = UUID.randomUUID()
    Boolean activeStatus = true

    static PaymentBuilder builder() {
        return new PaymentBuilder()
    }

    //TODO: setting the guids below does not set them as they are set automatically
    Payment build() {
        Payment payment = new Payment().with {
            accountNameOwner = this.accountNameOwner
            amount = this.amount
            transactionDate = this.transactionDate
            guidSource = this.guidSource
            guidDestination = this.guidDestination
            return it
        }
        return payment
    }

    PaymentBuilder withAccountNameOwner(String accountNameOwner) {
        this.accountNameOwner = accountNameOwner
        return this
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

    PaymentBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }
}
