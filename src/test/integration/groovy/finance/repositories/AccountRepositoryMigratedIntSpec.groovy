package finance.repositories

import finance.BaseIntegrationSpec
import finance.domain.Account
import finance.domain.AccountType
import finance.helpers.SmartAccountBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Shared

import java.math.BigDecimal
import java.sql.Timestamp

/**
 * MIGRATED INTEGRATION TEST - Demonstrates robust, isolated architecture
 *
 * This is the migrated version of AccountRepositoryIntSpec showing:
 * ✅ No hardcoded account names - all use testOwner for uniqueness
 * ✅ SmartBuilder pattern with constraint validation
 * ✅ Test isolation - each test gets its own test data
 * ✅ Constraint-aware test data creation
 * ✅ Centralized test data management
 */
class AccountRepositoryMigratedIntSpec extends BaseIntegrationSpec {

    @Autowired
    AccountRepository accountRepository

    @Shared
    def repositoryContext

    def setupSpec() {
        repositoryContext = testFixtures.createRepositoryTestContext(testOwner)
    }

    void 'test account repository basic CRUD operations'() {
        given:
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("testsavings")
                .asDebit()
                .withCleared(new BigDecimal("1500.50"))
                .buildAndValidate()

        when:
        Account savedAccount = accountRepository.save(account)

        then:
        savedAccount.accountId != null
        savedAccount.accountNameOwner.contains(testOwner.replaceAll(/[^a-z]/, ''))
        savedAccount.accountNameOwner != "testsavings_brian"  // No hardcoded names
        savedAccount.accountType == AccountType.Debit
        savedAccount.cleared == new BigDecimal("1500.50")

        when:
        Optional<Account> foundAccount = accountRepository.findByAccountNameOwner(savedAccount.accountNameOwner)

        then:
        foundAccount.isPresent()
        foundAccount.get().accountType == AccountType.Debit
        foundAccount.get().cleared == new BigDecimal("1500.50")
    }

    void 'test find accounts by active status'() {
        given:
        Account activeAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("activeaccount")
                .asDebit()
                .asActive()
                .withCleared(new BigDecimal("2000.00"))
                .withMoniker("2000")
                .buildAndValidate()

        Account inactiveAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("inactiveaccount")
                .asCredit()
                .asInactive()
                .withCleared(new BigDecimal("-500.00"))
                .withMoniker("3000")
                .buildAndValidate()

        accountRepository.save(activeAccount)
        accountRepository.save(inactiveAccount)

        when:
        List<Account> activeAccounts = accountRepository.findByActiveStatus(true)
        List<Account> inactiveAccounts = accountRepository.findByActiveStatus(false)

        then:
        activeAccounts.size() >= 1
        activeAccounts.every { it.activeStatus == true }
        inactiveAccounts.size() >= 1
        inactiveAccounts.every { it.activeStatus == false }

        // Verify our test accounts are included
        activeAccounts.any { it.accountNameOwner == activeAccount.accountNameOwner }
        inactiveAccounts.any { it.accountNameOwner == inactiveAccount.accountNameOwner }
    }

    void 'test find accounts by account type'() {
        given:
        Account checkingAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("checkingtypetest")
                .asDebit()
                .withCleared(new BigDecimal("1000.00"))
                .withMoniker("4000")
                .buildAndValidate()

        Account savingsAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("savingstypetest")
                .asCredit()
                .withCleared(new BigDecimal("5000.00"))
                .withMoniker("5000")
                .buildAndValidate()

        accountRepository.save(checkingAccount)
        accountRepository.save(savingsAccount)

        when:
        List<Account> debitAccounts = accountRepository.findByAccountType(AccountType.Debit)
        List<Account> creditAccounts = accountRepository.findByAccountType(AccountType.Credit)

        then:
        debitAccounts.size() >= 1
        debitAccounts.every { it.accountType == AccountType.Debit }
        creditAccounts.size() >= 1
        creditAccounts.every { it.accountType == AccountType.Credit }

        // Verify our test accounts are included
        debitAccounts.any { it.accountNameOwner == checkingAccount.accountNameOwner }
        creditAccounts.any { it.accountNameOwner == savingsAccount.accountNameOwner }
    }

    void 'test find accounts by active status and account type'() {
        given:
        Account activeCheckingAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("activecheckingcombo")
                .asDebit()
                .asActive()
                .withCleared(new BigDecimal("1200.00"))
                .withMoniker("6000")
                .buildAndValidate()

        Account inactiveCheckingAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("inactivecheckingcombo")
                .asDebit()
                .asInactive()
                .withCleared(new BigDecimal("800.00"))
                .withMoniker("7000")
                .buildAndValidate()

        accountRepository.save(activeCheckingAccount)
        accountRepository.save(inactiveCheckingAccount)

        when:
        List<Account> activeCheckingAccounts = accountRepository
                .findByActiveStatusAndAccountType(true, AccountType.Debit)

        then:
        activeCheckingAccounts.size() >= 1
        activeCheckingAccounts.every {
            it.activeStatus == true && it.accountType == AccountType.Debit
        }
        activeCheckingAccounts.any { it.accountNameOwner == activeCheckingAccount.accountNameOwner }
        !activeCheckingAccounts.any { it.accountNameOwner == inactiveCheckingAccount.accountNameOwner }
    }

    void 'test account constraint violations'() {
        given:
        Account duplicateAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("duplicatetest")
                .asDebit()
                .withCleared(new BigDecimal("100.00"))
                .withMoniker("8000")
                .buildAndValidate()

        Account duplicateAccount2 = SmartAccountBuilder.builderForOwner(testOwner)
                .withAccountNameOwner(duplicateAccount.accountNameOwner)  // Same account name - will cause unique constraint violation
                .asCredit()  // Different account type but same name - violates unique constraint
                .withCleared(new BigDecimal("200.00"))
                .withMoniker("9000")
                .buildAndValidate()

        when:
        accountRepository.save(duplicateAccount)
        accountRepository.flush() // Force the first save to complete

        then:
        notThrown(Exception) // First save should succeed

        when:
        accountRepository.save(duplicateAccount2)
        accountRepository.flush() // This should fail due to unique constraint

        then:
        thrown(DataIntegrityViolationException)
    }

    void 'test account constraint validation at build time'() {
        when: "trying to create an account with invalid name length"
        SmartAccountBuilder.builderForOwner(testOwner)
                .withAccountNameOwner("ab")  // Too short - violates size constraint (min 3)
                .buildAndValidate()

        then: "constraint violation is caught at build time, not at save time"
        def ex = thrown(IllegalStateException)
        ex.message.contains("violates length constraints")

        when: "trying to create an account with invalid pattern"
        SmartAccountBuilder.builderForOwner(testOwner)
                .withAccountNameOwner("invalid pattern with spaces")  // Violates ALPHA_UNDERSCORE_PATTERN
                .buildAndValidate()

        then: "constraint violation is caught at build time"
        def ex2 = thrown(IllegalStateException)
        ex2.message.contains("violates alpha_underscore pattern")

        when: "trying to create an account with invalid moniker"
        SmartAccountBuilder.builderForOwner(testOwner)
                .withMoniker("123")  // Too short - must be exactly 4 digits
                .buildAndValidate()

        then: "constraint violation is caught at build time"
        def ex3 = thrown(IllegalStateException)
        ex3.message.contains("must be exactly 4 digits")
    }

    void 'test account update operations'() {
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
        savedAccount.dateUpdated = new Timestamp(System.currentTimeMillis())
        Account updatedAccount = accountRepository.save(savedAccount)

        then:
        updatedAccount.cleared == new BigDecimal("1500.00")
        updatedAccount.outstanding == new BigDecimal("50.00")
        updatedAccount.accountNameOwner == savedAccount.accountNameOwner

        when:
        Optional<Account> refetchedAccount = accountRepository.findByAccountNameOwner(savedAccount.accountNameOwner)

        then:
        refetchedAccount.isPresent()
        refetchedAccount.get().cleared == new BigDecimal("1500.00")
        refetchedAccount.get().outstanding == new BigDecimal("50.00")
    }

    void 'test account deletion'() {
        given:
        Account accountToDelete = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("deletetest")
                .asCredit()
                .withCleared(new BigDecimal("-200.00"))
                .withMoniker("1200")
                .buildAndValidate()
        Account savedAccount = accountRepository.save(accountToDelete)

        when:
        accountRepository.delete(savedAccount)
        Optional<Account> deletedAccount = accountRepository.findByAccountNameOwner(savedAccount.accountNameOwner)

        then:
        !deletedAccount.isPresent()
    }

    void 'test repository context helper methods'() {
        given:
        Long testAccountId = repositoryContext.createTestAccount("helper", AccountType.Debit)
        Account uniqueAccount = repositoryContext.createUniqueAccount("unique")

        when:
        Optional<Account> foundAccount = accountRepository.findById(testAccountId)
        Account savedUniqueAccount = accountRepository.save(uniqueAccount)

        then:
        foundAccount.isPresent()
        foundAccount.get().accountNameOwner.contains(testOwner.replaceAll(/[^a-z]/, ''))

        savedUniqueAccount.accountId != null
        savedUniqueAccount.accountNameOwner.contains("unique")
        savedUniqueAccount.accountNameOwner.contains(testOwner.replaceAll(/[^a-z]/, ''))
    }

    void 'test smart builder convenience methods'() {
        given:
        Account checkingAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("convenience")
                .asDebit()
                .asActive()
                .withBalances(
                    new BigDecimal("1000.00"),  // cleared
                    new BigDecimal("100.00"),   // outstanding
                    new BigDecimal("50.00")     // future
                )
                .buildAndValidate()

        when:
        Account savedAccount = accountRepository.save(checkingAccount)

        then:
        savedAccount.accountType == AccountType.Debit
        savedAccount.activeStatus == true
        savedAccount.cleared == new BigDecimal("1000.00")
        savedAccount.outstanding == new BigDecimal("100.00")
        savedAccount.future == new BigDecimal("50.00")

        // Account name follows pattern and contains test owner
        savedAccount.accountNameOwner.matches(/^[a-z-]*_[a-z]*$/)
        savedAccount.accountNameOwner.contains(testOwner.replaceAll(/[^a-z]/, ''))
    }
}