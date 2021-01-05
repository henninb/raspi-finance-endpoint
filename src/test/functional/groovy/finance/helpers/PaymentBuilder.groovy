package finance.helpers

import finance.domain.Payment

import java.sql.Date

class PaymentBuilder {

    String accountNameOwner = 'foo_brian'
    BigDecimal amount = 0.00G
    Date transactionDate = Date.valueOf('2020-12-11')
    String guidSource = UUID.randomUUID()
    String guidDestination = UUID.randomUUID()
    Boolean activeStatus = true

    static PaymentBuilder builder() {
        return new PaymentBuilder()
    }

    Payment build() {
        Payment payment = new Payment()
        payment.accountNameOwner = accountNameOwner
        payment.amount = amount
        payment.transactionDate = transactionDate
        payment.guidSource = guidSource
        payment.guidDestination = guidDestination
        payment.activeStatus = activeStatus
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
