package finance.utils

import finance.domain.AccountType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class AccountTypeConverter : AttributeConverter<AccountType, String> {
    override fun convertToDatabaseColumn(attribute: AccountType): String = attribute.label

    override fun convertToEntityAttribute(attribute: String): AccountType =
        when (attribute.trim().lowercase()) {
            // Existing types (preserve compatibility)
            "credit" -> AccountType.Credit
            "debit" -> AccountType.Debit
            "undefined" -> AccountType.Undefined

            // Banking/Traditional Accounts
            "checking" -> AccountType.Checking
            "savings" -> AccountType.Savings
            "credit_card" -> AccountType.CreditCard
            "certificate" -> AccountType.Certificate
            "money_market" -> AccountType.MoneyMarket

            // Investment Accounts
            "brokerage" -> AccountType.Brokerage
            "retirement_401k" -> AccountType.Retirement401k
            "retirement_ira" -> AccountType.RetirementIRA
            "retirement_roth" -> AccountType.RetirementRoth
            "pension" -> AccountType.Pension

            // Medical/Healthcare Accounts
            "hsa" -> AccountType.HSA
            "fsa" -> AccountType.FSA
            "medical_savings" -> AccountType.MedicalSavings

            // Loan/Debt Accounts
            "mortgage" -> AccountType.Mortgage
            "auto_loan" -> AccountType.AutoLoan
            "student_loan" -> AccountType.StudentLoan
            "personal_loan" -> AccountType.PersonalLoan
            "line_of_credit" -> AccountType.LineOfCredit

            // Utility/Service Accounts
            "utility" -> AccountType.Utility
            "prepaid" -> AccountType.Prepaid
            "gift_card" -> AccountType.GiftCard

            // Business Accounts
            "business_checking" -> AccountType.BusinessChecking
            "business_savings" -> AccountType.BusinessSavings
            "business_credit" -> AccountType.BusinessCredit

            // Other/Miscellaneous
            "cash" -> AccountType.Cash
            "escrow" -> AccountType.Escrow
            "trust" -> AccountType.Trust

            else -> throw RuntimeException("Unknown account type attribute: $attribute")
        }
}
