package finance.domain

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Additional unit tests for PaymentBehavior that cover edge cases
 * and scenarios not covered in the main spec.
 */
class PaymentBehaviorAdditionalSpec extends Specification {

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

    def "inferBehavior with unknown category should return UNDEFINED"() {
        when: "using account type with unknown category"
        def result = PaymentBehavior.inferBehavior(AccountType.Undefined, AccountType.Undefined)

        then: "should return UNDEFINED"
        result == PaymentBehavior.UNDEFINED
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

    def "PaymentBehavior enum should be serializable for JSON"() {
        expect: "each behavior can be converted to string and has label"
        PaymentBehavior.values().every { behavior ->
            behavior.toString() != null
            behavior.label != null
            behavior.description != null
        }
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

    def "inferBehavior should handle all medical account types correctly"() {
        given: "all medical account types"
        def medicalTypes = AccountType.getMedicalTypes()

        expect: "medical types are assets"
        medicalTypes.every { it.category == "asset" }

        when: "testing medical account combinations"
        def medicalToMedical = medicalTypes.collectMany { source ->
            medicalTypes.collect { dest ->
                PaymentBehavior.inferBehavior(source, dest)
            }
        }

        then: "all should be TRANSFER (asset to asset)"
        medicalToMedical.every { it == PaymentBehavior.TRANSFER }
    }

    def "inferBehavior should handle all investment account types correctly"() {
        given: "all investment account types"
        def investmentTypes = AccountType.getInvestmentTypes()

        expect: "investment types are assets"
        investmentTypes.every { it.category == "asset" }

        when: "testing investment account combinations"
        def investmentToInvestment = investmentTypes.collectMany { source ->
            investmentTypes.collect { dest ->
                PaymentBehavior.inferBehavior(source, dest)
            }
        }

        then: "all should be TRANSFER (asset to asset)"
        investmentToInvestment.every { it == PaymentBehavior.TRANSFER }
    }

    def "inferBehavior should handle all business account type combinations"() {
        given: "all business account types"
        def businessTypes = AccountType.getBusinessTypes()

        when: "separating business assets and liabilities"
        def businessAssets = businessTypes.findAll { it.category == "asset" }
        def businessLiabilities = businessTypes.findAll { it.category == "liability" }

        then: "business accounts include both categories"
        businessAssets.size() > 0
        businessLiabilities.size() > 0

        when: "testing business asset to liability"
        def assetToLiability = businessAssets.collectMany { asset ->
            businessLiabilities.collect { liability ->
                PaymentBehavior.inferBehavior(asset, liability)
            }
        }

        then: "should be BILL_PAYMENT"
        assetToLiability.every { it == PaymentBehavior.BILL_PAYMENT }

        when: "testing business liability to asset"
        def liabilityToAsset = businessLiabilities.collectMany { liability ->
            businessAssets.collect { asset ->
                PaymentBehavior.inferBehavior(liability, asset)
            }
        }

        then: "should be CASH_ADVANCE"
        liabilityToAsset.every { it == PaymentBehavior.CASH_ADVANCE }
    }

    def "inferBehavior should handle mixed business and personal account types"() {
        when: "business checking to personal credit card"
        def businessToPersonal = PaymentBehavior.inferBehavior(
                AccountType.BusinessChecking,
                AccountType.CreditCard
        )

        then: "should be BILL_PAYMENT (asset to liability)"
        businessToPersonal == PaymentBehavior.BILL_PAYMENT

        when: "personal checking to business credit"
        def personalToBusiness = PaymentBehavior.inferBehavior(
                AccountType.Checking,
                AccountType.BusinessCredit
        )

        then: "should be BILL_PAYMENT (asset to liability)"
        personalToBusiness == PaymentBehavior.BILL_PAYMENT
    }

    def "PaymentBehavior description should be meaningful and non-empty"() {
        expect: "all behaviors have meaningful descriptions"
        PaymentBehavior.values().every { behavior ->
            behavior.description != null
            behavior.description.length() > 10  // Reasonable minimum
            behavior.description.toLowerCase().contains("account") ||
                    behavior.description.toLowerCase().contains("debt") ||
                    behavior.description.toLowerCase().contains("unknown")
        }
    }

    def "PaymentBehavior label should match expected naming convention"() {
        expect: "labels use snake_case convention"
        PaymentBehavior.BILL_PAYMENT.label == "bill_payment"
        PaymentBehavior.CASH_ADVANCE.label == "cash_advance"
        PaymentBehavior.BALANCE_TRANSFER.label == "balance_transfer"

        and: "single-word labels are lowercase"
        PaymentBehavior.TRANSFER.label == "transfer"
        PaymentBehavior.UNDEFINED.label == "undefined"
    }

    def "inferBehavior with same account for source and destination should still work"() {
        when: "using same account type for both source and destination"
        def checkingToChecking = PaymentBehavior.inferBehavior(AccountType.Checking, AccountType.Checking)
        def creditToCredit = PaymentBehavior.inferBehavior(AccountType.CreditCard, AccountType.CreditCard)

        then: "should infer behavior based on category alone"
        checkingToChecking == PaymentBehavior.TRANSFER  // asset to asset
        creditToCredit == PaymentBehavior.BALANCE_TRANSFER  // liability to liability
    }
}
