package finance.repositories

import finance.BaseIntegrationSpec
import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Transaction
import finance.domain.TransactionType
import finance.helpers.SmartAccountBuilder
import finance.helpers.SmartTransactionBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import spock.lang.Shared

import java.time.LocalDate
import java.util.Optional

/**
 * Integration spec for date range queries across all accounts using pagination.
 */
class TransactionRepositoryDateRangeIntSpec extends BaseIntegrationSpec {

    @Autowired
    TransactionRepository transactionRepository

    @Shared
    @Autowired
    AccountRepository accountRepository

    @Shared
    Long primaryAccountId

    @Shared
    Long secondaryAccountId

    @Shared
    String primaryName

    @Shared
    String secondaryName

    def setupSpec() {
        // Derive clean owner and deterministic account names (avoid getter name conflicts)
        String cleanOwner = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        if (cleanOwner.isEmpty()) cleanOwner = 'testowner'
        primaryName = "primary_${cleanOwner}".toLowerCase()
        secondaryName = "secondary_${cleanOwner}".toLowerCase()

        // Ensure two accounts exist for the test owner
        def primary = SmartAccountBuilder.builderForOwner(testOwner)
                .withAccountNameOwner(primaryName)
                .asDebit()
                .buildAndValidate()
        def secondary = SmartAccountBuilder.builderForOwner(testOwner)
                .withAccountNameOwner(secondaryName)
                .asDebit()
                .buildAndValidate()

        Optional<Account> p = accountRepository.findByOwnerAndAccountNameOwner(testOwner,primaryName)
        if (!p.isPresent()) {
            p = Optional.of(accountRepository.save(primary))
        }
        primaryAccountId = p.get().accountId

        Optional<Account> s = accountRepository.findByOwnerAndAccountNameOwner(testOwner,secondaryName)
        if (!s.isPresent()) {
            s = Optional.of(accountRepository.save(secondary))
        }
        secondaryAccountId = s.get().accountId
    }

    void 'find transactions by transactionDate between with pagination (across accounts)'() {
        given: 'A set of transactions inside and outside the date range for two accounts'
        LocalDate d2023_01_05 = LocalDate.parse('2023-01-05')
        LocalDate d2023_01_10 = LocalDate.parse('2023-01-10')
        LocalDate d2022_12_31 = LocalDate.parse('2022-12-31')
        LocalDate d2023_02_01 = LocalDate.parse('2023-02-01')

        Transaction in1 = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(primaryAccountId)
                .withAccountType(AccountType.Debit)
                .withTransactionType(TransactionType.Expense)
                .withAccountNameOwner(primaryName)
                .withTransactionDate(d2023_01_05)
                .withDescription('date_range_in_1')
                .withCategory('range_test')
                .withAmount('10.00')
                .buildAndValidate()

        Transaction in2 = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(secondaryAccountId)
                .withAccountType(AccountType.Debit)
                .withTransactionType(TransactionType.Expense)
                .withAccountNameOwner(secondaryName)
                .withTransactionDate(d2023_01_10)
                .withDescription('date_range_in_2')
                .withCategory('range_test')
                .withAmount('20.00')
                .buildAndValidate()

        Transaction out1 = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(primaryAccountId)
                .withAccountType(AccountType.Debit)
                .withTransactionType(TransactionType.Expense)
                .withAccountNameOwner(primaryName)
                .withTransactionDate(d2022_12_31)
                .withDescription('date_range_out_1')
                .withCategory('range_test')
                .withAmount('30.00')
                .buildAndValidate()

        Transaction out2 = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(secondaryAccountId)
                .withAccountType(AccountType.Debit)
                .withTransactionType(TransactionType.Expense)
                .withAccountNameOwner(secondaryName)
                .withTransactionDate(d2023_02_01)
                .withDescription('date_range_out_2')
                .withCategory('range_test')
                .withAmount('40.00')
                .buildAndValidate()

        transactionRepository.saveAll([in1, in2, out1, out2])

        and: 'A pageable sorted by transactionDate desc'
        def pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc('transactionDate')))

        when: 'Querying by date range 2023-01-01 to 2023-01-31'
        def start = LocalDate.parse('2023-01-01')
        def end = LocalDate.parse('2023-01-31')
        def page = transactionRepository.findByOwnerAndTransactionDateBetween(testOwner, start, end, pageable)

        then: 'Only the in-range transactions are returned, across both accounts'
        page.totalElements >= 2
        page.content*.description.containsAll(['date_range_in_1', 'date_range_in_2'])
        !page.content*.description.contains('date_range_out_1')
        !page.content*.description.contains('date_range_out_2')

        and: 'Results are ordered by date descending (latest first)'
        page.content.size() >= 2
        !page.content[0].transactionDate.isBefore(page.content[1].transactionDate)
    }
}
