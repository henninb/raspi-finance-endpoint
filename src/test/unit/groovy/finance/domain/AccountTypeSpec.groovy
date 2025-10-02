package finance.domain

import spock.lang.Specification
import spock.lang.Unroll

class AccountTypeSpec extends Specification {

    def "should have correct label and category for each account type"() {
        expect: "account type has expected label and category"
        accountType.label == expectedLabel
        accountType.category == expectedCategory

        where:
        accountType                    | expectedLabel           | expectedCategory
        AccountType.Credit             | "credit"                | "liability"
        AccountType.Debit              | "debit"                 | "asset"
        AccountType.Undefined          | "undefined"             | "unknown"
        AccountType.Checking           | "checking"              | "asset"
        AccountType.Savings            | "savings"               | "asset"
        AccountType.CreditCard         | "credit_card"           | "liability"
        AccountType.Certificate        | "certificate"           | "asset"
        AccountType.MoneyMarket        | "money_market"          | "asset"
        AccountType.Brokerage          | "brokerage"             | "asset"
        AccountType.Retirement401k     | "retirement_401k"       | "asset"
        AccountType.RetirementIRA      | "retirement_ira"        | "asset"
        AccountType.RetirementRoth     | "retirement_roth"       | "asset"
        AccountType.Pension            | "pension"               | "asset"
        AccountType.HSA                | "hsa"                   | "asset"
        AccountType.FSA                | "fsa"                   | "asset"
        AccountType.MedicalSavings     | "medical_savings"       | "asset"
        AccountType.Mortgage           | "mortgage"              | "liability"
        AccountType.AutoLoan           | "auto_loan"             | "liability"
        AccountType.StudentLoan        | "student_loan"          | "liability"
        AccountType.PersonalLoan       | "personal_loan"         | "liability"
        AccountType.LineOfCredit       | "line_of_credit"        | "liability"
        AccountType.Utility            | "utility"               | "expense"
        AccountType.Prepaid            | "prepaid"               | "asset"
        AccountType.GiftCard           | "gift_card"             | "asset"
        AccountType.BusinessChecking   | "business_checking"     | "asset"
        AccountType.BusinessSavings    | "business_savings"      | "asset"
        AccountType.BusinessCredit     | "business_credit"       | "liability"
        AccountType.Cash               | "cash"                  | "asset"
        AccountType.Escrow             | "escrow"                | "asset"
        AccountType.Trust              | "trust"                 | "asset"
    }

    def "toString should return lowercase name"() {
        expect:
        AccountType.Checking.toString() == "checking"
        AccountType.CreditCard.toString() == "creditcard"
        AccountType.Retirement401k.toString() == "retirement401k"
        AccountType.HSA.toString() == "hsa"
    }

    def "getAssetTypes should return all asset account types"() {
        when: "getting asset types"
        def assetTypes = AccountType.getAssetTypes()

        then: "all asset types are returned"
        assetTypes.every { it.category == "asset" }
        assetTypes.size() > 10

        and: "common asset types are included"
        assetTypes.contains(AccountType.Checking)
        assetTypes.contains(AccountType.Savings)
        assetTypes.contains(AccountType.Brokerage)
        assetTypes.contains(AccountType.HSA)
        assetTypes.contains(AccountType.Cash)

        and: "liability types are not included"
        !assetTypes.contains(AccountType.Credit)
        !assetTypes.contains(AccountType.CreditCard)
        !assetTypes.contains(AccountType.Mortgage)
    }

    def "getLiabilityTypes should return all liability account types"() {
        when: "getting liability types"
        def liabilityTypes = AccountType.getLiabilityTypes()

        then: "all liability types are returned"
        liabilityTypes.every { it.category == "liability" }
        liabilityTypes.size() > 5

        and: "common liability types are included"
        liabilityTypes.contains(AccountType.Credit)
        liabilityTypes.contains(AccountType.CreditCard)
        liabilityTypes.contains(AccountType.Mortgage)
        liabilityTypes.contains(AccountType.AutoLoan)
        liabilityTypes.contains(AccountType.StudentLoan)

        and: "asset types are not included"
        !liabilityTypes.contains(AccountType.Checking)
        !liabilityTypes.contains(AccountType.Savings)
        !liabilityTypes.contains(AccountType.Brokerage)
    }

    def "getMedicalTypes should return medical-related account types"() {
        when: "getting medical types"
        def medicalTypes = AccountType.getMedicalTypes()

        then: "medical account types are returned"
        medicalTypes.size() == 3
        medicalTypes.contains(AccountType.HSA)
        medicalTypes.contains(AccountType.FSA)
        medicalTypes.contains(AccountType.MedicalSavings)

        and: "non-medical types are not included"
        !medicalTypes.contains(AccountType.Checking)
        !medicalTypes.contains(AccountType.Brokerage)
    }

    def "getInvestmentTypes should return investment account types"() {
        when: "getting investment types"
        def investmentTypes = AccountType.getInvestmentTypes()

        then: "investment account types are returned"
        investmentTypes.size() == 5
        investmentTypes.contains(AccountType.Brokerage)
        investmentTypes.contains(AccountType.Retirement401k)
        investmentTypes.contains(AccountType.RetirementIRA)
        investmentTypes.contains(AccountType.RetirementRoth)
        investmentTypes.contains(AccountType.Pension)

        and: "non-investment types are not included"
        !investmentTypes.contains(AccountType.Checking)
        !investmentTypes.contains(AccountType.Savings)
        !investmentTypes.contains(AccountType.HSA)
    }

    def "getBusinessTypes should return business account types"() {
        when: "getting business types"
        def businessTypes = AccountType.getBusinessTypes()

        then: "business account types are returned"
        businessTypes.size() == 3
        businessTypes.contains(AccountType.BusinessChecking)
        businessTypes.contains(AccountType.BusinessSavings)
        businessTypes.contains(AccountType.BusinessCredit)

        and: "personal types are not included"
        !businessTypes.contains(AccountType.Checking)
        !businessTypes.contains(AccountType.Savings)
        !businessTypes.contains(AccountType.CreditCard)
    }

    @Unroll
    def "asset types should have correct category: #accountType"() {
        expect:
        accountType.category == "asset"

        where:
        accountType << AccountType.getAssetTypes()
    }

    @Unroll
    def "liability types should have correct category: #accountType"() {
        expect:
        accountType.category == "liability"

        where:
        accountType << AccountType.getLiabilityTypes()
    }

    def "medical types should all be assets"() {
        when: "getting medical types"
        def medicalTypes = AccountType.getMedicalTypes()

        then: "all medical types are assets"
        medicalTypes.every { it.category == "asset" }
    }

    def "investment types should all be assets"() {
        when: "getting investment types"
        def investmentTypes = AccountType.getInvestmentTypes()

        then: "all investment types are assets"
        investmentTypes.every { it.category == "asset" }
    }

    def "business types should have varied categories"() {
        when: "getting business types"
        def businessTypes = AccountType.getBusinessTypes()

        then: "business types include both assets and liabilities"
        businessTypes.any { it.category == "asset" }
        businessTypes.any { it.category == "liability" }

        and: "specific categorization is correct"
        AccountType.BusinessChecking.category == "asset"
        AccountType.BusinessSavings.category == "asset"
        AccountType.BusinessCredit.category == "liability"
    }

    def "no overlap between asset and liability types"() {
        given: "asset and liability type lists"
        def assetTypes = AccountType.getAssetTypes()
        def liabilityTypes = AccountType.getLiabilityTypes()

        expect: "no overlap exists"
        assetTypes.intersect(liabilityTypes).isEmpty()
    }

    def "all enum values should be categorized"() {
        when: "getting all account types"
        def allTypes = AccountType.values() as List

        then: "every type has a valid category"
        allTypes.every { it.category in ["asset", "liability", "expense", "unknown"] }
    }
}
