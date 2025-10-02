package finance.repositories

import finance.domain.Account
import finance.domain.AccountType
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spock.lang.Specification

import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType

class AccountRepositorySpec extends Specification {

    def "should extend JpaRepository interface"() {
        when:
        def interfaces = AccountRepository.getInterfaces()

        then:
        interfaces.length > 0
        interfaces.any { it == JpaRepository }
    }

    def "should be a proper Spring Data repository interface"() {
        when:
        def repositoryInterface = AccountRepository

        then:
        repositoryInterface.isInterface()
        repositoryInterface.name == "finance.repositories.AccountRepository"
    }

    def "should have declared query methods"() {
        when:
        def methods = AccountRepository.getDeclaredMethods()
        def methodNames = methods.collect { it.name }

        then:
        methodNames.contains("findByAccountNameOwner")
        methodNames.contains("findByActiveStatusOrderByAccountNameOwner")
        methodNames.contains("findByAccountType")
        methodNames.contains("findByActiveStatusAndAccountType")
        methodNames.contains("updateTotalsForAllAccounts")
        methodNames.contains("sumOfAllTransactionsByTransactionState")
        methodNames.contains("sumOfAllTransactionsByTransactionStateJpql")
        methodNames.contains("findAccountsThatRequirePayment")
        methodNames.contains("updateValidationDateForAccount")
        methodNames.contains("updateValidationDateForAllAccounts")
    }

    def "should have updateTotalsForAllAccounts method with proper annotations"() {
        when:
        def method = AccountRepository.getDeclaredMethods().find { it.name == "updateTotalsForAllAccounts" }

        then:
        method != null
        method.isAnnotationPresent(Modifying.class)
        method.isAnnotationPresent(Transactional.class)
        method.isAnnotationPresent(Query.class)

        def queryAnnotation = method.getAnnotation(Query.class)
        queryAnnotation.nativeQuery() == true
        queryAnnotation.value().contains("UPDATE t_account")
    }

    def "should have sumOfAllTransactionsByTransactionState method with Query annotation"() {
        when:
        def method = AccountRepository.getDeclaredMethods().find { it.name == "sumOfAllTransactionsByTransactionState" }

        then:
        method != null
        method.returnType == java.math.BigDecimal.class
        method.isAnnotationPresent(Query.class)

        def queryAnnotation = method.getAnnotation(Query.class)
        queryAnnotation.nativeQuery() == true
        queryAnnotation.value().contains("SELECT COALESCE")

        // Check if method has @Param annotation on parameters
        method.parameters.any { param ->
            param.isAnnotationPresent(Param.class) &&
            param.getAnnotation(Param.class).value() == "transactionState"
        }
    }

    def "should have sumOfAllTransactionsByTransactionStateJpql method with JPQL query"() {
        when:
        def method = AccountRepository.getDeclaredMethods().find { it.name == "sumOfAllTransactionsByTransactionStateJpql" }

        then:
        method != null
        method.returnType == java.math.BigDecimal.class
        method.isAnnotationPresent(Query.class)

        def queryAnnotation = method.getAnnotation(Query.class)
        queryAnnotation.nativeQuery() == false  // JPQL query, not native
        queryAnnotation.value().contains("FROM Transaction t")

        // Check if method has @Param annotation on parameters
        method.parameters.any { param ->
            param.isAnnotationPresent(Param.class) &&
            param.getAnnotation(Param.class).value() == "transactionState"
        }
    }

    def "should have findAccountsThatRequirePayment method with Query annotation"() {
        when:
        def method = AccountRepository.getDeclaredMethods().find { it.name == "findAccountsThatRequirePayment" }

        then:
        method != null
        method.returnType == List.class
        method.isAnnotationPresent(Query.class)

        def queryAnnotation = method.getAnnotation(Query.class)
        queryAnnotation.value().contains("FROM Account a")
        queryAnnotation.value().contains("ORDER BY a.accountNameOwner")
    }

    def "should have updateValidationDateForAccount method with proper annotations"() {
        when:
        def method = AccountRepository.getDeclaredMethods().find { it.name == "updateValidationDateForAccount" }

        then:
        method != null
        method.returnType == int.class
        method.isAnnotationPresent(Modifying.class)
        method.isAnnotationPresent(Transactional.class)
        method.isAnnotationPresent(Query.class)

        def queryAnnotation = method.getAnnotation(Query.class)
        queryAnnotation.nativeQuery() == true
        queryAnnotation.value().contains("UPDATE t_account a")
        queryAnnotation.value().contains("SET validation_date = sub.max_validation_date")

        // Check if method has @Param annotation on parameters
        method.parameters.any { param ->
            param.isAnnotationPresent(Param.class) &&
            param.getAnnotation(Param.class).value() == "accountId"
        }
    }

    def "should have updateValidationDateForAllAccounts method with proper annotations"() {
        when:
        def method = AccountRepository.getDeclaredMethods().find { it.name == "updateValidationDateForAllAccounts" }

        then:
        method != null
        method.isAnnotationPresent(Modifying.class)
        method.isAnnotationPresent(Transactional.class)
        method.isAnnotationPresent(Query.class)

        def queryAnnotation = method.getAnnotation(Query.class)
        queryAnnotation.nativeQuery() == true
        queryAnnotation.value().contains("UPDATE t_account a")
        queryAnnotation.value().contains("FROM (")
        queryAnnotation.value().contains("MAX(va.validation_date)")
    }

    def "should have all query methods properly annotated"() {
        when:
        def methods = AccountRepository.getDeclaredMethods()
        def queryMethods = methods.findAll { method ->
            method.isAnnotationPresent(Query.class)
        }

        then:
        queryMethods.size() >= 5  // At least 5 methods with @Query annotations

        // Verify all modifying queries have @Modifying and @Transactional
        queryMethods.findAll { it.isAnnotationPresent(Modifying.class) }.each { method ->
            assert method.isAnnotationPresent(Transactional.class)
        }
    }

    def "should have proper method return types"() {
        when:
        def methods = AccountRepository.getDeclaredMethods()

        then:
        // Check specific return types
        methods.find { it.name == "findByAccountNameOwner" }?.returnType == Optional.class
        methods.find { it.name == "findByActiveStatusOrderByAccountNameOwner" }?.returnType == List.class
        methods.find { it.name == "findByAccountType" }?.returnType == List.class
        methods.find { it.name == "findByActiveStatusAndAccountType" }?.returnType == List.class
        methods.find { it.name == "findAccountsThatRequirePayment" }?.returnType == List.class
        methods.find { it.name == "sumOfAllTransactionsByTransactionState" }?.returnType == java.math.BigDecimal.class
        methods.find { it.name == "sumOfAllTransactionsByTransactionStateJpql" }?.returnType == java.math.BigDecimal.class
        methods.find { it.name == "updateValidationDateForAccount" }?.returnType == int.class
    }
}