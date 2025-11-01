package finance.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Defines the behavior of a payment based on source and destination account types.
 *
 * Payment behavior determines how transaction amounts are signed to correctly
 * reflect balance impacts on different account types (assets vs liabilities).
 *
 * @property label Human-readable label for the behavior
 * @property description Detailed explanation of the payment behavior
 */
@JsonFormat
enum class PaymentBehavior(
    val label: String,
    val description: String,
) {
    /**
     * Asset account paying down a liability account (e.g., checking pays credit card)
     * Source: negative (asset decreases)
     * Destination: negative (liability decreases - debt paid down)
     */
    @JsonProperty("bill_payment")
    BILL_PAYMENT(
        "bill_payment",
        "Payment from asset account to liability account (paying down debt)",
    ),

    /**
     * Asset account transferring to another asset account (e.g., checking to savings)
     * Source: negative (asset decreases)
     * Destination: positive (asset increases)
     */
    @JsonProperty("transfer")
    TRANSFER(
        "transfer",
        "Transfer between two asset accounts",
    ),

    /**
     * Liability account providing funds to asset account (e.g., credit card cash advance)
     * Source: positive (liability increases - more debt)
     * Destination: positive (asset increases - cash received)
     */
    @JsonProperty("cash_advance")
    CASH_ADVANCE(
        "cash_advance",
        "Cash advance from liability account to asset account (borrowing)",
    ),

    /**
     * Liability account transferring to another liability account (e.g., paying one credit card with another)
     * Source: positive (liability increases - charging on this card)
     * Destination: negative (liability decreases - paying off this card)
     */
    @JsonProperty("balance_transfer")
    BALANCE_TRANSFER(
        "balance_transfer",
        "Balance transfer between two liability accounts (using one credit card to pay another)",
    ),

    /**
     * Unknown or unsupported account type combination
     * Default: both transactions negative (safest option)
     */
    @JsonProperty("undefined")
    UNDEFINED(
        "undefined",
        "Unknown or unsupported account type combination",
    ),
    ;

    override fun toString(): String = label

    companion object {
        /**
         * Determines the appropriate payment behavior based on account type categories.
         *
         * @param sourceAccountType The account type of the payment source
         * @param destinationAccountType The account type of the payment destination
         * @return The inferred PaymentBehavior
         */
        @JvmStatic
        fun inferBehavior(
            sourceAccountType: AccountType,
            destinationAccountType: AccountType,
        ): PaymentBehavior {
            val sourceCategory = sourceAccountType.category
            val destCategory = destinationAccountType.category

            return when {
                sourceCategory == "asset" && destCategory == "liability" -> BILL_PAYMENT
                sourceCategory == "asset" && destCategory == "asset" -> TRANSFER
                sourceCategory == "liability" && destCategory == "asset" -> CASH_ADVANCE
                sourceCategory == "liability" && destCategory == "liability" -> BALANCE_TRANSFER
                else -> UNDEFINED
            }
        }
    }
}
