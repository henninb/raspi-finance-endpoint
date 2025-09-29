package finance.repositories

import finance.BaseIntegrationSpec
import finance.domain.Account
import finance.domain.AccountType
import finance.helpers.SmartAccountBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Shared

import java.math.BigDecimal
import java.util.Optional

/**
 * MIGRATED INTEGRATION TEST - Account Repository Simple with robust, isolated architecture
 *
 * This is the migrated version of AccountRepositorySimpleIntSpec showing:
 * ✅ No hardcoded account names - all use testOwner for uniqueness
 * ✅ SmartBuilder pattern with constraint validation
 * ✅ Test isolation - each test gets its own test data
 * ✅ Constraint-aware test data creation using SmartAccountBuilder
 * ✅ Proper FK relationship management with TestDataManager
 * ✅ Financial validation and consistency
 */
class AccountRepositorySimpleIntSpec extends BaseIntegrationSpec {

    @Autowired
    AccountRepository accountRepository

    @Shared
    def repositoryContext

    def setupSpec() {
        repositoryContext = testFixtures.createRepositoryTestContext(testOwner)
    }

    def "test account repository basic CRUD operations"() {
        given:
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("testsavings")
                .asDebit()
                .withCleared(new BigDecimal("1500.50"))
                .withMoniker("1000")
                .buildAndValidate()

        when:
        Account savedAccount = accountRepository.save(account)

        then:
        savedAccount.accountId != null
        savedAccount.accountNameOwner.contains(testOwner.replaceAll(/[^a-z]/, ''))  // Match SmartAccountBuilder logic
        savedAccount.accountType == AccountType.Debit
        savedAccount.cleared == new BigDecimal("1500.50")

        when:
        Optional<Account> foundAccount = accountRepository.findByAccountNameOwner(savedAccount.accountNameOwner)

        then:
        foundAccount.isPresent()
        foundAccount.get().accountType == AccountType.Debit
        foundAccount.get().cleared == new BigDecimal("1500.50")
    }

    def "test find accounts by active status"() {
        given:
        // Create active account
        Account activeAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("activeaccount")
                .asDebit()
                .asActive()
                .withCleared(new BigDecimal("2000.00"))
                .withMoniker("2000")
                .buildAndValidate()

        // Create inactive account
        Account inactiveAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("inactiveaccount")
                .asCredit()
                .asInactive()
                .withOutstanding(new BigDecimal("500.00"))
                .withMoniker("3000")
                .buildAndValidate()

        accountRepository.save(activeAccount)
        accountRepository.save(inactiveAccount)

        when:
        List<Account> activeAccounts = accountRepository.findByActiveStatusOrderByAccountNameOwner(true)

        then:
        activeAccounts.size() >= 1
        activeAccounts.every { it.activeStatus == true }
        activeAccounts.any { it.accountNameOwner.contains(testOwner.replaceAll(/[^a-z]/, '')) }
    }

    def "test account repository update operations"() {
        given:
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("updatetest")
                .asDebit()
                .withCleared(new BigDecimal("1000.00"))
                .withMoniker("1100")
                .buildAndValidate()
        Account savedAccount = accountRepository.save(account)

        when:
        savedAccount.cleared = new BigDecimal("1500.00")
        savedAccount.outstanding = new BigDecimal("50.00")
        Account updatedAccount = accountRepository.save(savedAccount)

        then:
        updatedAccount.cleared == new BigDecimal("1500.00")
        updatedAccount.outstanding == new BigDecimal("50.00")
        updatedAccount.accountNameOwner.contains(testOwner.replaceAll(/[^a-z]/, ''))

        when:
        Optional<Account> refetchedAccount = accountRepository.findByAccountNameOwner(savedAccount.accountNameOwner)

        then:
        refetchedAccount.isPresent()
        refetchedAccount.get().cleared == new BigDecimal("1500.00")
        refetchedAccount.get().outstanding == new BigDecimal("50.00")
    }

    def "test account deletion"() {
        given:
        Account accountToDelete = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("deletetest")
                .asCredit()
                .withOutstanding(new BigDecimal("200.00"))
                .withMoniker("1200")
                .buildAndValidate()
        Account savedAccount = accountRepository.save(accountToDelete)

        when:
        accountRepository.delete(savedAccount)
        Optional<Account> deletedAccount = accountRepository.findByAccountNameOwner(savedAccount.accountNameOwner)

        then:
        !deletedAccount.isPresent()
    }

    def "test account constraint violations"() {
        given:
        Account duplicateAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("duplicatetest")
                .asDebit()
                .withCleared(new BigDecimal("100.00"))
                .withMoniker("8000")
                .buildAndValidate()
        Account savedAccount = accountRepository.save(duplicateAccount)

        // Manually create second account with same name to force constraint violation
        Account duplicateAccount2 = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("duplicatetest2")
                .asDebit()
                .withCleared(new BigDecimal("200.00"))
                .withMoniker("9000")
                .build()  // Use build() not buildAndValidate()
        // Override the account name to match the first one
        duplicateAccount2.accountNameOwner = savedAccount.accountNameOwner

        when:
        accountRepository.save(duplicateAccount2)
        accountRepository.flush()

        then:
        thrown(DataIntegrityViolationException)
    }

    def "test account repository custom queries"() {
        given:
        Account creditAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("credittest")
                .asCredit()
                .withOutstanding(new BigDecimal("100.00"))
                .withFuture(new BigDecimal("50.00"))
                .withMoniker("5000")
                .buildAndValidate()

        accountRepository.save(creditAccount)

        when:
        List<Account> accountsRequiringPayment = accountRepository.findAccountsThatRequirePayment(true, AccountType.Credit)

        then:
        accountsRequiringPayment.size() >= 1
        accountsRequiringPayment.any { it.accountNameOwner.contains(testOwner.replaceAll(/[^a-z]/, '')) }
        accountsRequiringPayment.every {
            it.accountType == AccountType.Credit &&
            (it.outstanding > BigDecimal.ZERO || it.future > BigDecimal.ZERO || it.cleared > BigDecimal.ZERO)
        }
    }

    def "test SmartBuilder constraint validation at build-time"() {
        when: "Creating account with valid name to show SmartBuilder works"
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("valid_account_name")  // Valid name
                .asDebit()
                .buildAndValidate()

        then: "SmartBuilder creates valid account successfully"
        account != null
        account.accountNameOwner.length() >= 3
        account.accountNameOwner.contains(testOwner.replaceAll(/[^a-z]/, ''))
    }

    def "test isolated test data with unique testOwner"() {
        given: "Multiple accounts with testOwner-based naming"
        Account account1 = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("isolation_one")
                .asDebit()
                .buildAndValidate()

        Account account2 = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("isolation_two")
                .asCredit()
                .buildAndValidate()

        when: "Saving accounts"
        Account saved1 = accountRepository.save(account1)
        Account saved2 = accountRepository.save(account2)

        then: "Both accounts have unique names containing testOwner"
        saved1.accountNameOwner != saved2.accountNameOwner
        saved1.accountNameOwner.contains(testOwner.replaceAll(/[^a-z]/, ''))
        saved2.accountNameOwner.contains(testOwner.replaceAll(/[^a-z]/, ''))

        and: "Account types are correctly set"
        saved1.accountType == AccountType.Debit
        saved2.accountType == AccountType.Credit
    }
}
