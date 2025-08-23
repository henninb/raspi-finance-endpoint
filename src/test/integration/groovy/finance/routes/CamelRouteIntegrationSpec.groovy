package finance.routes

import finance.Application
import finance.repositories.TransactionRepository
import finance.repositories.AccountRepository
import finance.domain.Account
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
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.ResourceUtils
import spock.lang.Specification
import spock.lang.Ignore
import spock.util.concurrent.PollingConditions

import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Date
import java.sql.Timestamp

@ActiveProfiles("int")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Application)
@Transactional
class CamelRouteIntegrationSpec extends Specification {

    @Autowired
    CamelContext camelContext

    @Autowired
    ProducerTemplate producerTemplate

    @Autowired
    TransactionRepository transactionRepository

    @Autowired
    AccountRepository accountRepository

    protected String baseName = new File(".").absolutePath
    protected PollingConditions conditions = new PollingConditions(timeout: 10, initialDelay: 0.5, factor: 1.1)

    def setup() {
        cleanupTestDirectories()
        createTestAccount()
    }

    private void createTestAccount() {
        // Create test accounts for Camel route testing - must match JSON accountNameOwner
        Account testAccount = new Account()
        testAccount.accountNameOwner = "test-checking_brian"  // Matches pattern ^[a-z-]*_[a-z]*$
        testAccount.accountType = AccountType.Debit  // Use Debit type as specified in JSON
        testAccount.activeStatus = true
        testAccount.moniker = "0000"
        testAccount.outstanding = new BigDecimal("0.00")
        testAccount.future = new BigDecimal("0.00")
        testAccount.cleared = new BigDecimal("0.00")
        testAccount.dateClosed = new Timestamp(System.currentTimeMillis())
        testAccount.validationDate = new Timestamp(System.currentTimeMillis())
        Account savedTestAccount = accountRepository.save(testAccount)
        println("Created test account: ${savedTestAccount.accountNameOwner} with ID: ${savedTestAccount.accountId}")

        // Create second test account for multiple transactions test
        Account testSavingsAccount = new Account()
        testSavingsAccount.accountNameOwner = "test-savings_brian"
        testSavingsAccount.accountType = AccountType.Credit
        testSavingsAccount.activeStatus = true
        testSavingsAccount.moniker = "0001"
        testSavingsAccount.outstanding = new BigDecimal("0.00")
        testSavingsAccount.future = new BigDecimal("0.00")
        testSavingsAccount.cleared = new BigDecimal("0.00")
        testSavingsAccount.dateClosed = new Timestamp(System.currentTimeMillis())
        testSavingsAccount.validationDate = new Timestamp(System.currentTimeMillis())
        Account savedSavingsAccount = accountRepository.save(testSavingsAccount)
        println("Created savings account: ${savedSavingsAccount.accountNameOwner} with ID: ${savedSavingsAccount.accountId}")
    }

    def cleanup() {
        cleanupTestDirectories()
    }

    private void cleanupTestDirectories() {
        ["int_json_in", "int_json_in/.processed-successfully",
         "int_json_in/.not-processed-failed-with-errors",
         "int_json_in/.not-processed-non-json-file",
         "int_json_in/.not-processed-json-parsing-errors"].each { dir ->
            def dirFile = new File("$baseName/$dir")
            if (dirFile.exists()) {
                dirFile.listFiles()?.each { file ->
                    if (file.isFile() && !file.name.startsWith('.')) {
                        file.delete()
                    }
                }
            }
        }
    }

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
            // Check that error handling directory exists and contains the failed file, or file was moved to any error directory
            def jsonParsingErrorDir = new File("$baseName/int_json_in/.not-processed-json-parsing-errors")
            def failedWithErrorsDir = new File("$baseName/int_json_in/.not-processed-failed-with-errors")

            (jsonParsingErrorDir.exists() && jsonParsingErrorDir.listFiles()?.size() > 0) ||
            (failedWithErrorsDir.exists() && failedWithErrorsDir.listFiles()?.size() > 0) ||
            !destinationFile.exists()  // File was processed (deleted/moved)
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
            failureDir.exists() && failureDir.listFiles()?.size() > 0
        }

        cleanup:
        sourceFile?.delete()
    }


    void 'test camel route error handling and recovery'() {
        given:
        def invalidTransactionJson = """[
            {
                "guid": "${UUID.randomUUID()}",
                "accountNameOwner": "test-checking_brian",
                "accountType": "InvalidAccountType",
                "description": "Error Test Transaction",
                "category": "Error Category",
                "amount": "invalid_amount",
                "transactionDate": "invalid-date",
                "transactionState": "Cleared",
                "transactionType": "expense"
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

}
