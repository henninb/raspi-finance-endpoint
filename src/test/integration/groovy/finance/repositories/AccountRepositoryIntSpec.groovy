package finance.repositories

import finance.Application
import finance.domain.Account
import finance.domain.AccountType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

import java.sql.Date
import java.math.BigDecimal

@ActiveProfiles("int")
@SpringBootTest
@ContextConfiguration(classes = Application)
@Transactional
class AccountRepositoryIntSpec extends Specification {

    @Autowired
    AccountRepository accountRepository

    void 'test account repository basic CRUD operations'() {
        given:
        Account account = new Account(
            accountId: 0L,
            accountNameOwner: "test-savings_brian",
            accountType: AccountType.Savings,
            activeStatus: true,
            moniker: "1000",
            outstanding: new BigDecimal("0.00"),
            future: new BigDecimal("0.00"),
            cleared: new BigDecimal("1500.50"),
            dateClosed: new java.sql.Timestamp(System.currentTimeMillis()),
            validationDate: new java.sql.Timestamp(System.currentTimeMillis())
        )

        when:
        Account savedAccount = accountRepository.save(account)

        then:
        savedAccount.accountId != null
        savedAccount.accountNameOwner == "test-savings_brian"
        savedAccount.accountType == AccountType.Savings
        savedAccount.cleared == new BigDecimal("1500.50")

        when:
        Optional<Account> foundAccount = accountRepository.findByAccountNameOwner("test-savings_brian")

        then:
        foundAccount.isPresent()
        foundAccount.get().accountType == AccountType.Savings
        foundAccount.get().cleared == new BigDecimal("1500.50")
    }

    void 'test find accounts by active status'() {
        given:
        // Create active account
        Account activeAccount = new Account(
            accountNameOwner: "active_account_brian",
            accountType: AccountType.Checking,
            activeStatus: true,
            moniker: 2000L,
            totals: 2000.00,
            totalsBalanced: 2000.00,
            dateUpdated: new Date(System.currentTimeMillis()),
            dateAdded: new Date(System.currentTimeMillis())
        )

        // Create inactive account
        Account inactiveAccount = new Account(
            accountNameOwner: "inactive_account_brian",
            accountType: AccountType.Credit,
            activeStatus: false,
            moniker: 3000L,
            totals: -500.00,
            totalsBalanced: -500.00,
            dateUpdated: new Date(System.currentTimeMillis()),
            dateAdded: new Date(System.currentTimeMillis())
        )

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
    }

    void 'test find accounts by account type'() {
        given:
        Account checkingAccount = new Account(
            accountNameOwner: "checking_type_test_brian",
            accountType: AccountType.Checking,
            activeStatus: true,
            moniker: 4000L,
            totals: 1000.00,
            totalsBalanced: 1000.00,
            dateUpdated: new Date(System.currentTimeMillis()),
            dateAdded: new Date(System.currentTimeMillis())
        )

        Account savingsAccount = new Account(
            accountNameOwner: "savings-type-test_brian",
            accountType: AccountType.Savings,
            activeStatus: true,
            moniker: 5000L,
            totals: 5000.00,
            totalsBalanced: 5000.00,
            dateUpdated: new Date(System.currentTimeMillis()),
            dateAdded: new Date(System.currentTimeMillis())
        )

        accountRepository.save(checkingAccount)
        accountRepository.save(savingsAccount)

        when:
        List<Account> checkingAccounts = accountRepository.findByAccountType(AccountType.Checking)
        List<Account> savingsAccounts = accountRepository.findByAccountType(AccountType.Savings)

        then:
        checkingAccounts.size() >= 1
        checkingAccounts.every { it.accountType == AccountType.Checking }
        savingsAccounts.size() >= 1
        savingsAccounts.every { it.accountType == AccountType.Savings }
    }

    void 'test find accounts by active status and account type'() {
        given:
        Account activeCheckingAccount = new Account(
            accountNameOwner: "active-checking-combo_brian",
            accountType: AccountType.Checking,
            activeStatus: true,
            moniker: 6000L,
            totals: 1200.00,
            totalsBalanced: 1200.00,
            dateUpdated: new Date(System.currentTimeMillis()),
            dateAdded: new Date(System.currentTimeMillis())
        )

        Account inactiveCheckingAccount = new Account(
            accountNameOwner: "inactive-checking-combo_brian",
            accountType: AccountType.Checking,
            activeStatus: false,
            moniker: 7000L,
            totals: 800.00,
            totalsBalanced: 800.00,
            dateUpdated: new Date(System.currentTimeMillis()),
            dateAdded: new Date(System.currentTimeMillis())
        )

        accountRepository.save(activeCheckingAccount)
        accountRepository.save(inactiveCheckingAccount)

        when:
        List<Account> activeCheckingAccounts = accountRepository
            .findByActiveStatusAndAccountType(true, AccountType.Checking)

        then:
        activeCheckingAccounts.size() >= 1
        activeCheckingAccounts.every {
            it.activeStatus == true && it.accountType == AccountType.Checking
        }
        activeCheckingAccounts.any { it.accountNameOwner == "active-checking-combo_brian" }
        !activeCheckingAccounts.any { it.accountNameOwner == "inactive-checking-combo_brian" }
    }

    void 'test account constraint violations'() {
        given:
        Account duplicateAccount = new Account(
            accountNameOwner: "duplicate-test_brian",
            accountType: AccountType.Checking,
            activeStatus: true,
            moniker: 8000L,
            totals: 100.00,
            totalsBalanced: 100.00,
            dateUpdated: new Date(System.currentTimeMillis()),
            dateAdded: new Date(System.currentTimeMillis())
        )

        Account duplicateAccount2 = new Account(
            accountNameOwner: "duplicate-test_brian",  // Same account name
            accountType: AccountType.Savings,
            activeStatus: true,
            moniker: 9000L,
            totals: 200.00,
            totalsBalanced: 200.00,
            dateUpdated: new Date(System.currentTimeMillis()),
            dateAdded: new Date(System.currentTimeMillis())
        )

        when:
        accountRepository.save(duplicateAccount)
        accountRepository.save(duplicateAccount2)
        accountRepository.flush()

        then:
        thrown(DataIntegrityViolationException)
    }

    void 'test account null constraint violations'() {
        given:
        Account invalidAccount = new Account(
            accountNameOwner: null,  // This should cause constraint violation
            accountType: AccountType.Checking,
            activeStatus: true,
            moniker: 10000L,
            totals: 100.00,
            totalsBalanced: 100.00,
            dateUpdated: new Date(System.currentTimeMillis()),
            dateAdded: new Date(System.currentTimeMillis())
        )

        when:
        accountRepository.save(invalidAccount)
        accountRepository.flush()

        then:
        thrown(DataIntegrityViolationException)
    }

    void 'test account update operations'() {
        given:
        Account account = new Account(
            accountNameOwner: "update-test_brian",
            accountType: AccountType.Checking,
            activeStatus: true,
            moniker: 11000L,
            totals: 1000.00,
            totalsBalanced: 1000.00,
            dateUpdated: new Date(System.currentTimeMillis()),
            dateAdded: new Date(System.currentTimeMillis())
        )
        Account savedAccount = accountRepository.save(account)

        when:
        savedAccount.totals = 1500.00
        savedAccount.totalsBalanced = 1450.00
        savedAccount.dateUpdated = new Date(System.currentTimeMillis())
        Account updatedAccount = accountRepository.save(savedAccount)

        then:
        updatedAccount.totals == 1500.00
        updatedAccount.totalsBalanced == 1450.00
        updatedAccount.accountNameOwner == "update-test_brian"

        when:
        Optional<Account> refetchedAccount = accountRepository.findByAccountNameOwner("update-test_brian")

        then:
        refetchedAccount.isPresent()
        refetchedAccount.get().totals == 1500.00
        refetchedAccount.get().totalsBalanced == 1450.00
    }

    void 'test account deletion'() {
        given:
        Account accountToDelete = new Account(
            accountNameOwner: "delete-test_brian",
            accountType: AccountType.Credit,
            activeStatus: true,
            moniker: 12000L,
            totals: -200.00,
            totalsBalanced: -200.00,
            dateUpdated: new Date(System.currentTimeMillis()),
            dateAdded: new Date(System.currentTimeMillis())
        )
        Account savedAccount = accountRepository.save(accountToDelete)

        when:
        accountRepository.delete(savedAccount)
        Optional<Account> deletedAccount = accountRepository.findByAccountNameOwner("delete-test_brian")

        then:
        !deletedAccount.isPresent()
    }

    void 'test account query performance'() {
        given:
        // Create multiple accounts to test query performance
        List<Account> accounts = []
        for (int i = 0; i < 50; i++) {
            Account account = new Account(
                accountNameOwner: "performance-test_${i}_brian",
                accountType: i % 2 == 0 ? AccountType.Checking : AccountType.Savings,
                activeStatus: i % 3 != 0,
                moniker: 13000L + i,
                totals: Math.random() * 10000,
                totalsBalanced: Math.random() * 10000,
                dateUpdated: new Date(System.currentTimeMillis()),
                dateAdded: new Date(System.currentTimeMillis())
            )
            accounts.add(account)
        }
        accountRepository.saveAll(accounts)

        when:
        long startTime = System.currentTimeMillis()
        List<Account> activeAccounts = accountRepository.findByActiveStatus(true)
        List<Account> checkingAccounts = accountRepository.findByAccountType(AccountType.Checking)
        long endTime = System.currentTimeMillis()

        then:
        activeAccounts.size() >= 33  // Approximately 2/3 of 50 accounts
        checkingAccounts.size() >= 25  // Approximately half of 50 accounts
        (endTime - startTime) < 3000  // Queries should complete within 3 seconds
    }
}
