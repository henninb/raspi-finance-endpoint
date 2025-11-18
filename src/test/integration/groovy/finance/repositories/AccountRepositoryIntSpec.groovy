package finance.repositories

import finance.BaseIntegrationSpec
import finance.domain.Account
import finance.domain.AccountType
import finance.helpers.SmartAccountBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import jakarta.validation.ConstraintViolationException

import java.sql.Timestamp
import java.math.BigDecimal

class AccountRepositoryIntSpec extends BaseIntegrationSpec {

    @Autowired
    AccountRepository accountRepository

    void 'test account repository basic CRUD operations'() {
        given:
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName("testsavings")
            .asDebit()
            .withMoniker("1000")
            .withBalances(new BigDecimal("1500.50"))
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

    void 'test find accounts by active status'() {
        given:
        Account activeAccount = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName("active")
            .asDebit()
            .asActive()
            .withMoniker("2000")
            .withBalances(new BigDecimal("2000.00"))
            .buildAndValidate()

        Account inactiveAccount = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName("inactive")
            .asCredit()
            .asInactive()
            .withMoniker("3000")
            .withBalances(new BigDecimal("-500.00"))
            .buildAndValidate()

        accountRepository.save(activeAccount)
        accountRepository.save(inactiveAccount)

        when:
        List<Account> activeAccounts = accountRepository.findByActiveStatus(true)
        List<Account> inactiveAccounts = accountRepository.findByActiveStatus(false)

        then:
        activeAccounts.size() >= 1
        activeAccounts.every { it.activeStatus == true }
        activeAccounts.any { it.accountNameOwner == activeAccount.accountNameOwner }

        inactiveAccounts.size() >= 1
        inactiveAccounts.every { it.activeStatus == false }
        inactiveAccounts.any { it.accountNameOwner == inactiveAccount.accountNameOwner }
    }

    void 'test find accounts by account type'() {
        given:
        Account checkingAccount = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName("checking")
            .asDebit()
            .withMoniker("4000")
            .withBalances(new BigDecimal("1000.00"))
            .buildAndValidate()

        Account savingsAccount = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName("savings")
            .asCredit()
            .withMoniker("5000")
            .withBalances(new BigDecimal("5000.00"))
            .buildAndValidate()

        accountRepository.save(checkingAccount)
        accountRepository.save(savingsAccount)

        when:
        List<Account> debitAccounts = accountRepository.findByAccountType(AccountType.Debit)
        List<Account> creditAccounts = accountRepository.findByAccountType(AccountType.Credit)

        then:
        debitAccounts.size() >= 1
        debitAccounts.every { it.accountType == AccountType.Debit }
        debitAccounts.any { it.accountNameOwner == checkingAccount.accountNameOwner }

        creditAccounts.size() >= 1
        creditAccounts.every { it.accountType == AccountType.Credit }
        creditAccounts.any { it.accountNameOwner == savingsAccount.accountNameOwner }
    }

    void 'test find accounts by active status and account type'() {
        given:
        Account activeCheckingAccount = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName("activechecking")
            .asDebit()
            .asActive()
            .withMoniker("6000")
            .withBalances(new BigDecimal("1200.00"))
            .buildAndValidate()

        Account inactiveCheckingAccount = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName("inactivechecking")
            .asDebit()
            .asInactive()
            .withMoniker("7000")
            .withBalances(new BigDecimal("800.00"))
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
        // Create first account with unique name for this test
        Account firstAccount = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName("duplicate")
            .asDebit()
            .withMoniker("8000")
            .withBalances(new BigDecimal("100.00"))
            .buildAndValidate()

        when:
        Account savedAccount = accountRepository.save(firstAccount)
        accountRepository.flush()

        then:
        notThrown(Exception)
        savedAccount.accountId != null

        when:
        // Try to create a second account with the exact same account name
        Account duplicateAccount = SmartAccountBuilder.builderForOwner(testOwner)
            .withAccountNameOwner(savedAccount.accountNameOwner)  // Use exact same name
            .asCredit()  // Different type but same name violates unique constraint
            .withMoniker("9000")
            .withBalances(new BigDecimal("200.00"))
            .buildAndValidate()

        accountRepository.save(duplicateAccount)
        accountRepository.flush()

        then:
        thrown(DataIntegrityViolationException)
    }

    void 'test account constraint validation during build'() {
        when:
        // SmartBuilder should catch constraint violations at build time
        SmartAccountBuilder.builderForOwner(testOwner)
            .withAccountNameOwner("ab")  // Too short - violates size constraint (min 3)
            .asDebit()
            .withMoniker("1000")
            .withBalances(new BigDecimal("100.00"))
            .buildAndValidate()  // This should fail during validation

        then:
        thrown(IllegalStateException)

        when:
        // Test invalid pattern - numbers not allowed
        SmartAccountBuilder.builderForOwner(testOwner)
            .withAccountNameOwner("invalid123_test")  // Numbers violate alpha_underscore pattern
            .asDebit()
            .withMoniker("1000")
            .withBalances(new BigDecimal("100.00"))
            .buildAndValidate()

        then:
        thrown(IllegalStateException)
    }

    void 'test account update operations'() {
        given:
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName("update")
            .asDebit()
            .withMoniker("1100")
            .withBalances(new BigDecimal("1000.00"))
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
            .withUniqueAccountName("delete")
            .asCredit()
            .withMoniker("1200")
            .withBalances(new BigDecimal("-200.00"))
            .buildAndValidate()

        Account savedAccount = accountRepository.save(accountToDelete)

        when:
        accountRepository.delete(savedAccount)
        Optional<Account> deletedAccount = accountRepository.findByAccountNameOwner(savedAccount.accountNameOwner)

        then:
        !deletedAccount.isPresent()
    }

    void 'test invalid moniker is rejected on save'() {
        given:
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName("invalidmoniker")
            .asDebit()
            .withMoniker("1000")
            .withBalances(new BigDecimal("100.00"))
            .buildAndValidate()
        // Bypass builder validation to simulate bad input
        account.moniker = "invalid" // violates numeric/pattern constraints

        when:
        accountRepository.save(account)
        accountRepository.flush()

        then:
        thrown(ConstraintViolationException)
    }

    void 'test invalid accountNameOwner pattern is rejected on save'() {
        given:
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName("badname")
            .asDebit()
            .withMoniker("1000")
            .withBalances(new BigDecimal("100.00"))
            .buildAndValidate()
        // Bypass builder and set an invalid name that violates pattern/length
        account.accountNameOwner = "invalid" // too short or wrong pattern

        when:
        accountRepository.save(account)
        accountRepository.flush()

        then:
        thrown(ConstraintViolationException)
    }

    void 'test account repository custom queries'() {
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

}
