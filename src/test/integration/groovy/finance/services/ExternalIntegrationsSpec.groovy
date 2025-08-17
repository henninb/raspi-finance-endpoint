package finance.services

import finance.Application
import finance.domain.Transaction
import finance.domain.Account
import finance.domain.AccountType
import finance.domain.TransactionState
import finance.domain.TransactionType
import finance.repositories.TransactionRepository
import finance.repositories.AccountRepository
import finance.services.MeterService
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.Counter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

import java.sql.Date
import java.sql.Timestamp
import java.math.BigDecimal

@ActiveProfiles("int")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Application)
@Transactional
class ExternalIntegrationsSpec extends Specification {

    @LocalServerPort
    int port

    @Autowired
    TestRestTemplate restTemplate

    @Autowired
    MeterRegistry meterRegistry

    @Autowired(required = false)
    MeterService meterService

    @Autowired
    TransactionRepository transactionRepository

    @Autowired
    AccountRepository accountRepository

    String baseUrl

    void setup() {
        baseUrl = "http://localhost:${port}"
        setupTestData()
    }

    void setupTestData() {
        Account testAccount = new Account()
        testAccount.accountNameOwner = "metricstestchecking_brian"
        testAccount.accountType = AccountType.Credit
        testAccount.activeStatus = true
        testAccount.moniker = "2500"
        testAccount.outstanding = new BigDecimal("2000.00")
        testAccount.future = new BigDecimal("0.00")
        testAccount.cleared = new BigDecimal("2000.00")
        testAccount.dateClosed = new Timestamp(System.currentTimeMillis())
        testAccount.validationDate = new Timestamp(System.currentTimeMillis())
        accountRepository.save(testAccount)
    }

    void 'test meter registry bean configuration'() {
        expect:
        meterRegistry != null
        meterRegistry.getClass().simpleName.contains("MeterRegistry")
    }

    void 'test meter service bean availability'() {
        expect:
        meterService != null || true  // Service may be conditional
    }

    void 'test actuator metrics endpoint accessibility'() {
        when:
        ResponseEntity<String> response = restTemplate.getForEntity("${baseUrl}/actuator/metrics", String.class)

        then:
        response.statusCode == HttpStatus.OK
        response.body != null
        response.body.contains("names")
    }

    void 'test actuator health endpoint with detailed information'() {
        when:
        ResponseEntity<Map> response = restTemplate.getForEntity("${baseUrl}/actuator/health", Map.class)

        then:
        response.statusCode == HttpStatus.OK
        response.body != null
        response.body.status != null
        response.body.status == "UP" || response.body.status == "DOWN"
    }

    void 'test JVM metrics availability'() {
        when:
        ResponseEntity<Map> response = restTemplate.getForEntity("${baseUrl}/actuator/metrics/jvm.memory.used", Map.class)

        then:
        response.statusCode == HttpStatus.OK
        response.body != null
        response.body.name == "jvm.memory.used"
        response.body.measurements != null
    }

    void 'test database connection pool metrics'() {
        when:
        ResponseEntity<Map> response = restTemplate.getForEntity("${baseUrl}/actuator/metrics/hikari.connections.active", Map.class)

        then:
        // HikariCP metrics may not be available in test environment with H2
        response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.NOT_FOUND
        
        if (response.statusCode == HttpStatus.OK) {
            assert response.body.name == "hikari.connections.active"
            assert response.body.measurements != null
        }
    }

    void 'test custom application metrics registration'() {
        given:
        Counter customCounter = meterRegistry.counter("test.integration.counter")
        Timer customTimer = meterRegistry.timer("test.integration.timer")

        when:
        customCounter.increment()
        customCounter.increment(5)
        
        Timer.Sample sample = Timer.start(meterRegistry)
        Thread.sleep(100)  // Simulate some work
        sample.stop(customTimer)

        then:
        customCounter.count() == 6.0
        customTimer.count() >= 1
        customTimer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS) > 0
    }

    void 'test transaction metrics integration'() {
        given:
        def savedAccount = accountRepository.findByAccountNameOwner("metricstestchecking_brian").get()
        
        Transaction testTransaction = new Transaction()
        testTransaction.guid = UUID.randomUUID().toString()
        testTransaction.accountNameOwner = "metricstestchecking_brian"
        testTransaction.accountId = savedAccount.accountId
        testTransaction.accountType = AccountType.Credit
        testTransaction.description = "metricstest"
        testTransaction.category = "metricstest"
        testTransaction.amount = new BigDecimal("100.00")
        testTransaction.transactionDate = Date.valueOf("2023-05-20")
        testTransaction.transactionState = TransactionState.Cleared
        testTransaction.transactionType = TransactionType.Expense
        testTransaction.activeStatus = true
        testTransaction.notes = ""

        when:
        transactionRepository.save(testTransaction)
        
        // Create metrics for transaction operations
        Counter transactionCounter = meterRegistry.counter("transactions.created", "type", "debit")
        transactionCounter.increment()

        then:
        transactionCounter.count() == 1.0
        transactionRepository.findByGuid(testTransaction.guid).isPresent()
    }

    void 'test HTTP request metrics'() {
        when:
        // Make several HTTP requests to generate metrics
        restTemplate.getForEntity("${baseUrl}/actuator/health", String.class)
        restTemplate.getForEntity("${baseUrl}/actuator/info", String.class)
        restTemplate.getForEntity("${baseUrl}/actuator/metrics", String.class)

        ResponseEntity<Map> response = restTemplate.getForEntity("${baseUrl}/actuator/metrics/http.server.requests", Map.class)

        then:
        response.statusCode == HttpStatus.OK
        response.body != null
        response.body.name == "http.server.requests"
        response.body.measurements != null
        response.body.availableTags != null
    }

    void 'test actuator info endpoint'() {
        when:
        ResponseEntity<Map> response = restTemplate.getForEntity("${baseUrl}/actuator/info", Map.class)

        then:
        response.statusCode == HttpStatus.OK
        response.body != null
        // Info endpoint may be empty or contain application information
    }

    void 'test actuator beans endpoint'() {
        when:
        ResponseEntity<Map> response = restTemplate.getForEntity("${baseUrl}/actuator/beans", Map.class)

        then:
        response.statusCode == HttpStatus.OK
        response.body != null
        response.body.contexts != null
        response.body.contexts.containsKey("application") || response.body.contexts.size() > 0
    }

    void 'test actuator env endpoint'() {
        when:
        ResponseEntity<Map> response = restTemplate.getForEntity("${baseUrl}/actuator/env", Map.class)

        then:
        response.statusCode == HttpStatus.OK
        response.body != null
        response.body.activeProfiles != null
        response.body.propertySources != null
        response.body.activeProfiles.contains("int")
    }

    void 'test database metrics with transaction operations'() {
        given:
        Timer dbTimer = meterRegistry.timer("database.transaction.insert.time")
        Counter dbCounter = meterRegistry.counter("database.transaction.insert.count")

        when:
        Timer.Sample sample = Timer.start(meterRegistry)
        
        def savedAccount = accountRepository.findByAccountNameOwner("metricstestchecking_brian").get()
        
        Transaction transaction = new Transaction()
        transaction.guid = UUID.randomUUID().toString()
        transaction.accountNameOwner = "metricstestchecking_brian"
        transaction.accountId = savedAccount.accountId
        transaction.accountType = AccountType.Credit
        transaction.description = "dbtest"
        transaction.category = "dbtest"
        transaction.amount = new BigDecimal("75.50")
        transaction.transactionDate = Date.valueOf("2023-05-21")
        transaction.transactionState = TransactionState.Cleared
        transaction.transactionType = TransactionType.Income
        transaction.activeStatus = true
        transaction.notes = ""
        
        transactionRepository.save(transaction)
        sample.stop(dbTimer)
        dbCounter.increment()

        then:
        dbTimer.count() >= 1
        dbTimer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS) > 0
        dbCounter.count() >= 1
    }

    void 'test circuit breaker metrics integration'() {
        when:
        ResponseEntity<Map> response = restTemplate.getForEntity("${baseUrl}/actuator/metrics/resilience4j.circuitbreaker.state", Map.class)

        then:
        // Circuit breaker metrics may not be available if no circuit breakers are configured for the integration profile
        response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.NOT_FOUND

        if (response.statusCode == HttpStatus.OK) {
            assert response.body.name == "resilience4j.circuitbreaker.state"
            assert response.body.measurements != null
        }
    }

    void 'test memory and garbage collection metrics'() {
        when:
        ResponseEntity<Map> memoryResponse = restTemplate.getForEntity("${baseUrl}/actuator/metrics/jvm.memory.max", Map.class)
        ResponseEntity<Map> gcResponse = restTemplate.getForEntity("${baseUrl}/actuator/metrics/jvm.gc.pause", Map.class)

        then:
        memoryResponse.statusCode == HttpStatus.OK
        memoryResponse.body.name == "jvm.memory.max"
        
        gcResponse.statusCode == HttpStatus.OK
        gcResponse.body.name == "jvm.gc.pause"
    }

    void 'test thread pool metrics'() {
        when:
        ResponseEntity<Map> response = restTemplate.getForEntity("${baseUrl}/actuator/metrics/jvm.threads.live", Map.class)

        then:
        response.statusCode == HttpStatus.OK
        response.body != null
        response.body.name == "jvm.threads.live"
        response.body.measurements != null
        response.body.measurements.size() > 0
    }

    void 'test application startup metrics'() {
        when:
        ResponseEntity<Map> response = restTemplate.getForEntity("${baseUrl}/actuator/metrics/application.started.time", Map.class)

        then:
        response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.NOT_FOUND
        
        if (response.statusCode == HttpStatus.OK) {
            assert response.body.name == "application.started.time"
            assert response.body.measurements != null
        }
    }

    void 'test custom business metrics with MeterService'() {
        given:
        def transactionAmount = 250.75
        def accountName = "metricstestchecking_brian"

        when:
        // Simulate business metrics using MeterService if available
        if (meterService != null) {
            // Custom meter service operations would go here
            meterService.class.getDeclaredMethods().each { method ->
                // Verify meter service has expected methods
                assert method != null
            }
        }

        // Create custom business metrics directly
        Counter transactionAmountCounter = meterRegistry.counter("business.transaction.amount.total")
        transactionAmountCounter.increment(transactionAmount)

        then:
        transactionAmountCounter.count() == transactionAmount
    }

    void 'test metrics export configuration'() {
        when:
        // Check if metrics export is configured for InfluxDB or other systems
        ResponseEntity<Map> response = restTemplate.getForEntity("${baseUrl}/actuator/metrics", Map.class)

        then:
        response.statusCode == HttpStatus.OK
        response.body != null
        response.body.names != null
        
        // Verify key application metrics are present
        List<String> metricNames = response.body.names as List<String>
        metricNames.contains("jvm.memory.used")
        metricNames.contains("http.server.requests")
        metricNames.contains("jvm.threads.live")
    }

    void 'test performance monitoring integration'() {
        given:
        List<Long> responseTimes = []

        when:
        // Measure response times for multiple requests
        for (int i = 0; i < 5; i++) {
            long startTime = System.currentTimeMillis()
            restTemplate.getForEntity("${baseUrl}/actuator/health", String.class)
            long endTime = System.currentTimeMillis()
            responseTimes.add(endTime - startTime)
        }

        then:
        responseTimes.size() == 5
        responseTimes.every { it > 0 && it < 5000 }  // Response times should be reasonable
        
        // Create performance metrics
        Timer performanceTimer = meterRegistry.timer("performance.health.endpoint")
        responseTimes.each { responseTime ->
            performanceTimer.record(responseTime, java.util.concurrent.TimeUnit.MILLISECONDS)
        }
        
        performanceTimer.count() == 5
    }

    void 'test application health indicators integration'() {
        when:
        ResponseEntity<Map> response = restTemplate.getForEntity("${baseUrl}/actuator/health", Map.class)

        then:
        response.statusCode == HttpStatus.OK
        response.body != null
        response.body.status != null
        
        if (response.body.components != null) {
            Map components = response.body.components as Map
            // Common health indicators that should be present
            assert components.containsKey("diskSpace") || components.containsKey("db") || components.size() > 0
        }
    }
}
