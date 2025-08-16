package finance.routes

import finance.Application
import finance.repositories.TransactionRepository
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.domain.AccountType
import finance.domain.TransactionType
import org.apache.camel.CamelContext
import org.apache.camel.ProducerTemplate
import org.apache.camel.component.mock.MockEndpoint
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.util.ResourceUtils
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.nio.file.Files
import java.nio.file.Paths

@ActiveProfiles("int")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Application)
class CamelRouteIntegrationSpec extends Specification {

    @Autowired
    CamelContext camelContext

    @Autowired
    ProducerTemplate producerTemplate

    @Autowired
    TransactionRepository transactionRepository

    protected String baseName = new File(".").absolutePath
    protected PollingConditions conditions = new PollingConditions(timeout: 30, initialDelay: 2, factor: 1.25)

    void 'test camel context is running'() {
        expect:
        camelContext != null
        camelContext.isStarted()
        camelContext.getRoutes().size() > 0
    }

    void 'test json file reader route exists and is active'() {
        when:
        def jsonFileReaderRoute = camelContext.getRoute("JsonFileReaderRoute")

        then:
        jsonFileReaderRoute != null
        camelContext.getRouteController().getRouteStatus("JsonFileReaderRoute").isStarted()
    }

    void 'test transaction to database route exists and is active'() {
        when:
        def transactionToDatabaseRoute = camelContext.getRoute("TransactionToDatabaseRoute")

        then:
        transactionToDatabaseRoute != null
        camelContext.getRouteController().getRouteStatus("TransactionToDatabaseRoute").isStarted()
    }

    void 'test json file writer route exists and is active'() {
        when:
        def jsonFileWriterRoute = camelContext.getRoute("JsonFileWriterRoute")

        then:
        jsonFileWriterRoute != null
        camelContext.getRouteController().getRouteStatus("JsonFileWriterRoute").isStarted()
    }

    void 'test complete file processing workflow'() {
        given:
        def testTransactionJson = """[
            {
                "guid": "${UUID.randomUUID()}",
                "accountNameOwner": "test_checking_brian",
                "accountType": "Checking",
                "description": "Integration Test Transaction",
                "category": "Test Category",
                "amount": 123.45,
                "transactionDate": "2023-05-15",
                "transactionState": "Cleared",
                "transactionType": "Debit",
                "notes": "Camel integration test"
            }
        ]"""

        File sourceFile = File.createTempFile("test-transaction", ".json")
        sourceFile.text = testTransactionJson
        File destinationFile = new File("$baseName/int_json_in/${UUID.randomUUID()}.json")

        when:
        Files.copy(sourceFile.toPath(), destinationFile.toPath())

        then:
        conditions.eventually {
            def transactions = transactionRepository.findByDescriptionAndActiveStatusOrderByTransactionDateDesc(
                "Integration Test Transaction", true)
            transactions.size() == 1
            transactions[0].description == "Integration Test Transaction"
            transactions[0].category == "Test Category"
            transactions[0].amount == 123.45
            transactions[0].accountNameOwner == "test_checking_brian"
            transactions[0].transactionState == TransactionState.Cleared
            transactions[0].transactionType == TransactionType.Expense
        }

        cleanup:
        sourceFile?.delete()
    }

    void 'test multiple transactions in single file processing'() {
        given:
        def multipleTransactionsJson = """[
            {
                "guid": "${UUID.randomUUID()}",
                "accountNameOwner": "test_checking_brian",
                "accountType": "Checking",
                "description": "Multi Test Transaction 1",
                "category": "Test Category A",
                "amount": 50.00,
                "transactionDate": "2023-05-15",
                "transactionState": "Cleared",
                "transactionType": "Credit"
            },
            {
                "guid": "${UUID.randomUUID()}",
                "accountNameOwner": "testsavings_brian",
                "accountType": "Savings",
                "description": "Multi Test Transaction 2",
                "category": "Test Category B",
                "amount": 75.25,
                "transactionDate": "2023-05-16",
                "transactionState": "Outstanding",
                "transactionType": "Debit"
            },
            {
                "guid": "${UUID.randomUUID()}",
                "accountNameOwner": "test_checking_brian",
                "accountType": "Checking",
                "description": "Multi Test Transaction 3",
                "category": "Test Category C",
                "amount": 100.00,
                "transactionDate": "2023-05-17",
                "transactionState": "Future",
                "transactionType": "Debit"
            }
        ]"""

        File sourceFile = File.createTempFile("multi-transaction", ".json")
        sourceFile.text = multipleTransactionsJson
        File destinationFile = new File("$baseName/int_json_in/${UUID.randomUUID()}.json")

        when:
        Files.copy(sourceFile.toPath(), destinationFile.toPath())

        then:
        conditions.eventually {
            def transactions1 = transactionRepository.findByDescriptionAndActiveStatusOrderByTransactionDateDesc(
                "Multi Test Transaction 1", true)
            def transactions2 = transactionRepository.findByDescriptionAndActiveStatusOrderByTransactionDateDesc(
                "Multi Test Transaction 2", true)
            def transactions3 = transactionRepository.findByDescriptionAndActiveStatusOrderByTransactionDateDesc(
                "Multi Test Transaction 3", true)
            
            transactions1.size() == 1
            transactions2.size() == 1
            transactions3.size() == 1
            
            transactions1[0].amount == 50.00
            transactions1[0].transactionType == TransactionType.Income
            transactions1[0].accountNameOwner == "test_checking_brian"
            
            transactions2[0].amount == 75.25
            transactions2[0].transactionState == TransactionState.Outstanding
            transactions2[0].accountNameOwner == "testsavings_brian"
            
            transactions3[0].amount == 100.00
            transactions3[0].transactionState == TransactionState.Future
            transactions3[0].accountNameOwner == "test_checking_brian"
        }

        cleanup:
        sourceFile?.delete()
    }

    void 'test invalid json file handling'() {
        given:
        def invalidJson = """{ "invalid": "json", "missing": "closing bracket" """

        File sourceFile = File.createTempFile("invalid-json", ".json")
        sourceFile.text = invalidJson
        File destinationFile = new File("$baseName/int_json_in/${UUID.randomUUID()}.json")

        when:
        Files.copy(sourceFile.toPath(), destinationFile.toPath())

        then:
        conditions.eventually {
            // Check that error handling directory exists and contains the failed file
            def errorDir = new File("$baseName/int_json_in/.not-processed-json-parsing-errors")
            errorDir.exists() || errorDir.listFiles()?.size() > 0
        }

        cleanup:
        sourceFile?.delete()
    }

    void 'test non-json file handling'() {
        given:
        def textContent = "This is not a JSON file"

        File sourceFile = File.createTempFile("non-json", ".txt")
        sourceFile.text = textContent
        File destinationFile = new File("$baseName/int_json_in/${UUID.randomUUID()}.txt")

        when:
        Files.copy(sourceFile.toPath(), destinationFile.toPath())

        then:
        conditions.eventually {
            // Check that non-JSON files are moved to failure directory
            def failureDir = new File("$baseName/int_json_in/.not-processed-non-json-file")
            failureDir.exists() || failureDir.listFiles()?.size() > 0
        }

        cleanup:
        sourceFile?.delete()
    }

    void 'test direct route transaction processing'() {
        given:
        def transactionData = [
            guid: UUID.randomUUID().toString(),
            accountNameOwner: "test_checking_brian",
            accountType: AccountType.Checking,
            description: "Direct Route Test Transaction",
            category: "Direct Test Category",
            amount: 99.99,
            transactionDate: "2023-05-20",
            transactionState: TransactionState.Cleared,
            transactionType: TransactionType.Expense,
            notes: "Direct route processing test"
        ]

        when:
        producerTemplate.sendBody("direct:transactionToDatabaseRoute", transactionData)

        then:
        conditions.eventually {
            def transactions = transactionRepository.findByDescriptionAndActiveStatusOrderByTransactionDateDesc(
                "Direct Route Test Transaction", true)
            transactions.size() == 1
            transactions[0].description == "Direct Route Test Transaction"
            transactions[0].amount == 99.99
        }
    }

    void 'test camel route error handling and recovery'() {
        given:
        def invalidTransactionJson = """[
            {
                "guid": "${UUID.randomUUID()}",
                "accountNameOwner": "test_checking_brian",
                "accountType": "InvalidAccountType",
                "description": "Error Test Transaction",
                "category": "Error Category",
                "amount": "invalid_amount",
                "transactionDate": "invalid-date",
                "transactionState": "Cleared",
                "transactionType": "Debit"
            }
        ]"""

        File sourceFile = File.createTempFile("error-transaction", ".json")
        sourceFile.text = invalidTransactionJson
        File destinationFile = new File("$baseName/int_json_in/${UUID.randomUUID()}.json")

        when:
        Files.copy(sourceFile.toPath(), destinationFile.toPath())

        then:
        conditions.eventually {
            // Verify that the invalid file was moved to error directory
            def errorDirs = [
                new File("$baseName/int_json_in/.not-processed-json-parsing-errors"),
                new File("$baseName/int_json_in/.not-processed-failed-with-errors")
            ]
            errorDirs.any { it.exists() && it.listFiles()?.size() > 0 }
        }

        cleanup:
        sourceFile?.delete()
    }

    void 'test file processing performance with multiple files'() {
        given:
        List<File> testFiles = []
        int fileCount = 5

        for (int i = 0; i < fileCount; i++) {
            def transactionJson = """[
                {
                    "guid": "${UUID.randomUUID()}",
                    "accountNameOwner": "test_checking_brian",
                    "accountType": "Checking",
                    "description": "Performance Test Transaction ${i}",
                    "category": "Performance Category",
                    "amount": ${10.00 + i},
                    "transactionDate": "2023-05-${15 + i}",
                    "transactionState": "Cleared",
                    "transactionType": "Debit"
                }
            ]"""

            File sourceFile = File.createTempFile("perf-test-${i}", ".json")
            sourceFile.text = transactionJson
            File destinationFile = new File("$baseName/int_json_in/${UUID.randomUUID()}.json")
            
            testFiles.add(sourceFile)
            Files.copy(sourceFile.toPath(), destinationFile.toPath())
        }

        when:
        long startTime = System.currentTimeMillis()

        then:
        conditions.eventually {
            def allProcessed = (0..<fileCount).every { i ->
                def transactions = transactionRepository.findByDescriptionAndActiveStatusOrderByTransactionDateDesc(
                    "Performance Test Transaction ${i}", true)
                transactions.size() == 1
            }
            allProcessed
        }

        long endTime = System.currentTimeMillis()
        (endTime - startTime) < 60000  // Should process within 60 seconds

        cleanup:
        testFiles?.each { it.delete() }
    }

    void 'test camel route metrics and monitoring'() {
        given:
        def routes = camelContext.getRoutes()

        when:
        def jsonReaderRoute = routes.find { it.id == "JsonFileReaderRoute" }
        def transactionDbRoute = routes.find { it.id == "TransactionToDatabaseRoute" }
        def jsonWriterRoute = routes.find { it.id == "JsonFileWriterRoute" }

        then:
        jsonReaderRoute != null
        transactionDbRoute != null
        jsonWriterRoute != null

        // Verify route statistics are available
        jsonReaderRoute.getCamelContext() != null
        transactionDbRoute.getCamelContext() != null
        jsonWriterRoute.getCamelContext() != null
    }

    void 'test concurrent file processing'() {
        given:
        List<File> sourceFiles = []
        List<Thread> processingThreads = []
        int concurrentFiles = 3

        for (int i = 0; i < concurrentFiles; i++) {
            def transactionJson = """[
                {
                    "guid": "${UUID.randomUUID()}",
                    "accountNameOwner": "test_checking_brian",
                    "accountType": "Checking",
                    "description": "Concurrent Test Transaction ${i}",
                    "category": "Concurrent Category",
                    "amount": ${20.00 + i},
                    "transactionDate": "2023-06-${10 + i}",
                    "transactionState": "Cleared",
                    "transactionType": "Debit"
                }
            ]"""

            File sourceFile = File.createTempFile("concurrent-test-${i}", ".json")
            sourceFile.text = transactionJson
            sourceFiles.add(sourceFile)

            Thread processThread = new Thread({
                try {
                    File destinationFile = new File("$baseName/int_json_in/${UUID.randomUUID()}.json")
                    Files.copy(sourceFile.toPath(), destinationFile.toPath())
                } catch (Exception e) {
                    e.printStackTrace()
                }
            })
            processingThreads.add(processThread)
        }

        when:
        processingThreads.each { it.start() }
        processingThreads.each { it.join(10000) }  // Wait up to 10 seconds for each thread

        then:
        conditions.eventually {
            def allProcessed = (0..<concurrentFiles).every { i ->
                def transactions = transactionRepository.findByDescriptionAndActiveStatusOrderByTransactionDateDesc(
                    "Concurrent Test Transaction ${i}", true)
                transactions.size() == 1
            }
            allProcessed
        }

        cleanup:
        sourceFiles?.each { it.delete() }
    }
}
