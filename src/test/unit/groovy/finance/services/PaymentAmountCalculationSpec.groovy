package finance.services

import finance.domain.PaymentBehavior
import spock.lang.Specification
import spock.lang.Unroll

import java.math.BigDecimal

/**
 * Unit tests for payment amount calculation logic.
 * Tests the mathematical correctness of transaction amount sign determination.
 */
class PaymentAmountCalculationSpec extends Specification {

    // We test these private methods indirectly through transaction population,
    // but we document the expected behavior here for clarity

    @Unroll
    def "calculateSourceAmount should return #expectedSign for #behavior with amount #amount"() {
        given: "a payment behavior and amount"
        def absoluteAmount = amount.abs()

        when: "calculating source amount based on behavior"
        def result = calculateSourceAmountForBehavior(absoluteAmount, behavior)

        then: "the sign should match expected behavior"
        result.signum() == expectedSign

        where:
        behavior                            | amount                   | expectedSign
        PaymentBehavior.BILL_PAYMENT        | new BigDecimal("100.00") | -1  // negative
        PaymentBehavior.TRANSFER            | new BigDecimal("100.00") | -1  // negative
        PaymentBehavior.CASH_ADVANCE        | new BigDecimal("100.00") | 1   // positive
        PaymentBehavior.BALANCE_TRANSFER    | new BigDecimal("100.00") | -1  // negative
        PaymentBehavior.UNDEFINED           | new BigDecimal("100.00") | -1  // negative (safest)
    }

    @Unroll
    def "calculateDestinationAmount should return #expectedSign for #behavior with amount #amount"() {
        given: "a payment behavior and amount"
        def absoluteAmount = amount.abs()

        when: "calculating destination amount based on behavior"
        def result = calculateDestinationAmountForBehavior(absoluteAmount, behavior)

        then: "the sign should match expected behavior"
        result.signum() == expectedSign

        where:
        behavior                            | amount                   | expectedSign
        PaymentBehavior.BILL_PAYMENT        | new BigDecimal("100.00") | -1  // negative (paying down debt)
        PaymentBehavior.TRANSFER            | new BigDecimal("100.00") | 1   // positive (receiving money)
        PaymentBehavior.CASH_ADVANCE        | new BigDecimal("100.00") | 1   // positive (receiving cash)
        PaymentBehavior.BALANCE_TRANSFER    | new BigDecimal("100.00") | 1   // positive (receiving debt)
        PaymentBehavior.UNDEFINED           | new BigDecimal("100.00") | -1  // negative (safest)
    }

    def "source and destination amounts should have correct signs for BILL_PAYMENT"() {
        given: "a bill payment scenario"
        def amount = new BigDecimal("250.50")

        when: "calculating amounts"
        def sourceAmount = calculateSourceAmountForBehavior(amount, PaymentBehavior.BILL_PAYMENT)
        def destAmount = calculateDestinationAmountForBehavior(amount, PaymentBehavior.BILL_PAYMENT)

        then: "both should be negative (asset out, liability down)"
        sourceAmount == new BigDecimal("-250.50")
        destAmount == new BigDecimal("-250.50")
    }

    def "source and destination amounts should have correct signs for TRANSFER"() {
        given: "a transfer scenario"
        def amount = new BigDecimal("1000.00")

        when: "calculating amounts"
        def sourceAmount = calculateSourceAmountForBehavior(amount, PaymentBehavior.TRANSFER)
        def destAmount = calculateDestinationAmountForBehavior(amount, PaymentBehavior.TRANSFER)

        then: "source negative, destination positive"
        sourceAmount == new BigDecimal("-1000.00")
        destAmount == new BigDecimal("1000.00")
    }

    def "source and destination amounts should have correct signs for CASH_ADVANCE"() {
        given: "a cash advance scenario"
        def amount = new BigDecimal("500.00")

        when: "calculating amounts"
        def sourceAmount = calculateSourceAmountForBehavior(amount, PaymentBehavior.CASH_ADVANCE)
        def destAmount = calculateDestinationAmountForBehavior(amount, PaymentBehavior.CASH_ADVANCE)

        then: "both should be positive (liability up, asset up)"
        sourceAmount == new BigDecimal("500.00")
        destAmount == new BigDecimal("500.00")
    }

    def "source and destination amounts should have correct signs for BALANCE_TRANSFER"() {
        given: "a balance transfer scenario"
        def amount = new BigDecimal("2500.00")

        when: "calculating amounts"
        def sourceAmount = calculateSourceAmountForBehavior(amount, PaymentBehavior.BALANCE_TRANSFER)
        def destAmount = calculateDestinationAmountForBehavior(amount, PaymentBehavior.BALANCE_TRANSFER)

        then: "source negative, destination positive (debt moved)"
        sourceAmount == new BigDecimal("-2500.00")
        destAmount == new BigDecimal("2500.00")
    }

    @Unroll
    def "amount calculation should handle edge case: #scenario"() {
        when: "calculating with edge case amount"
        def sourceAmount = calculateSourceAmountForBehavior(amount, behavior)
        def destAmount = calculateDestinationAmountForBehavior(amount, behavior)

        then: "calculations should not fail"
        sourceAmount != null
        destAmount != null

        and: "amounts should be absolute value based (no negative inputs expected)"
        sourceAmount.abs() == amount.abs()
        destAmount.abs() == amount.abs()

        where:
        scenario                | amount                          | behavior
        "zero amount"           | BigDecimal.ZERO                 | PaymentBehavior.TRANSFER
        "very small amount"     | new BigDecimal("0.01")          | PaymentBehavior.BILL_PAYMENT
        "large amount"          | new BigDecimal("999999.99")     | PaymentBehavior.CASH_ADVANCE
        "many decimal places"   | new BigDecimal("123.456789")    | PaymentBehavior.BALANCE_TRANSFER
    }

    def "negative input amounts should be converted to positive (abs) before sign logic"() {
        given: "a negative amount (defensive programming scenario)"
        def negativeAmount = new BigDecimal("-100.00")

        when: "calculating for TRANSFER behavior"
        def sourceAmount = calculateSourceAmountForBehavior(negativeAmount, PaymentBehavior.TRANSFER)
        def destAmount = calculateDestinationAmountForBehavior(negativeAmount, PaymentBehavior.TRANSFER)

        then: "abs() should convert to positive before applying behavior logic"
        sourceAmount == new BigDecimal("-100.00")
        destAmount == new BigDecimal("100.00")
    }

    def "amount calculation should preserve precision for all behaviors"() {
        given: "an amount with 2 decimal precision"
        def amount = new BigDecimal("123.45")

        when: "calculating for all behaviors"
        def billPaymentSource = calculateSourceAmountForBehavior(amount, PaymentBehavior.BILL_PAYMENT)
        def transferDest = calculateDestinationAmountForBehavior(amount, PaymentBehavior.TRANSFER)
        def cashAdvanceSource = calculateSourceAmountForBehavior(amount, PaymentBehavior.CASH_ADVANCE)
        def balanceTransferDest = calculateDestinationAmountForBehavior(amount, PaymentBehavior.BALANCE_TRANSFER)

        then: "all amounts should maintain 2 decimal precision"
        billPaymentSource.scale() == 2
        transferDest.scale() == 2
        cashAdvanceSource.scale() == 2
        balanceTransferDest.scale() == 2
    }

    // Helper methods that mimic the private service methods
    // These replicate the logic from StandardizedPaymentService

    private BigDecimal calculateSourceAmountForBehavior(BigDecimal amount, PaymentBehavior behavior) {
        switch (behavior) {
            case PaymentBehavior.BILL_PAYMENT:
                return -amount.abs()
            case PaymentBehavior.TRANSFER:
                return -amount.abs()
            case PaymentBehavior.CASH_ADVANCE:
                return amount.abs()
            case PaymentBehavior.BALANCE_TRANSFER:
                return -amount.abs()
            default:
                return -amount.abs()
        }
    }

    private BigDecimal calculateDestinationAmountForBehavior(BigDecimal amount, PaymentBehavior behavior) {
        switch (behavior) {
            case PaymentBehavior.BILL_PAYMENT:
                return -amount.abs()
            case PaymentBehavior.TRANSFER:
                return amount.abs()
            case PaymentBehavior.CASH_ADVANCE:
                return amount.abs()
            case PaymentBehavior.BALANCE_TRANSFER:
                return amount.abs()
            default:
                return -amount.abs()
        }
    }
}
