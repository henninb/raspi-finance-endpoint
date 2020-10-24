package finance.helpers

import finance.domain.Payment

import java.sql.Date

class PaymentBuilder {

    Long paymentId = 1
    String accountNameOwner = "foo_brian"
    BigDecimal amount = new BigDecimal(0.0)
    Date transactionDate = new Date(1553645394)

    static PaymentBuilder builder() {
        return new PaymentBuilder()
    }

    Payment build() {
        Payment payment = new Payment()
        payment.paymentId = paymentId
        payment.accountNameOwner = accountNameOwner
        payment.amount = amount
        payment.transactionDate = transactionDate

        return payment
    }

    PaymentBuilder accountNameOwner(accountNameOwner) {
        this.accountNameOwner = accountNameOwner
        return this
    }

    PaymentBuilder amount(amount) {
        this.amount = amount
        return this
    }

    PaymentBuilder transactionDate(transactionDate) {
        this.transactionDate = transactionDate
        return this
    }

    PaymentBuilder paymentId(paymentId) {
        this.paymentId = paymentId
        return this
    }
}
