package finance.domain

import com.fasterxml.jackson.annotation.JsonCreator

enum class PaymentBehavior(
    override val label: String,
    val description: String,
) : LabeledEnum {
    // asset → liability: source decreases, liability decreases (debt paid down)
    BILL_PAYMENT(
        "bill_payment",
        "Payment from asset account to liability account (paying down debt)",
    ),

    // asset → asset: source decreases, destination increases
    TRANSFER(
        "transfer",
        "Transfer between two asset accounts",
    ),

    // liability → asset: liability increases (more debt), asset increases (cash received)
    CASH_ADVANCE(
        "cash_advance",
        "Cash advance from liability account to asset account (borrowing)",
    ),

    // liability → liability: source liability increases (charging), destination liability decreases (paid off)
    BALANCE_TRANSFER(
        "balance_transfer",
        "Balance transfer between two liability accounts (using one credit card to pay another)",
    ),

    UNDEFINED(
        "undefined",
        "Unknown or unsupported account type combination",
    ),
    ;

    override fun toString(): String = label

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(value: String?): PaymentBehavior = fromLabelOrThrow(value)

        @JvmStatic
        fun inferBehavior(
            sourceAccountType: AccountType,
            destinationAccountType: AccountType,
        ): PaymentBehavior =
            when {
                sourceAccountType.isAsset && destinationAccountType.isLiability -> BILL_PAYMENT
                sourceAccountType.isAsset && destinationAccountType.isAsset -> TRANSFER
                sourceAccountType.isLiability && destinationAccountType.isAsset -> CASH_ADVANCE
                sourceAccountType.isLiability && destinationAccountType.isLiability -> BALANCE_TRANSFER
                else -> UNDEFINED
            }
    }
}
