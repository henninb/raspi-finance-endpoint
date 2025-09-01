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
import spock.lang.Ignore

import java.sql.Date
import java.sql.Timestamp
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
            accountNameOwner: "testsavings_brian",
            accountType: AccountType.Debit,
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
        savedAccount.accountNameOwner == "testsavings_brian"
        savedAccount.accountType == AccountType.Debit
        savedAccount.cleared == new BigDecimal("1500.50")

        when:
        Optional<Account> foundAccount = accountRepository.findByAccountNameOwner("testsavings_brian")

        then:
        foundAccount.isPresent()
        foundAccount.get().accountType == AccountType.Debit
        foundAccount.get().cleared == new BigDecimal("1500.50")
    }

    void 'test find accounts by active status'() {
        given:
        // Create active account
        Account activeAccount = new Account(
            accountId: 0L,
            accountNameOwner: "activeaccount_brian",
            accountType: AccountType.Debit,
            activeStatus: true,
            moniker: "2000",
            outstanding: new BigDecimal("0.00"),
            future: new BigDecimal("0.00"),
            cleared: new BigDecimal("2000.00"),
            dateClosed: new Timestamp(System.currentTimeMillis()),
            validationDate: new Timestamp(System.currentTimeMillis())
        )

        // Create inactive account
        Account inactiveAccount = new Account(
            accountId: 0L,
            accountNameOwner: "inactiveaccount_brian",
            accountType: AccountType.Credit,
            activeStatus: false,
            moniker: "3000",
            outstanding: new BigDecimal("0.00"),
            future: new BigDecimal("0.00"),
            cleared: new BigDecimal("-500.00"),
            dateClosed: new Timestamp(System.currentTimeMillis()),
            validationDate: new Timestamp(System.currentTimeMillis())
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
            accountId: 0L,
            accountNameOwner: "checkingtypetest_brian",
            accountType: AccountType.Debit,
            activeStatus: true,
            moniker: "4000",
            outstanding: new BigDecimal("0.00"),
            future: new BigDecimal("0.00"),
            cleared: new BigDecimal("1000.00"),
            dateClosed: new Timestamp(System.currentTimeMillis()),
            validationDate: new Timestamp(System.currentTimeMillis())
        )

        Account savingsAccount = new Account(
            accountId: 0L,
            accountNameOwner: "savingstypetest_brian",
            accountType: AccountType.Credit,
            activeStatus: true,
            moniker: "5000",
            outstanding: new BigDecimal("0.00"),
            future: new BigDecimal("0.00"),
            cleared: new BigDecimal("5000.00"),
            dateClosed: new Timestamp(System.currentTimeMillis()),
            validationDate: new Timestamp(System.currentTimeMillis())
        )

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
    }

    void 'test find accounts by active status and account type'() {
        given:
        Account activeCheckingAccount = new Account(
            accountId: 0L,
            accountNameOwner: "activecheckingcombo_brian",
            accountType: AccountType.Debit,
            activeStatus: true,
            moniker: "6000",
            outstanding: new BigDecimal("0.00"),
            future: new BigDecimal("0.00"),
            cleared: new BigDecimal("1200.00"),
            dateClosed: new Timestamp(System.currentTimeMillis()),
            validationDate: new Timestamp(System.currentTimeMillis())
        )

        Account inactiveCheckingAccount = new Account(
            accountId: 0L,
            accountNameOwner: "inactivecheckingcombo_brian",
            accountType: AccountType.Debit,
            activeStatus: false,
            moniker: "7000",
            outstanding: new BigDecimal("0.00"),
            future: new BigDecimal("0.00"),
            cleared: new BigDecimal("800.00"),
            dateClosed: new Timestamp(System.currentTimeMillis()),
            validationDate: new Timestamp(System.currentTimeMillis())
        )

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
        activeCheckingAccounts.any { it.accountNameOwner == "activecheckingcombo_brian" }
        !activeCheckingAccounts.any { it.accountNameOwner == "inactivecheckingcombo_brian" }
    }

    void 'test account constraint violations'() {
        given:
        Account duplicateAccount = new Account(
            accountId: 0L,
            accountNameOwner: "duplicatetest_brian",
            accountType: AccountType.Debit,
            activeStatus: true,
            moniker: "8000",
            outstanding: new BigDecimal("0.00"),
            future: new BigDecimal("0.00"),
            cleared: new BigDecimal("100.00"),
            dateClosed: new Timestamp(System.currentTimeMillis()),
            validationDate: new Timestamp(System.currentTimeMillis())
        )

        Account duplicateAccount2 = new Account(
            accountId: 0L,
            accountNameOwner: "duplicatetest_brian",  // Same account name - will cause unique constraint violation
            accountType: AccountType.Credit,  // Different account type but same name - violates unique constraint
            activeStatus: true,
            moniker: "9000",
            outstanding: new BigDecimal("0.00"),
            future: new BigDecimal("0.00"),
            cleared: new BigDecimal("200.00"),
            dateClosed: new Timestamp(System.currentTimeMillis()),
            validationDate: new Timestamp(System.currentTimeMillis())
        )

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

    void 'test account null constraint violations'() {
        given:
        // Test with invalid account name that violates pattern constraints
        Account invalidAccount = new Account(
            accountId: 0L,
            accountNameOwner: "ab",  // Too short - violates size constraint (min 3)
            accountType: AccountType.Debit,
            activeStatus: true,
            moniker: "1000",
            outstanding: new BigDecimal("0.00"),
            future: new BigDecimal("0.00"),
            cleared: new BigDecimal("100.00"),
            dateClosed: new Timestamp(System.currentTimeMillis()),
            validationDate: new Timestamp(System.currentTimeMillis())
        )

        when:
        accountRepository.save(invalidAccount)
        accountRepository.flush()

        then:
        thrown(Exception)
    }

    void 'test account update operations'() {
        given:
        Account account = new Account(
            accountId: 0L,
            accountNameOwner: "updatetest_brian",
            accountType: AccountType.Debit,
            activeStatus: true,
            moniker: "1100",
            outstanding: new BigDecimal("0.00"),
            future: new BigDecimal("0.00"),
            cleared: new BigDecimal("1000.00"),
            dateClosed: new Timestamp(System.currentTimeMillis()),
            validationDate: new Timestamp(System.currentTimeMillis())
        )
        Account savedAccount = accountRepository.save(account)

        when:
        savedAccount.cleared = new BigDecimal("1500.00")
        savedAccount.outstanding = new BigDecimal("50.00")
        savedAccount.dateUpdated = new Timestamp(System.currentTimeMillis())
        Account updatedAccount = accountRepository.save(savedAccount)

        then:
        updatedAccount.cleared == new BigDecimal("1500.00")
        updatedAccount.outstanding == new BigDecimal("50.00")
        updatedAccount.accountNameOwner == "updatetest_brian"

        when:
        Optional<Account> refetchedAccount = accountRepository.findByAccountNameOwner("updatetest_brian")

        then:
        refetchedAccount.isPresent()
        refetchedAccount.get().cleared == new BigDecimal("1500.00")
        refetchedAccount.get().outstanding == new BigDecimal("50.00")
    }

    void 'test account deletion'() {
        given:
        Account accountToDelete = new Account(
            accountId: 0L,
            accountNameOwner: "deletetest_brian",
            accountType: AccountType.Credit,
            activeStatus: true,
            moniker: "1200",
            outstanding: new BigDecimal("0.00"),
            future: new BigDecimal("0.00"),
            cleared: new BigDecimal("-200.00"),
            dateClosed: new Timestamp(System.currentTimeMillis()),
            validationDate: new Timestamp(System.currentTimeMillis())
        )
        Account savedAccount = accountRepository.save(accountToDelete)

        when:
        accountRepository.delete(savedAccount)
        Optional<Account> deletedAccount = accountRepository.findByAccountNameOwner("deletetest_brian")

        then:
        !deletedAccount.isPresent()
    }

}
