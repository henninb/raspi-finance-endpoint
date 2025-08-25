package finance.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

@JsonFormat
enum class AccountType(val label: String, val category: String) {
    // Existing types (preserve compatibility)
    @JsonProperty("credit")
    Credit("credit", "liability"),

    @JsonProperty("debit")
    Debit("debit", "asset"),

    @JsonProperty("undefined")
    Undefined("undefined", "unknown"),

    // Banking/Traditional Accounts
    @JsonProperty("checking")
    Checking("checking", "asset"),

    @JsonProperty("savings")
    Savings("savings", "asset"),

    @JsonProperty("credit_card")
    CreditCard("credit_card", "liability"),

    @JsonProperty("certificate")
    Certificate("certificate", "asset"), // Certificates of Deposit

    @JsonProperty("money_market")
    MoneyMarket("money_market", "asset"),

    // Investment Accounts
    @JsonProperty("brokerage")
    Brokerage("brokerage", "asset"),

    @JsonProperty("retirement_401k")
    Retirement401k("retirement_401k", "asset"),

    @JsonProperty("retirement_ira")
    RetirementIRA("retirement_ira", "asset"),

    @JsonProperty("retirement_roth")
    RetirementRoth("retirement_roth", "asset"),

    @JsonProperty("pension")
    Pension("pension", "asset"),

    // Medical/Healthcare Accounts
    @JsonProperty("hsa")
    HSA("hsa", "asset"), // Health Savings Account

    @JsonProperty("fsa")
    FSA("fsa", "asset"), // Flexible Spending Account

    @JsonProperty("medical_savings")
    MedicalSavings("medical_savings", "asset"),

    // Loan/Debt Accounts
    @JsonProperty("mortgage")
    Mortgage("mortgage", "liability"),

    @JsonProperty("auto_loan")
    AutoLoan("auto_loan", "liability"),

    @JsonProperty("student_loan")
    StudentLoan("student_loan", "liability"),

    @JsonProperty("personal_loan")
    PersonalLoan("personal_loan", "liability"),

    @JsonProperty("line_of_credit")
    LineOfCredit("line_of_credit", "liability"),

    // Utility/Service Accounts
    @JsonProperty("utility")
    Utility("utility", "expense"),

    @JsonProperty("prepaid")
    Prepaid("prepaid", "asset"),

    @JsonProperty("gift_card")
    GiftCard("gift_card", "asset"),

    // Business Accounts
    @JsonProperty("business_checking")
    BusinessChecking("business_checking", "asset"),

    @JsonProperty("business_savings")
    BusinessSavings("business_savings", "asset"),

    @JsonProperty("business_credit")
    BusinessCredit("business_credit", "liability"),

    // Other/Miscellaneous
    @JsonProperty("cash")
    Cash("cash", "asset"),

    @JsonProperty("escrow")
    Escrow("escrow", "asset"),

    @JsonProperty("trust")
    Trust("trust", "asset");

    override fun toString(): String = name.lowercase()

    companion object {
        fun getAssetTypes(): List<AccountType> = values().filter { it.category == "asset" }
        fun getLiabilityTypes(): List<AccountType> = values().filter { it.category == "liability" }
        fun getMedicalTypes(): List<AccountType> = listOf(HSA, FSA, MedicalSavings)
        fun getInvestmentTypes(): List<AccountType> = listOf(Brokerage, Retirement401k, RetirementIRA, RetirementRoth, Pension)
        fun getBusinessTypes(): List<AccountType> = listOf(BusinessChecking, BusinessSavings, BusinessCredit)
    }
}