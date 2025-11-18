package finance.domain

import spock.lang.Specification
import spock.lang.Unroll

class PaymentBehaviorSpec extends Specification {

    @Unroll
    def "inferBehavior should return #expectedBehavior when source is #sourceType and destination is #destType"() {
        when: "inferring payment behavior from account types"
        def behavior = PaymentBehavior.inferBehavior(sourceType, destType)

        then: "the correct behavior is returned"
        behavior == expectedBehavior

        where:
        sourceType              | destType                | expectedBehavior
        AccountType.Checking    | AccountType.CreditCard  | PaymentBehavior.BILL_PAYMENT
        AccountType.Savings     | AccountType.CreditCard  | PaymentBehavior.BILL_PAYMENT
        AccountType.Cash        | AccountType.Mortgage    | PaymentBehavior.BILL_PAYMENT
        AccountType.Brokerage   | AccountType.AutoLoan    | PaymentBehavior.BILL_PAYMENT
        AccountType.Checking    | AccountType.Savings     | PaymentBehavior.TRANSFER
        AccountType.Savings     | AccountType.Checking    | PaymentBehavior.TRANSFER
        AccountType.Cash        | AccountType.Brokerage   | PaymentBehavior.TRANSFER
        AccountType.HSA         | AccountType.Checking    | PaymentBehavior.TRANSFER
        AccountType.CreditCard  | AccountType.Checking    | PaymentBehavior.CASH_ADVANCE
        AccountType.Mortgage    | AccountType.Savings     | PaymentBehavior.CASH_ADVANCE
        AccountType.AutoLoan    | AccountType.Cash        | PaymentBehavior.CASH_ADVANCE
        AccountType.CreditCard  | AccountType.CreditCard  | PaymentBehavior.BALANCE_TRANSFER
        AccountType.Mortgage    | AccountType.AutoLoan    | PaymentBehavior.BALANCE_TRANSFER
        AccountType.AutoLoan    | AccountType.Mortgage    | PaymentBehavior.BALANCE_TRANSFER
        AccountType.Undefined   | AccountType.Checking    | PaymentBehavior.UNDEFINED
        AccountType.Checking    | AccountType.Undefined   | PaymentBehavior.UNDEFINED
    }

    def "inferBehavior should handle legacy Debit and Credit account types"() {
        when: "using legacy Debit account type as source"
        def debitToCredit = PaymentBehavior.inferBehavior(AccountType.Debit, AccountType.Credit)

        then: "BILL_PAYMENT is inferred"
        debitToCredit == PaymentBehavior.BILL_PAYMENT

        when: "using asset to asset with legacy types"
        def debitToDebit = PaymentBehavior.inferBehavior(AccountType.Debit, AccountType.Debit)

        then: "TRANSFER is inferred"
        debitToDebit == PaymentBehavior.TRANSFER

        when: "using liability to asset with legacy types"
        def creditToDebit = PaymentBehavior.inferBehavior(AccountType.Credit, AccountType.Debit)

        then: "CASH_ADVANCE is inferred"
        creditToDebit == PaymentBehavior.CASH_ADVANCE

        when: "using liability to liability with legacy types"
        def creditToCredit = PaymentBehavior.inferBehavior(AccountType.Credit, AccountType.Credit)

        then: "BALANCE_TRANSFER is inferred"
        creditToCredit == PaymentBehavior.BALANCE_TRANSFER
    }

    def "inferBehavior should correctly classify all asset account types"() {
        given: "a list of all asset account types"
        def assetTypes = AccountType.getAssetTypes()

        when: "creating a payment from asset to liability"
        def behaviors = assetTypes.collect { assetType ->
            PaymentBehavior.inferBehavior(assetType, AccountType.CreditCard)
        }

        then: "all should be BILL_PAYMENT"
        behaviors.every { it == PaymentBehavior.BILL_PAYMENT }

        when: "creating a payment from asset to asset"
        def transferBehaviors = assetTypes.collect { assetType ->
            PaymentBehavior.inferBehavior(assetType, AccountType.Checking)
        }

        then: "all should be TRANSFER"
        transferBehaviors.every { it == PaymentBehavior.TRANSFER }
    }

    def "inferBehavior should correctly classify all liability account types"() {
        given: "a list of all liability account types"
        def liabilityTypes = AccountType.getLiabilityTypes()

        when: "creating a payment from liability to asset"
        def behaviors = liabilityTypes.collect { liabilityType ->
            PaymentBehavior.inferBehavior(liabilityType, AccountType.Checking)
        }

        then: "all should be CASH_ADVANCE"
        behaviors.every { it == PaymentBehavior.CASH_ADVANCE }

        when: "creating a payment from liability to liability"
        def balanceTransferBehaviors = liabilityTypes.collect { liabilityType ->
            PaymentBehavior.inferBehavior(liabilityType, AccountType.CreditCard)
        }

        then: "all should be BALANCE_TRANSFER"
        balanceTransferBehaviors.every { it == PaymentBehavior.BALANCE_TRANSFER }
    }

    def "PaymentBehavior enum should have correct labels and descriptions"() {
        expect: "each behavior has appropriate label and description"
        PaymentBehavior.BILL_PAYMENT.label == "bill_payment"
        PaymentBehavior.BILL_PAYMENT.description.contains("asset")
        PaymentBehavior.BILL_PAYMENT.description.contains("liability")

        PaymentBehavior.TRANSFER.label == "transfer"
        PaymentBehavior.TRANSFER.description.contains("asset")

        PaymentBehavior.CASH_ADVANCE.label == "cash_advance"
        PaymentBehavior.CASH_ADVANCE.description.contains("liability")
        PaymentBehavior.CASH_ADVANCE.description.contains("asset")

        PaymentBehavior.BALANCE_TRANSFER.label == "balance_transfer"
        PaymentBehavior.BALANCE_TRANSFER.description.contains("liability")

        PaymentBehavior.UNDEFINED.label == "undefined"
    }

    def "PaymentBehavior toString should return label"() {
        expect: "toString returns the label"
        PaymentBehavior.BILL_PAYMENT.toString() == "bill_payment"
        PaymentBehavior.TRANSFER.toString() == "transfer"
        PaymentBehavior.CASH_ADVANCE.toString() == "cash_advance"
        PaymentBehavior.BALANCE_TRANSFER.toString() == "balance_transfer"
        PaymentBehavior.UNDEFINED.toString() == "undefined"
    }

    def "inferBehavior should handle medical account types correctly"() {
        given: "medical account types"
        def medicalTypes = AccountType.getMedicalTypes()

        when: "creating payment from medical account to liability"
        def billPaymentBehaviors = medicalTypes.collect { medicalType ->
            PaymentBehavior.inferBehavior(medicalType, AccountType.CreditCard)
        }

        then: "all should be BILL_PAYMENT"
        billPaymentBehaviors.every { it == PaymentBehavior.BILL_PAYMENT }

        when: "creating payment from medical account to checking"
        def transferBehaviors = medicalTypes.collect { medicalType ->
            PaymentBehavior.inferBehavior(medicalType, AccountType.Checking)
        }

        then: "all should be TRANSFER"
        transferBehaviors.every { it == PaymentBehavior.TRANSFER }
    }

    def "inferBehavior should handle investment account types correctly"() {
        given: "investment account types"
        def investmentTypes = AccountType.getInvestmentTypes()

        when: "creating payment from investment account to liability"
        def billPaymentBehaviors = investmentTypes.collect { investmentType ->
            PaymentBehavior.inferBehavior(investmentType, AccountType.Mortgage)
        }

        then: "all should be BILL_PAYMENT"
        billPaymentBehaviors.every { it == PaymentBehavior.BILL_PAYMENT }

        when: "creating payment from investment account to savings"
        def transferBehaviors = investmentTypes.collect { investmentType ->
            PaymentBehavior.inferBehavior(investmentType, AccountType.Savings)
        }

        then: "all should be TRANSFER"
        transferBehaviors.every { it == PaymentBehavior.TRANSFER }
    }

    def "inferBehavior should handle business account types correctly"() {
        when: "creating payment from business checking to business savings"
        def behavior1 = PaymentBehavior.inferBehavior(
            AccountType.BusinessChecking,
            AccountType.BusinessSavings
        )

        then: "should be TRANSFER (both are assets)"
        behavior1 == PaymentBehavior.TRANSFER

        when: "creating payment from business checking to business credit"
        def behavior2 = PaymentBehavior.inferBehavior(
            AccountType.BusinessChecking,
            AccountType.BusinessCredit
        )

        then: "should be BILL_PAYMENT (asset to liability)"
        behavior2 == PaymentBehavior.BILL_PAYMENT

        when: "creating payment from business credit to business checking"
        def behavior3 = PaymentBehavior.inferBehavior(
            AccountType.BusinessCredit,
            AccountType.BusinessChecking
        )

        then: "should be CASH_ADVANCE (liability to asset)"
        behavior3 == PaymentBehavior.CASH_ADVANCE
    }

    def "inferBehavior behavior matrix matches expected real-world scenarios"() {
        expect: "behavior inference matches real-world use cases"
        // Paying credit card bill from checking
        PaymentBehavior.inferBehavior(AccountType.Checking, AccountType.CreditCard) == PaymentBehavior.BILL_PAYMENT

        // Moving money from checking to savings
        PaymentBehavior.inferBehavior(AccountType.Checking, AccountType.Savings) == PaymentBehavior.TRANSFER

        // Taking cash advance from credit card
        PaymentBehavior.inferBehavior(AccountType.CreditCard, AccountType.Checking) == PaymentBehavior.CASH_ADVANCE

        // Balance transfer between credit cards
        PaymentBehavior.inferBehavior(AccountType.CreditCard, AccountType.CreditCard) == PaymentBehavior.BALANCE_TRANSFER

        // Paying mortgage from savings
        PaymentBehavior.inferBehavior(AccountType.Savings, AccountType.Mortgage) == PaymentBehavior.BILL_PAYMENT

        // Transferring from HSA to checking
        PaymentBehavior.inferBehavior(AccountType.HSA, AccountType.Checking) == PaymentBehavior.TRANSFER
    }

    def "inferBehavior should handle all combinations of asset and liability systematically"() {
        given: "lists of all asset and liability types"
        def assetTypes = AccountType.getAssetTypes()
        def liabilityTypes = AccountType.getLiabilityTypes()

        when: "testing all asset-to-liability combinations"
        def assetToLiabilityResults = assetTypes.collectMany { asset ->
            liabilityTypes.collect { liability ->
                PaymentBehavior.inferBehavior(asset, liability)
            }
        }

        then: "all should be BILL_PAYMENT"
        assetToLiabilityResults.every { it == PaymentBehavior.BILL_PAYMENT }
        assetToLiabilityResults.size() == assetTypes.size() * liabilityTypes.size()
    }

    def "inferBehavior should handle all combinations of liability and asset systematically"() {
        given: "lists of all asset and liability types"
        def assetTypes = AccountType.getAssetTypes()
        def liabilityTypes = AccountType.getLiabilityTypes()

        when: "testing all liability-to-asset combinations"
        def liabilityToAssetResults = liabilityTypes.collectMany { liability ->
            assetTypes.collect { asset ->
                PaymentBehavior.inferBehavior(liability, asset)
            }
        }

        then: "all should be CASH_ADVANCE"
        liabilityToAssetResults.every { it == PaymentBehavior.CASH_ADVANCE }
        liabilityToAssetResults.size() == assetTypes.size() * liabilityTypes.size()
    }

    def "inferBehavior should handle all asset-to-asset pairwise combinations"() {
        given: "list of all asset types"
        def assetTypes = AccountType.getAssetTypes()

        when: "testing all asset-to-asset combinations (including self)"
        def assetToAssetResults = assetTypes.collectMany { source ->
            assetTypes.collect { dest ->
                PaymentBehavior.inferBehavior(source, dest)
            }
        }

        then: "all should be TRANSFER"
        assetToAssetResults.every { it == PaymentBehavior.TRANSFER }
        assetToAssetResults.size() == assetTypes.size() * assetTypes.size()
    }

    def "inferBehavior should handle all liability-to-liability pairwise combinations"() {
        given: "list of all liability types"
        def liabilityTypes = AccountType.getLiabilityTypes()

        when: "testing all liability-to-liability combinations (including self)"
        def liabilityToLiabilityResults = liabilityTypes.collectMany { source ->
            liabilityTypes.collect { dest ->
                PaymentBehavior.inferBehavior(source, dest)
            }
        }

        then: "all should be BALANCE_TRANSFER"
        liabilityToLiabilityResults.every { it == PaymentBehavior.BALANCE_TRANSFER }
        liabilityToLiabilityResults.size() == liabilityTypes.size() * liabilityTypes.size()
    }

    @Unroll
    def "inferBehavior with Undefined should always return UNDEFINED for #scenario"() {
        when: "inferring behavior with Undefined account type"
        def behavior = PaymentBehavior.inferBehavior(sourceType, destType)

        then: "should return UNDEFINED"
        behavior == PaymentBehavior.UNDEFINED

        where:
        scenario                            | sourceType              | destType
        "Undefined source to asset"         | AccountType.Undefined   | AccountType.Checking
        "Undefined source to liability"     | AccountType.Undefined   | AccountType.CreditCard
        "Asset to Undefined destination"    | AccountType.Savings     | AccountType.Undefined
        "Liability to Undefined dest"       | AccountType.Mortgage    | AccountType.Undefined
        "Undefined to Undefined"            | AccountType.Undefined   | AccountType.Undefined
    }

    def "inferBehavior with expense category account should return UNDEFINED"() {
        when: "using Utility account type (expense category)"
        def assetToExpense = PaymentBehavior.inferBehavior(AccountType.Checking, AccountType.Utility)
        def expenseToAsset = PaymentBehavior.inferBehavior(AccountType.Utility, AccountType.Savings)
        def expenseToLiability = PaymentBehavior.inferBehavior(AccountType.Utility, AccountType.CreditCard)
        def expenseToExpense = PaymentBehavior.inferBehavior(AccountType.Utility, AccountType.Utility)

        then: "all should return UNDEFINED (expense is not asset or liability)"
        assetToExpense == PaymentBehavior.UNDEFINED
        expenseToAsset == PaymentBehavior.UNDEFINED
        expenseToLiability == PaymentBehavior.UNDEFINED
        expenseToExpense == PaymentBehavior.UNDEFINED
    }

    def "PaymentBehavior enum should have exactly 5 values"() {
        when: "checking enum values"
        def values = PaymentBehavior.values()

        then: "should have all 5 expected behaviors"
        values.length == 5
        values.contains(PaymentBehavior.BILL_PAYMENT)
        values.contains(PaymentBehavior.TRANSFER)
        values.contains(PaymentBehavior.CASH_ADVANCE)
        values.contains(PaymentBehavior.BALANCE_TRANSFER)
        values.contains(PaymentBehavior.UNDEFINED)
    }

    @Unroll
    def "PaymentBehavior.valueOf should work for #behaviorName"() {
        when: "getting enum by name"
        def behavior = PaymentBehavior.valueOf(behaviorName)

        then: "should return correct enum value"
        behavior == expectedBehavior

        where:
        behaviorName        | expectedBehavior
        "BILL_PAYMENT"      | PaymentBehavior.BILL_PAYMENT
        "TRANSFER"          | PaymentBehavior.TRANSFER
        "CASH_ADVANCE"      | PaymentBehavior.CASH_ADVANCE
        "BALANCE_TRANSFER"  | PaymentBehavior.BALANCE_TRANSFER
        "UNDEFINED"         | PaymentBehavior.UNDEFINED
    }

    def "inferBehavior should be deterministic for same inputs"() {
        given: "repeated calls with same account types"
        def iterations = 100

        when: "calling inferBehavior multiple times"
        def results = (1..iterations).collect {
            PaymentBehavior.inferBehavior(AccountType.Checking, AccountType.CreditCard)
        }

        then: "all results should be identical"
        results.every { it == PaymentBehavior.BILL_PAYMENT }
        results.unique().size() == 1
    }

    def "inferBehavior with same account for source and destination should still work"() {
        when: "using same account type for both source and destination"
        def checkingToChecking = PaymentBehavior.inferBehavior(AccountType.Checking, AccountType.Checking)
        def creditToCredit = PaymentBehavior.inferBehavior(AccountType.CreditCard, AccountType.CreditCard)

        then: "should infer behavior based on category alone"
        checkingToChecking == PaymentBehavior.TRANSFER
        creditToCredit == PaymentBehavior.BALANCE_TRANSFER
    }
}
