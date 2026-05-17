package finance.controller

import com.fasterxml.jackson.databind.ObjectMapper
import finance.Application
import finance.config.TestSecurityConfig
import finance.domain.Account
import finance.domain.AccountType
import finance.domain.ReoccurringType
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.helpers.TransactionBuilder
import finance.repositories.AccountRepository
import finance.repositories.TransactionRepository
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.core.env.Environment
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.spockframework.spring.EnableSharedInjection
import spock.lang.Shared
import spock.lang.Specification

import javax.crypto.SecretKey
import java.time.LocalDate
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

@ActiveProfiles("perf")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import([TestSecurityConfig])
@EnableSharedInjection
class TransactionControllerPerfSpec extends Specification {

    static final int THREAD_COUNT = 10
    static final int TOTAL_REQUESTS = 200
    static final int WARMUP_REQUESTS = 20
    static final int HIGH_THREAD_COUNT = 20
    static final long P95_THRESHOLD_MS = 500
    static final long P99_THRESHOLD_MS = 1000

    @Shared
    @Autowired
    AccountRepository accountRepository

    @Shared
    @Autowired
    TransactionRepository transactionRepository

    @Shared
    @Autowired
    Environment environment

    @LocalServerPort
    int port

    @Shared
    Long testAccountId

    @Shared
    String seedGuid

    @Shared
    String username = "perf_user"

    @Shared
    ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()

    @Shared
    org.springframework.web.client.RestTemplate restTemplate

    String jwtToken

    def setupSpec() {
        restTemplate = new org.springframework.web.client.RestTemplate()
        restTemplate.errorHandler = new org.springframework.web.client.ResponseErrorHandler() {
            boolean hasError(org.springframework.http.client.ClientHttpResponse r) { false }
            void handleError(org.springframework.http.client.ClientHttpResponse r) {}
        }

        Account account = new Account()
        account.accountNameOwner = "perf_brian"
        account.accountType = AccountType.Credit
        account.owner = username
        testAccountId = accountRepository.save(account).accountId

        seedGuid = UUID.randomUUID().toString()
        (1..50).each { i ->
            Transaction txn = new Transaction()
            txn.guid = (i == 1) ? seedGuid : UUID.randomUUID().toString()
            txn.accountId = testAccountId
            txn.accountNameOwner = "perf_brian"
            txn.accountType = AccountType.Credit
            txn.transactionDate = LocalDate.of(2024, 1, 15)
            txn.description = "amazon"
            txn.category = "online"
            txn.amount = new BigDecimal("${i}.99")
            txn.transactionState = TransactionState.Cleared
            txn.reoccurringType = ReoccurringType.Undefined
            txn.notes = "seed_${i}"
            txn.owner = username
            transactionRepository.save(txn)
        }
    }

    def setup() {
        String jwtKey = environment.getProperty("custom.project.jwt.key")
        jwtToken = generateJwtToken(username, jwtKey)
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        headers.add("Authorization", "Bearer " + jwtToken)
        return headers
    }

    private static String generateJwtToken(String user, String jwtKey) {
        SecretKey key = Keys.hmacShaKeyFor(jwtKey.bytes)
        long now = System.currentTimeMillis()
        return Jwts.builder()
            .issuer("raspi-finance-endpoint")
            .audience().add("raspi-finance-endpoint").and()
            .subject(user)
            .claim("username", user)
            .issuedAt(new Date(now))
            .notBefore(new Date(now))
            .expiration(new Date(now + 3600000L))
            .signWith(key)
            .compact()
    }

    private String url(String uri) {
        "http://localhost:${port}${uri}"
    }

    private List<Long> runConcurrent(int threads, Closure<ResponseEntity> requestFn) {
        List<Long> latencies = Collections.synchronizedList([])
        AtomicInteger errors = new AtomicInteger(0)
        ExecutorService pool = Executors.newFixedThreadPool(threads)
        CyclicBarrier barrier = new CyclicBarrier(threads)
        int requestsPerThread = TOTAL_REQUESTS / threads
        List<Future> futures = []

        threads.times {
            futures << pool.submit({
                barrier.await()
                requestsPerThread.times {
                    long start = System.nanoTime()
                    try {
                        ResponseEntity response = requestFn()
                        if (!response.statusCode.is2xxSuccessful()) {
                            errors.incrementAndGet()
                        }
                    } catch (Exception ignored) {
                        errors.incrementAndGet()
                    } finally {
                        latencies << TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
                    }
                }
            } as Callable)
        }

        futures.each { it.get(60, TimeUnit.SECONDS) }
        pool.shutdown()

        assert errors.get() == 0: "${errors.get()} requests failed during load test"
        return latencies.sort()
    }

    private List<Long> runConcurrent(Closure<ResponseEntity> requestFn) {
        return runConcurrent(THREAD_COUNT, requestFn)
    }

    private static Map<String, Long> percentiles(List<Long> sorted) {
        [
            p50: sorted[(int) (sorted.size() * 0.50)],
            p95: sorted[(int) (sorted.size() * 0.95)],
            p99: sorted[(int) (sorted.size() * 0.99)],
            max: sorted.last(),
            min: sorted.first(),
        ]
    }

    private static void printReport(String label, List<Long> sorted, int threads, long wallMs) {
        def p = percentiles(sorted)
        double rps = TOTAL_REQUESTS / (wallMs / 1000.0)
        println "\n=== $label ==="
        println "  threads=${threads}  requests=${TOTAL_REQUESTS}  wall=${wallMs}ms  throughput=${String.format('%.1f', rps)} req/sec"
        println "  min=${p.min}ms  p50=${p.p50}ms  p95=${p.p95}ms  p99=${p.p99}ms  max=${p.max}ms"
    }

    void "GET /api/transaction/active - concurrent read latency"() {
        given:
        WARMUP_REQUESTS.times {
            restTemplate.exchange(url("/api/transaction/active"), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class)
        }

        when:
        long wallStart = System.currentTimeMillis()
        List<Long> latencies = runConcurrent {
            restTemplate.exchange(url("/api/transaction/active"), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class)
        }
        printReport("GET /api/transaction/active", latencies, THREAD_COUNT, System.currentTimeMillis() - wallStart)

        then:
        def p = percentiles(latencies)
        p.p95 < P95_THRESHOLD_MS
        p.p99 < P99_THRESHOLD_MS
    }

    void "GET /api/transaction/{guid} - concurrent point lookup latency"() {
        given:
        WARMUP_REQUESTS.times {
            restTemplate.exchange(url("/api/transaction/${seedGuid}"), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class)
        }

        when:
        long wallStart = System.currentTimeMillis()
        List<Long> latencies = runConcurrent {
            restTemplate.exchange(url("/api/transaction/${seedGuid}"), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class)
        }
        printReport("GET /api/transaction/{guid}", latencies, THREAD_COUNT, System.currentTimeMillis() - wallStart)

        then:
        def p = percentiles(latencies)
        p.p95 < P95_THRESHOLD_MS
        p.p99 < P99_THRESHOLD_MS
    }

    void "GET /api/transaction/account/select/{accountNameOwner} - concurrent account filter latency"() {
        given:
        WARMUP_REQUESTS.times {
            restTemplate.exchange(url("/api/transaction/account/select/perf_brian"), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class)
        }

        when:
        long wallStart = System.currentTimeMillis()
        List<Long> latencies = runConcurrent {
            restTemplate.exchange(url("/api/transaction/account/select/perf_brian"), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class)
        }
        printReport("GET /api/transaction/account/select/{name}", latencies, THREAD_COUNT, System.currentTimeMillis() - wallStart)

        then:
        def p = percentiles(latencies)
        p.p95 < P95_THRESHOLD_MS
        p.p99 < P99_THRESHOLD_MS
    }

    void "GET /api/transaction/account/totals/{accountNameOwner} - concurrent aggregate latency"() {
        given:
        WARMUP_REQUESTS.times {
            restTemplate.exchange(url("/api/transaction/account/totals/perf_brian"), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class)
        }

        when:
        long wallStart = System.currentTimeMillis()
        List<Long> latencies = runConcurrent {
            restTemplate.exchange(url("/api/transaction/account/totals/perf_brian"), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class)
        }
        printReport("GET /api/transaction/account/totals/{name}", latencies, THREAD_COUNT, System.currentTimeMillis() - wallStart)

        then:
        def p = percentiles(latencies)
        p.p95 < P95_THRESHOLD_MS
        p.p99 < P99_THRESHOLD_MS
    }

    void "GET /api/transaction/active/paged - concurrent paginated read latency"() {
        given:
        WARMUP_REQUESTS.times {
            restTemplate.exchange(url("/api/transaction/active/paged?page=0&size=20"), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class)
        }

        when:
        long wallStart = System.currentTimeMillis()
        List<Long> latencies = runConcurrent {
            restTemplate.exchange(url("/api/transaction/active/paged?page=0&size=20"), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class)
        }
        printReport("GET /api/transaction/active/paged", latencies, THREAD_COUNT, System.currentTimeMillis() - wallStart)

        then:
        def p = percentiles(latencies)
        p.p95 < P95_THRESHOLD_MS
        p.p99 < P99_THRESHOLD_MS
    }

    void "GET /api/transaction/date-range - concurrent date range query latency"() {
        given:
        WARMUP_REQUESTS.times {
            restTemplate.exchange(
                url("/api/transaction/date-range?startDate=2020-01-01&endDate=2030-12-31"),
                HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class)
        }

        when:
        long wallStart = System.currentTimeMillis()
        List<Long> latencies = runConcurrent {
            restTemplate.exchange(
                url("/api/transaction/date-range?startDate=2020-01-01&endDate=2030-12-31"),
                HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class)
        }
        printReport("GET /api/transaction/date-range", latencies, THREAD_COUNT, System.currentTimeMillis() - wallStart)

        then:
        def p = percentiles(latencies)
        p.p95 < P95_THRESHOLD_MS
        p.p99 < P99_THRESHOLD_MS
    }

    void "GET /api/transaction/category/{category} - concurrent category filter latency"() {
        given:
        WARMUP_REQUESTS.times {
            restTemplate.exchange(url("/api/transaction/category/online"), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class)
        }

        when:
        long wallStart = System.currentTimeMillis()
        List<Long> latencies = runConcurrent {
            restTemplate.exchange(url("/api/transaction/category/online"), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class)
        }
        printReport("GET /api/transaction/category/{name}", latencies, THREAD_COUNT, System.currentTimeMillis() - wallStart)

        then:
        def p = percentiles(latencies)
        p.p95 < P95_THRESHOLD_MS
        p.p99 < P99_THRESHOLD_MS
    }

    void "POST /api/transaction - concurrent write latency"() {
        given:
        WARMUP_REQUESTS.times {
            def txn = TransactionBuilder.builder().guid(UUID.randomUUID().toString()).accountId(testAccountId).build()
            restTemplate.exchange(url("/api/transaction"), HttpMethod.POST,
                new HttpEntity<>(mapper.writeValueAsString(txn), authHeaders()), String.class)
        }

        when:
        long wallStart = System.currentTimeMillis()
        List<Long> latencies = runConcurrent {
            def txn = TransactionBuilder.builder().guid(UUID.randomUUID().toString()).accountId(testAccountId).build()
            restTemplate.exchange(url("/api/transaction"), HttpMethod.POST,
                new HttpEntity<>(mapper.writeValueAsString(txn), authHeaders()), String.class)
        }
        printReport("POST /api/transaction", latencies, THREAD_COUNT, System.currentTimeMillis() - wallStart)

        then:
        def p = percentiles(latencies)
        p.p95 < P95_THRESHOLD_MS
        p.p99 < P99_THRESHOLD_MS
    }

    void "POST /api/transaction - high-concurrency stress (20 threads)"() {
        given:
        WARMUP_REQUESTS.times {
            def txn = TransactionBuilder.builder().guid(UUID.randomUUID().toString()).accountId(testAccountId).build()
            restTemplate.exchange(url("/api/transaction"), HttpMethod.POST,
                new HttpEntity<>(mapper.writeValueAsString(txn), authHeaders()), String.class)
        }

        when:
        long wallStart = System.currentTimeMillis()
        List<Long> latencies = runConcurrent(HIGH_THREAD_COUNT) {
            def txn = TransactionBuilder.builder().guid(UUID.randomUUID().toString()).accountId(testAccountId).build()
            restTemplate.exchange(url("/api/transaction"), HttpMethod.POST,
                new HttpEntity<>(mapper.writeValueAsString(txn), authHeaders()), String.class)
        }
        printReport("POST /api/transaction (high concurrency)", latencies, HIGH_THREAD_COUNT, System.currentTimeMillis() - wallStart)

        then:
        def p = percentiles(latencies)
        p.p95 < P95_THRESHOLD_MS * 2
        p.p99 < P99_THRESHOLD_MS * 2
    }

    void "mixed 70pct read 30pct write - concurrent latency"() {
        given:
        AtomicInteger counter = new AtomicInteger(0)
        WARMUP_REQUESTS.times {
            restTemplate.exchange(url("/api/transaction/active"), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class)
        }

        when:
        long wallStart = System.currentTimeMillis()
        List<Long> latencies = runConcurrent {
            if (counter.getAndIncrement() % 10 < 7) {
                restTemplate.exchange(url("/api/transaction/active"), HttpMethod.GET,
                    new HttpEntity<>(authHeaders()), String.class)
            } else {
                def txn = TransactionBuilder.builder().guid(UUID.randomUUID().toString()).accountId(testAccountId).build()
                restTemplate.exchange(url("/api/transaction"), HttpMethod.POST,
                    new HttpEntity<>(mapper.writeValueAsString(txn), authHeaders()), String.class)
            }
        }
        printReport("Mixed 70% GET / 30% POST", latencies, THREAD_COUNT, System.currentTimeMillis() - wallStart)

        then:
        def p = percentiles(latencies)
        p.p95 < P95_THRESHOLD_MS
        p.p99 < P99_THRESHOLD_MS
    }
}
