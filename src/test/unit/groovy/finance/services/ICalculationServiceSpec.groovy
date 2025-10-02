package finance.services

import finance.domain.Totals
import finance.domain.Transaction
import finance.domain.TransactionState
import spock.lang.Specification

import java.lang.reflect.Method

class ICalculationServiceSpec extends Specification {

    def "should define calculateActiveTotalsByAccountNameOwner method"() {
        when:
        Method method = ICalculationService.getMethod("calculateActiveTotalsByAccountNameOwner", String.class)

        then:
        method != null
        method.returnType == Totals.class
        method.parameterTypes.length == 1
        method.parameterTypes[0] == String.class
    }

    def "should define calculateTotalsFromTransactions method"() {
        when:
        Method method = ICalculationService.getMethod("calculateTotalsFromTransactions", List.class)

        then:
        method != null
        method.returnType == Map.class
        method.parameterTypes.length == 1
        method.parameterTypes[0] == List.class
    }

    def "should define calculateGrandTotal method"() {
        when:
        Method method = ICalculationService.getMethod("calculateGrandTotal", Map.class)

        then:
        method != null
        method.returnType == java.math.BigDecimal.class
        method.parameterTypes.length == 1
        method.parameterTypes[0] == Map.class
    }

    def "should define createTotals method"() {
        when:
        def methods = ICalculationService.getDeclaredMethods().findAll { it.name == "createTotals" }

        then:
        methods.size() == 1
        def method = methods[0]
        method.returnType == Totals.class
        method.parameterTypes.length == 3
        method.parameterTypes.every { it == java.math.BigDecimal.class }
    }

    def "should define validateTotals method"() {
        when:
        Method method = ICalculationService.getMethod("validateTotals", Totals.class)

        then:
        method != null
        method.returnType == boolean.class
        method.parameterTypes.length == 1
        method.parameterTypes[0] == Totals.class
    }

    def "should be a proper interface"() {
        when:
        def serviceInterface = ICalculationService

        then:
        serviceInterface.isInterface()
        serviceInterface.name == "finance.services.ICalculationService"
        serviceInterface.package.name == "finance.services"
    }

    def "should have all required methods defined"() {
        when:
        def methods = ICalculationService.getDeclaredMethods()
        def methodNames = methods.collect { it.name }

        then:
        methodNames.containsAll([
            "calculateActiveTotalsByAccountNameOwner",
            "calculateTotalsFromTransactions",
            "calculateGrandTotal",
            "createTotals",
            "validateTotals"
        ])
    }

    def "should have proper method documentation through interface contract"() {
        when:
        def methods = ICalculationService.getDeclaredMethods()

        then:
        methods.size() == 5  // Expected number of interface methods
        methods.every { method ->
            // All methods should be public abstract (interface methods)
            method.isDefault() == false
        }
    }
}