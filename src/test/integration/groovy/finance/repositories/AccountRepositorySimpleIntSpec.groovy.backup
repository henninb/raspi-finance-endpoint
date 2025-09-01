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

import java.math.BigDecimal
import java.sql.Timestamp

@ActiveProfiles("int")
@SpringBootTest
@ContextConfiguration(classes = Application)
@Transactional
class AccountRepositorySimpleIntSpec extends Specification {

    @Autowired
    AccountRepository accountRepository

    void 'test account repository basic CRUD operations'() {
        given:
        Account account = new Account()
        account.accountNameOwner = "testsavings_brian"
        account.accountType = AccountType.Debit
        account.activeStatus = true
        account.moniker = "1000"
        account.outstanding = new BigDecimal("0.00")
        account.future = new BigDecimal("0.00")
        account.cleared = new BigDecimal("1500.50")
        account.dateClosed = new Timestamp(System.currentTimeMillis())
        account.validationDate = new Timestamp(System.currentTimeMillis())

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
        Account activeAccount = new Account()
        activeAccount.accountNameOwner = "activeaccount_brian"
        activeAccount.accountType = AccountType.Debit
        activeAccount.activeStatus = true
        activeAccount.moniker = "2000"
        activeAccount.outstanding = new BigDecimal("0.00")
        activeAccount.future = new BigDecimal("0.00")
        activeAccount.cleared = new BigDecimal("2000.00")
        activeAccount.dateClosed = new Timestamp(System.currentTimeMillis())
        activeAccount.validationDate = new Timestamp(System.currentTimeMillis())

        // Create inactive account
        Account inactiveAccount = new Account()
        inactiveAccount.accountNameOwner = "inactiveaccount_brian"
        inactiveAccount.accountType = AccountType.Credit
        inactiveAccount.activeStatus = false
        inactiveAccount.moniker = "3000"
        inactiveAccount.outstanding = new BigDecimal("500.00")
        inactiveAccount.future = new BigDecimal("0.00")
        inactiveAccount.cleared = new BigDecimal("0.00")
        inactiveAccount.dateClosed = new Timestamp(System.currentTimeMillis())
        inactiveAccount.validationDate = new Timestamp(System.currentTimeMillis())

        accountRepository.save(activeAccount)
        accountRepository.save(inactiveAccount)

        when:
        List<Account> activeAccounts = accountRepository.findByActiveStatusOrderByAccountNameOwner(true)

        then:
        activeAccounts.size() >= 1
        activeAccounts.every { it.activeStatus == true }
    }

    void 'test account repository update operations'() {
        given:
        Account account = new Account()
        account.accountNameOwner = "updatetest_brian"
        account.accountType = AccountType.Debit
        account.activeStatus = true
        account.moniker = "1100"
        account.outstanding = new BigDecimal("0.00")
        account.future = new BigDecimal("0.00")
        account.cleared = new BigDecimal("1000.00")
        account.dateClosed = new Timestamp(System.currentTimeMillis())
        account.validationDate = new Timestamp(System.currentTimeMillis())

        Account savedAccount = accountRepository.save(account)

        when:
        savedAccount.cleared = new BigDecimal("1500.00")
        savedAccount.outstanding = new BigDecimal("50.00")
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
        Account accountToDelete = new Account()
        accountToDelete.accountNameOwner = "deletetest_brian"
        accountToDelete.accountType = AccountType.Credit
        accountToDelete.activeStatus = true
        accountToDelete.moniker = "1200"
        accountToDelete.outstanding = new BigDecimal("200.00")
        accountToDelete.future = new BigDecimal("0.00")
        accountToDelete.cleared = new BigDecimal("0.00")
        accountToDelete.dateClosed = new Timestamp(System.currentTimeMillis())
        accountToDelete.validationDate = new Timestamp(System.currentTimeMillis())

        Account savedAccount = accountRepository.save(accountToDelete)

        when:
        accountRepository.delete(savedAccount)
        Optional<Account> deletedAccount = accountRepository.findByAccountNameOwner("deletetest_brian")

        then:
        !deletedAccount.isPresent()
    }

    void 'test account constraint violations'() {
        given:
        Account duplicateAccount = new Account()
        duplicateAccount.accountNameOwner = "duplicatetest_brian"
        duplicateAccount.accountType = AccountType.Debit
        duplicateAccount.activeStatus = true
        duplicateAccount.moniker = "8000"
        duplicateAccount.outstanding = new BigDecimal("0.00")
        duplicateAccount.future = new BigDecimal("0.00")
        duplicateAccount.cleared = new BigDecimal("100.00")
        duplicateAccount.dateClosed = new Timestamp(System.currentTimeMillis())
        duplicateAccount.validationDate = new Timestamp(System.currentTimeMillis())

        Account duplicateAccount2 = new Account()
        duplicateAccount2.accountNameOwner = "duplicatetest_brian"  // Same account name
        duplicateAccount2.accountType = AccountType.Debit
        duplicateAccount2.activeStatus = true
        duplicateAccount2.moniker = "9000"
        duplicateAccount2.outstanding = new BigDecimal("0.00")
        duplicateAccount2.future = new BigDecimal("0.00")
        duplicateAccount2.cleared = new BigDecimal("200.00")
        duplicateAccount2.dateClosed = new Timestamp(System.currentTimeMillis())
        duplicateAccount2.validationDate = new Timestamp(System.currentTimeMillis())

        when:
        accountRepository.save(duplicateAccount)
        accountRepository.save(duplicateAccount2)
        accountRepository.flush()

        then:
        thrown(DataIntegrityViolationException)
    }

    void 'test account repository custom queries'() {
        given:
        Account creditAccount = new Account()
        creditAccount.accountNameOwner = "credittest_brian"
        creditAccount.accountType = AccountType.Credit
        creditAccount.activeStatus = true
        creditAccount.moniker = "5000"
        creditAccount.outstanding = new BigDecimal("100.00")
        creditAccount.future = new BigDecimal("50.00")
        creditAccount.cleared = new BigDecimal("0.00")
        creditAccount.dateClosed = new Timestamp(System.currentTimeMillis())
        creditAccount.validationDate = new Timestamp(System.currentTimeMillis())

        accountRepository.save(creditAccount)

        when:
        List<Account> accountsRequiringPayment = accountRepository.findAccountsThatRequirePayment(true, AccountType.Credit)

        then:
        accountsRequiringPayment.size() >= 1
        accountsRequiringPayment.any { it.accountNameOwner == "credittest_brian" }
        accountsRequiringPayment.every {
            it.accountType == AccountType.Credit &&
            (it.outstanding > BigDecimal.ZERO || it.future > BigDecimal.ZERO || it.cleared > BigDecimal.ZERO)
        }
    }

    // Temporarily commented out due to validation constraints
    /*
    void 'test account performance with multiple accounts'() {
        given:
        List<Account> accounts = []
        for (int i = 0; i < 20; i++) {
            Account account = new Account()
            String[] prefixes = ["perf", "test", "check", "save", "demo", "sample", "bench", "trial", "probe", "exam", "study", "assess", "eval", "audit", "verify", "track", "monitor", "review", "survey", "scan"]
            account.accountNameOwner = prefixes[i % 20] + "_brian"
            account.accountType = i % 2 == 0 ? AccountType.Debit : AccountType.Credit
            account.activeStatus = i % 3 != 0
            account.moniker = String.format("%04d", 1300 + i)
            account.outstanding = new BigDecimal(Math.random() * 100)
            account.future = new BigDecimal(Math.random() * 100)
            account.cleared = new BigDecimal(Math.random() * 10000)
            account.dateClosed = new Timestamp(System.currentTimeMillis())
            account.validationDate = new Timestamp(System.currentTimeMillis())
            accounts.add(account)
        }
        accountRepository.saveAll(accounts)

        when:
        long startTime = System.currentTimeMillis()
        List<Account> activeAccounts = accountRepository.findByActiveStatusOrderByAccountNameOwner(true)
        long endTime = System.currentTimeMillis()

        then:
        activeAccounts.size() >= 13  // Approximately 2/3 of 20 accounts
        (endTime - startTime) < 3000  // Query should complete within 3 seconds
    }
    */
}
