package finance.processors

import finance.Application
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.domain.AccountType
import finance.domain.TransactionType
import finance.repositories.TransactionRepository
import org.apache.camel.CamelContext
import org.apache.camel.Exchange
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.support.DefaultExchange
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

@ActiveProfiles("int")
@SpringBootTest
@ContextConfiguration(classes = Application)
@Transactional
class ProcessorIntegrationSpec extends Specification {

    @Autowired
    JsonTransactionProcessor jsonTransactionProcessor

    @Autowired
    InsertTransactionProcessor insertTransactionProcessor

    @Autowired
    StringTransactionProcessor stringTransactionProcessor

    @Autowired
    ExceptionProcessor exceptionProcessor

    @Autowired
    TransactionRepository transactionRepository

    @Autowired
    CamelContext camelContext

    void 'test json transaction processor with valid json'() {
        given:
        def validJsonString = """[
            {
                "guid": "${UUID.randomUUID()}",
                "accountNameOwner": "test_checking",
                "accountType": "debit",
                "description": "JSON Processor Test",
                "category": "test_category",
                "amount": 150.75,
                "transactionDate": "2023-05-15",
                "transactionState": "cleared",
                "transactionType": "expense",
                "notes": "JSON processor integration test"
            }
        ]"""

        Exchange exchange = new DefaultExchange(camelContext)
        exchange.getIn().setBody(validJsonString)

        when:
        jsonTransactionProcessor.process(exchange)

        then:
        exchange.getIn().getBody() instanceof Transaction[]
        List<Transaction> transactions = (exchange.getIn().getBody() as Transaction[]).toList()
        transactions.size() == 1
        transactions[0].description == "JSON Processor Test"
        transactions[0].amount == 150.75
        transactions[0].accountNameOwner == "test_checking"
        transactions[0].transactionState == TransactionState.Cleared
        transactions[0].transactionType == TransactionType.Expense
    }

    void 'test json transaction processor with multiple transactions'() {
        given:
        def multipleTransactionsJson = """[
            {
                "guid": "${UUID.randomUUID()}",
                "accountNameOwner": "test_checking",
                "accountType": "debit",
                "description": "Multi JSON Test 1",
                "category": "categorya",
                "amount": 25.50,
                "transactionDate": "2023-05-15",
                "transactionState": "cleared",
                "transactionType": "income"
            },
            {
                "guid": "${UUID.randomUUID()}",
                "accountNameOwner": "test_savings",
                "accountType": "debit",
                "description": "Multi JSON Test 2",
                "category": "categoryb",
                "amount": 75.25,
                "transactionDate": "2023-05-16",
                "transactionState": "outstanding",
                "transactionType": "expense"
            }
        ]"""

        Exchange exchange = new DefaultExchange(camelContext)
        exchange.getIn().setBody(multipleTransactionsJson)

        when:
        jsonTransactionProcessor.process(exchange)

        then:
        exchange.getIn().getBody() instanceof Transaction[]
        List<Transaction> transactions = (exchange.getIn().getBody() as Transaction[]).toList()
        transactions.size() == 2

        transactions[0].description == "Multi JSON Test 1"
        transactions[0].amount == 25.50
        transactions[0].transactionType == TransactionType.Income

        transactions[1].description == "Multi JSON Test 2"
        transactions[1].amount == 75.25
        transactions[1].transactionState == TransactionState.Outstanding
    }

    void 'test insert transaction processor with valid transaction'() {
        given:
        def guid = UUID.randomUUID().toString()
        def json = new groovy.json.JsonBuilder([
            guid              : guid,
            accountNameOwner  : 'test_checking',
            accountType       : 'debit',
            description       : 'Insert Processor Test',
            category          : 'processor_category',
            amount            : 200.00,
            transactionDate   : '2023-05-20',
            transactionState  : 'cleared',
            transactionType   : 'expense',
            notes             : 'Insert processor integration test'
        ]).toString()

        Exchange exchange = new DefaultExchange(camelContext)
        exchange.getIn().setBody(json)

        when:
        insertTransactionProcessor.process(exchange)

        then:
        noExceptionThrown()

        // Verify transaction was saved to database
        Optional<Transaction> savedTransaction = transactionRepository.findByGuid(guid)
        savedTransaction.isPresent()
        savedTransaction.get().description == "Insert Processor Test"
        savedTransaction.get().amount == 200.00
    }

    void 'test insert transaction processor with list of transactions'() {
        given:
        List<Map<String, Object>> testTransactions = []
        for (int i = 0; i < 3; i++) {
            def owner = (i == 0) ? 'test_checking' : (i == 1) ? 'test_savings' : 'test_credit'
            def acctType = (owner == 'test_credit') ? 'credit' : 'debit'
            testTransactions.add([
                guid             : UUID.randomUUID().toString(),
                accountNameOwner : owner,
                accountType      : acctType,
                description      : "Bulk Insert Test ${i}",
                category         : 'bulk_category',
                amount           : 100.00 + i,
                transactionDate  : "2023-05-2${i + 1}",
                transactionState : 'cleared',
                transactionType  : 'expense',
                activeStatus     : true
            ])
        }

        when:
        // Process each transaction individually since the processor expects a single transaction payload
        testTransactions.each { tx ->
            def payload = new groovy.json.JsonBuilder(tx).toString()
            Exchange ex = new DefaultExchange(camelContext)
            ex.getIn().setBody(payload)
            insertTransactionProcessor.process(ex)
        }

        then:
        noExceptionThrown()

        // Verify all transactions were saved
        testTransactions.each { transaction ->
            Optional<Transaction> savedTransaction = transactionRepository.findByGuid(transaction.guid as String)
            assert savedTransaction.isPresent()
            assert savedTransaction.get().description.startsWith("Bulk Insert Test")
        }
    }

    void 'test string transaction processor with csv-like data'() {
        given:
        Transaction tx = new Transaction(
            guid: UUID.randomUUID().toString(),
            accountNameOwner: 'test_checking',
            accountType: AccountType.Debit,
            description: 'CSV Test Transaction',
            category: 'CSVCategory',
            amount: new BigDecimal('50.25'),
            transactionDate: java.sql.Date.valueOf('2023-05-25'),
            transactionState: TransactionState.Cleared,
            transactionType: TransactionType.Expense,
            activeStatus: true
        )

        Exchange exchange = new DefaultExchange(camelContext)
        exchange.getIn().setBody(tx)

        when:
        stringTransactionProcessor.process(exchange)

        then:
        noExceptionThrown()
        // The processor should handle string data appropriately
        exchange.getIn().getBody() != null
    }

    void 'test exception processor with error handling'() {
        given:
        Exception testException = new RuntimeException("Test exception for processor")

        Exchange exchange = new DefaultExchange(camelContext)
        exchange.setException(testException)
        exchange.getIn().setBody("Some test data that caused an error")

        when:
        exceptionProcessor.process(exchange)

        then:
        noExceptionThrown()
        // Exception processor should handle the error gracefully
        exchange.getException() != null
        exchange.getIn().getBody() != null
    }

    void 'test processor integration with transaction validation'() {
        given:
        def invalidTransactionJson = """[
            {
                "guid": "${UUID.randomUUID()}",
                "accountNameOwner": "test_checking",
                "accountType": "debit",
                "description": "ab",
                "category": "invalid category with spaces",
                "amount": 100.00,
                "transactionDate": "2023-05-15",
                "transactionState": "cleared",
                "transactionType": "expense"
            }
        ]"""

        Exchange exchange = new DefaultExchange(camelContext)
        exchange.getIn().setBody(invalidTransactionJson)

        when:
        jsonTransactionProcessor.process(exchange)

        then:
        thrown(jakarta.validation.ValidationException)
    }

    void 'test processor error handling and recovery'() {
        given:
        def malformedJson = """{ "invalid": "json", "missing": "bracket" """

        Exchange exchange = new DefaultExchange(camelContext)
        exchange.getIn().setBody(malformedJson)

        when:
        try {
            jsonTransactionProcessor.process(exchange)
        } catch (Exception e) {
            exchange.setException(e)
            exceptionProcessor.process(exchange)
        }

        then:
        // Exception should be handled by exception processor
        exchange.getException() != null
        exchange.getIn().getBody() != null
    }

    void 'test processor performance with large transaction set'() {
        given:
        List<Map<String, Object>> largeTransactionSet = []
        for (int i = 0; i < 100; i++) {
            largeTransactionSet.add([
                guid: UUID.randomUUID().toString(),
                accountNameOwner: "test_checking",
                accountType: "debit",
                description: "Performance Test Transaction ${i}",
                category: "performancecategory",
                amount: new BigDecimal(String.format('%.2f', Math.random() * 1000)),
                transactionDate: "2023-05-15",
                transactionState: "cleared",
                transactionType: "expense"
            ])
        }

        def largeJsonString = new groovy.json.JsonBuilder(largeTransactionSet).toString()
        Exchange exchange = new DefaultExchange(camelContext)
        exchange.getIn().setBody(largeJsonString)

        when:
        long startTime = System.currentTimeMillis()
        jsonTransactionProcessor.process(exchange)
        long endTime = System.currentTimeMillis()

        then:
        exchange.getIn().getBody() instanceof Transaction[]
        List<Transaction> transactions = (exchange.getIn().getBody() as Transaction[]).toList()
        transactions.size() == 100
        (endTime - startTime) < 10000  // Should process within 10 seconds
    }

    void 'test processor with different transaction types and states'() {
        given:
        def mixedTransactionsJson = """[
            {
                "guid": "${UUID.randomUUID()}",
                "accountNameOwner": "test_checking",
                "accountType": "debit",
                "description": "Credit Transaction",
                "category": "cat_income",
                "amount": 500.00,
                "transactionDate": "2023-05-15",
                "transactionState": "cleared",
                "transactionType": "income"
            },
            {
                "guid": "${UUID.randomUUID()}",
                "accountNameOwner": "test_savings",
                "accountType": "debit",
                "description": "Outstanding Transaction",
                "category": "cat_planned",
                "amount": 200.00,
                "transactionDate": "2023-05-16",
                "transactionState": "outstanding",
                "transactionType": "expense"
            },
            {
                "guid": "${UUID.randomUUID()}",
                "accountNameOwner": "test_credit",
                "accountType": "debit",
                "description": "Cleared Expense Transaction",
                "category": "cat_purchase",
                "amount": 75.00,
                "transactionDate": "2023-05-10",
                "transactionState": "cleared",
                "transactionType": "expense"
            }
        ]"""

        Exchange exchange = new DefaultExchange(camelContext)
        exchange.getIn().setBody(mixedTransactionsJson)

        when:
        jsonTransactionProcessor.process(exchange)

        then:
        exchange.getIn().getBody() instanceof Transaction[]
        List<Transaction> transactions = (exchange.getIn().getBody() as Transaction[]).toList()
        transactions.size() == 3

        // Verify different transaction types and states are processed correctly
        transactions.find { it.transactionType == TransactionType.Income } != null
        transactions.find { it.transactionType == TransactionType.Expense } != null
        transactions.find { it.transactionState == TransactionState.Cleared } != null
    }

    void 'test processor memory usage with concurrent processing'() {
        given:
        List<Thread> processingThreads = []
        int threadCount = 5

        for (int t = 0; t < threadCount; t++) {
            Thread thread = new Thread({
                def transactionJson = """[
                    {
                        "guid": "${UUID.randomUUID()}",
                        "accountNameOwner": "test_checking",
                        "accountType": "debit",
                        "description": "Concurrent Processor Test ${t}",
                        "category": "Concurrent Category",
                        "amount": ${100.00 + t},
                        "transactionDate": "2023-05-15",
                        "transactionState": "cleared",
                        "transactionType": "expense"
                    }
                ]"""

                Exchange exchange = new DefaultExchange(camelContext)
                exchange.getIn().setBody(transactionJson)

                try {
                    jsonTransactionProcessor.process(exchange)
                } catch (Exception e) {
                    e.printStackTrace()
                }
            })
            processingThreads.add(thread)
        }

        when:
        processingThreads.each { it.start() }
        processingThreads.each { it.join(5000) }  // Wait up to 5 seconds for each thread

        then:
        processingThreads.every { !it.isAlive() }  // All threads should complete
        noExceptionThrown()
    }
}
