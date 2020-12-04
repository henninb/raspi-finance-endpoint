package finance.helpers

import finance.domain.Payment

import java.sql.Date

class PaymentBuilder {

    String accountNameOwner = 'foo_brian'
    BigDecimal amount = 0.00G
    Date transactionDate = new Date(1605300155000)
    String guidSource = UUID.randomUUID().toString()
    String guidDestination = UUID.randomUUID().toString()

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
        return payment
    }

    PaymentBuilder accountNameOwner(String accountNameOwner) {
        this.accountNameOwner = accountNameOwner
        return this
    }

    PaymentBuilder amount(BigDecimal amount) {
        this.amount = amount
        return this
    }

    PaymentBuilder transactionDate(Date transactionDate) {
        this.transactionDate = transactionDate
        return this
    }

    PaymentBuilder guidSource(String guidSource) {
        this.guidSource = guidSource
        return this
    }

    PaymentBuilder guidDestination(String guidDestination) {
        this.guidDestination = guidDestination
        return this
    }
}
