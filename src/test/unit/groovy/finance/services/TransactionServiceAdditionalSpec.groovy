package finance.services

import finance.domain.*
import finance.helpers.TransactionBuilder
import org.springframework.util.ResourceUtils
import spock.lang.Specification

import java.math.BigDecimal
import java.sql.Date

class TransactionServiceAdditionalSpec extends BaseServiceSpec {

    void setup() {
        transactionService.validator = validatorMock
        transactionService.accountService = accountServiceMock
        transactionService.categoryService = categoryServiceMock
        transactionService.descriptionService = descriptionService
        transactionService.receiptImageService = receiptImageServiceMock
        transactionService.transactionRepository = transactionRepositoryMock
        transactionService.meterService = new MeterService(new io.micrometer.core.instrument.simple.SimpleMeterRegistry())
    }

    def "calculateActiveTotalsByAccountNameOwner handles unexpected state gracefully"() {
        given:
        def owner = 'acct_b'
        def rows = [
                [BigDecimal.valueOf(1.00), 1, 'weird'] as Object[],
                [BigDecimal.valueOf(2.00), 1, 'future'] as Object[]
        ]

        when:
        def totals = transactionService.calculateActiveTotalsByAccountNameOwner(owner)

        then:
        1 * transactionRepositoryMock.sumTotalsForActiveTransactionsByAccountNameOwner(owner) >> rows
        totals.totalsFuture == new BigDecimal('2.00')
        totals.totalsCleared == BigDecimal.ZERO
        totals.totalsOutstanding == BigDecimal.ZERO
        totals.totals == new BigDecimal('2.00')
    }

    def "updateTransaction updates when existing found"() {
        given:
        def db = TransactionBuilder.builder().withGuid('guid-1').withAccountNameOwner('acct_a').build()
        def incoming = TransactionBuilder.builder().withGuid('guid-1').withAccountNameOwner('acct_a').withCategory('cat1').withDescription('desc1').build()
        def account = new Account(accountId: 42L, accountNameOwner: 'acct_a', accountType: db.accountType)

        when:
        def result = transactionService.updateTransaction(incoming)

        then:
        1 * validatorMock.validate(incoming) >> ([] as Set)
        1 * transactionRepositoryMock.findByGuid('guid-1') >> Optional.of(db)
        1 * accountServiceMock.account('acct_a') >> Optional.of(account)
        1 * categoryServiceMock.category('cat1') >> Optional.of(new Category(categoryName: 'cat1'))
        1 * descriptionRepositoryMock.findByDescriptionName('desc1') >> Optional.of(new Description(descriptionName: 'desc1'))
        1 * transactionRepositoryMock.saveAndFlush({ Transaction t -> t.guid == 'guid-1' && t.accountId == 42L }) >> { it[0] }
        result.guid == 'guid-1'
    }

    def "createFutureTransaction fortnightly adds 14 days"() {
        given:
        def tx = TransactionBuilder.builder()
                .withTransactionDate(Date.valueOf('2024-01-01'))
                .withReoccurringType(ReoccurringType.FortNightly)
                .build()

        when:
        def fut = transactionService.createFutureTransaction(tx)

        then:
        fut.transactionState == TransactionState.Future
        fut.transactionDate == Date.valueOf('2024-01-15')
    }

    def "createFutureTransaction debit with non-monthly throws"() {
        given:
        def tx = TransactionBuilder.builder()
                .withAccountType(AccountType.Debit)
                .withTransactionDate(Date.valueOf('2024-03-01'))
                .withReoccurringType(ReoccurringType.Quarterly)
                .build()

        when:
        transactionService.createFutureTransaction(tx)

        then:
        thrown(InvalidReoccurringTypeException)
    }
    def "calculateActiveTotalsByAccountNameOwner aggregates by state"() {
        given:
        def owner = 'acct_a'
        def rows = [
                [BigDecimal.valueOf(10.00), 1, 'future'] as Object[],
                [BigDecimal.valueOf(5.00), 1, 'cleared'] as Object[],
                [BigDecimal.valueOf(2.50), 1, 'outstanding'] as Object[]
        ]

        when:
        def totals = transactionService.calculateActiveTotalsByAccountNameOwner(owner)

        then:
        1 * transactionRepositoryMock.sumTotalsForActiveTransactionsByAccountNameOwner(owner) >> rows
        totals.totalsFuture == new BigDecimal('10.00')
        totals.totalsCleared == new BigDecimal('5.00')
        totals.totalsOutstanding == new BigDecimal('2.50')
        totals.totals == new BigDecimal('17.50')
    }

    def "findByAccountNameOwnerOrderByTransactionDate returns empty and early when none"() {
        given:
        def owner = 'acct_x'

        when:
        def result = transactionService.findByAccountNameOwnerOrderByTransactionDate(owner)

        then:
        1 * transactionRepositoryMock.findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(owner, _ as Boolean) >> []
        result.isEmpty()
    }

    def "findByAccountNameOwnerOrderByTransactionDate sorts by state then date desc"() {
        given:
        def owner = 'acct_y'
        def t1 = TransactionBuilder.builder().withAccountNameOwner(owner).withTransactionState(TransactionState.Cleared).withTransactionDate(Date.valueOf('2024-01-01')).build()
        def t2 = TransactionBuilder.builder().withAccountNameOwner(owner).withTransactionState(TransactionState.Future).withTransactionDate(Date.valueOf('2024-01-01')).build()
        def t3 = TransactionBuilder.builder().withAccountNameOwner(owner).withTransactionState(TransactionState.Outstanding).withTransactionDate(Date.valueOf('2024-12-31')).build()

        when:
        def result = transactionService.findByAccountNameOwnerOrderByTransactionDate(owner)

        then:
        1 * transactionRepositoryMock.findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(owner, _ as Boolean) >> [t1, t3, t2]
        // Enum order descending: Undefined > Future > Outstanding > Cleared. Expect Future first, then Outstanding, then Cleared
        result[0].transactionState == TransactionState.Future
        result[1].transactionState == TransactionState.Outstanding
        result[2].transactionState == TransactionState.Cleared
    }

    def "updateTransaction throws when guid not found"() {
        given:
        def tx = TransactionBuilder.builder().withGuid('missing').build()

        when:
        transactionService.updateTransaction(tx)

        then:
        1 * validatorMock.validate(tx) >> ([] as Set)
        1 * transactionRepositoryMock.findByGuid('missing') >> Optional.empty()
        thrown(TransactionValidationException)
    }

    def "masterTransactionUpdater throws when guid mismatched"() {
        given:
        def db = TransactionBuilder.builder().withGuid('db-guid').build()
        def incoming = TransactionBuilder.builder().withGuid('other-guid').build()

        when:
        transactionService.masterTransactionUpdater(db, incoming)

        then:
        thrown(TransactionValidationException)
    }

    def "updateTransactionReceiptImageByGuid updates existing receipt image"() {
        given:
        def tx = TransactionBuilder.builder().withReceiptImageId(5L).build()
        byte[] jpg = ResourceUtils.getFile('src/test/unit/resources/viking-icon.jpg').bytes
        String base64Jpeg = Base64.encoder.encodeToString(jpg)
        def existing = new ReceiptImage(receiptImageId: 5L)

        when:
        def result = transactionService.updateTransactionReceiptImageByGuid(tx.guid, base64Jpeg)

        then:
        1 * transactionRepositoryMock.findByGuid(tx.guid) >> Optional.of(tx)
        1 * receiptImageServiceMock.findByReceiptImageId(5L) >> Optional.of(existing)
        1 * receiptImageServiceMock.insertReceiptImage({ it.receiptImageId == 5L }) >> { args -> args[0] }
        result.receiptImageId == 5L
    }

    def "updateTransactionReceiptImageByGuid missing existing image throws"() {
        given:
        def tx = TransactionBuilder.builder().withReceiptImageId(6L).build()
        byte[] jpg = ResourceUtils.getFile('src/test/unit/resources/viking-icon.jpg').bytes
        String payload = Base64.encoder.encodeToString(jpg)

        // stubs
        transactionRepositoryMock.findByGuid(_ as String) >> Optional.of(tx)
        receiptImageServiceMock.findByReceiptImageId(6L) >> Optional.empty()

        when:
        transactionService.updateTransactionReceiptImageByGuid(tx.guid, payload)

        then:
        thrown(ReceiptImageException)
    }

    def "updateTransactionReceiptImageByGuid transaction not found throws"() {
        given:
        byte[] jpg = ResourceUtils.getFile('src/test/unit/resources/viking-icon.jpg').bytes
        String payload = Base64.encoder.encodeToString(jpg)

        // stub
        transactionRepositoryMock.findByGuid(_ as String) >> Optional.empty()

        when:
        transactionService.updateTransactionReceiptImageByGuid('missing', payload)

        then:
        thrown(TransactionNotFoundException)
    }
}
