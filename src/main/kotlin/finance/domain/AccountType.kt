package finance.domain

import com.fasterxml.jackson.annotation.JsonCreator

enum class AccountType(
    override val label: String,
    val category: String,
) : LabeledEnum {
    Credit("credit", "liability"),
    Debit("debit", "asset"),
    Undefined("undefined", "unknown"),
    Checking("checking", "asset"),
    Savings("savings", "asset"),
    CreditCard("credit_card", "liability"),
    Certificate("certificate", "asset"),
    MoneyMarket("money_market", "asset"),
    Brokerage("brokerage", "asset"),
    Retirement401k("retirement_401k", "asset"),
    RetirementIRA("retirement_ira", "asset"),
    RetirementRoth("retirement_roth", "asset"),
    Pension("pension", "asset"),
    HSA("hsa", "asset"),
    FSA("fsa", "asset"),
    MedicalSavings("medical_savings", "asset"),
    Mortgage("mortgage", "liability"),
    AutoLoan("auto_loan", "liability"),
    StudentLoan("student_loan", "liability"),
    PersonalLoan("personal_loan", "liability"),
    LineOfCredit("line_of_credit", "liability"),
    Utility("utility", "expense"),
    Prepaid("prepaid", "asset"),
    GiftCard("gift_card", "asset"),
    BusinessChecking("business_checking", "asset"),
    BusinessSavings("business_savings", "asset"),
    BusinessCredit("business_credit", "liability"),
    Cash("cash", "asset"),
    Escrow("escrow", "asset"),
    Trust("trust", "asset"),
    ;

    val isAsset: Boolean get() = category == "asset"
    val isLiability: Boolean get() = category == "liability"

    override fun toString(): String = label

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(value: String?): AccountType = fromLabelOrThrow(value)

        @JvmStatic
        fun getAssetTypes(): List<AccountType> = values().filter { it.isAsset }

        @JvmStatic
        fun getLiabilityTypes(): List<AccountType> = values().filter { it.isLiability }

        @JvmStatic
        fun getMedicalTypes(): List<AccountType> = listOf(HSA, FSA, MedicalSavings)

        @JvmStatic
        fun getInvestmentTypes(): List<AccountType> = listOf(Brokerage, Retirement401k, RetirementIRA, RetirementRoth, Pension)

        @JvmStatic
        fun getBusinessTypes(): List<AccountType> = listOf(BusinessChecking, BusinessSavings, BusinessCredit)
    }

    fun taxBucket(): String =
        when (this) {
            Retirement401k, RetirementIRA, Pension, HSA, FSA, MedicalSavings -> "pretax"
            RetirementRoth -> "roth"
            else -> "taxable"
        }
}
