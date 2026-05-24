package finance.controller

import com.fasterxml.jackson.databind.ObjectMapper
import finance.Application
import finance.config.TestSecurityConfig
import finance.domain.Account
import finance.domain.AccountType
import finance.repositories.AccountRepository
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.core.env.Environment
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import groovy.util.logging.Slf4j
import org.spockframework.spring.EnableSharedInjection
import spock.lang.Shared
import spock.lang.Specification

import javax.crypto.SecretKey
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

@Slf4j
@ActiveProfiles("perf")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import([TestSecurityConfig])
@EnableSharedInjection
class AccountControllerPerfSpec extends Specification {

    static final int THREAD_COUNT = 10
    static final int TOTAL_REQUESTS = 200
    static final int WARMUP_REQUESTS = 20
    static final long P95_THRESHOLD_MS = 500
    static final long P99_THRESHOLD_MS = 1000

    @Shared
    @Autowired
    AccountRepository accountRepository

    @Shared
    @Autowired
    Environment environment

    @LocalServerPort
    int port

    @Shared
    String username = "perf_user"

    @Shared
    ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()

    @Shared
    org.springframework.web.client.RestTemplate restTemplate

    @Shared
    List<String> seedAccountNames = [
        "checking_perf", "savings_perf", "amex_perf", "chase_perf", "discover_perf",
        "capital_perf", "citi_perf", "wells_perf", "barclays_perf", "usbank_perf"
    ]

    String jwtToken

    def setupSpec() {
        restTemplate = new org.springframework.web.client.RestTemplate()
        restTemplate.errorHandler = new org.springframework.web.client.NoOpResponseErrorHandler()

        seedAccountNames.eachWithIndex { name, i ->
            Account account = new Account()
            account.accountNameOwner = name
            account.accountType = (i % 2 == 0) ? AccountType.Credit : AccountType.Debit
            account.owner = username
            accountRepository.save(account)
        }
    }

    private static String randomAlphaName(String prefix) {
        String letters = "abcdefghijklmnopqrstuvwxyz"
        Random rng = new Random()
        String suffix = (1..8).collect { letters[rng.nextInt(26)] }.join()
        "${prefix}_${suffix}"
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

    private List<Long> runConcurrent(Closure<ResponseEntity> requestFn) {
        List<Long> latencies = Collections.synchronizedList([])
        AtomicInteger errors = new AtomicInteger(0)
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT)
        CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT)
        int requestsPerThread = TOTAL_REQUESTS / THREAD_COUNT
        List<Future> futures = []

        THREAD_COUNT.times {
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

    private static Map<String, Long> percentiles(List<Long> sorted) {
        [
            p50: sorted[(int) (sorted.size() * 0.50)],
            p95: sorted[(int) (sorted.size() * 0.95)],
            p99: sorted[(int) (sorted.size() * 0.99)],
            max: sorted.last(),
            min: sorted.first(),
        ]
    }

    private static void printReport(String label, List<Long> sorted, long wallMs) {
        def p = percentiles(sorted)
        double rps = TOTAL_REQUESTS / (wallMs / 1000.0)
        log.info("\n=== ${label} ===")
        log.info("  threads=${THREAD_COUNT}  requests=${TOTAL_REQUESTS}  wall=${wallMs}ms  throughput=${String.format('%.1f', rps)} req/sec")
        log.info("  min=${p.min}ms  p50=${p.p50}ms  p95=${p.p95}ms  p99=${p.p99}ms  max=${p.max}ms")
    }

    void "GET /api/account/active - concurrent list latency"() {
        given:
        WARMUP_REQUESTS.times {
            restTemplate.exchange(url("/api/account/active"), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class)
        }

        when:
        long wallStart = System.currentTimeMillis()
        List<Long> latencies = runConcurrent {
            restTemplate.exchange(url("/api/account/active"), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class)
        }
        printReport("GET /api/account/active", latencies, System.currentTimeMillis() - wallStart)

        then:
        def p = percentiles(latencies)
        p.p95 < P95_THRESHOLD_MS
        p.p99 < P99_THRESHOLD_MS
    }

    void "GET /api/account/active/paged - concurrent paginated list latency"() {
        given:
        WARMUP_REQUESTS.times {
            restTemplate.exchange(url("/api/account/active/paged?page=0&size=10"), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class)
        }

        when:
        long wallStart = System.currentTimeMillis()
        List<Long> latencies = runConcurrent {
            restTemplate.exchange(url("/api/account/active/paged?page=0&size=10"), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class)
        }
        printReport("GET /api/account/active/paged", latencies, System.currentTimeMillis() - wallStart)

        then:
        def p = percentiles(latencies)
        p.p95 < P95_THRESHOLD_MS
        p.p99 < P99_THRESHOLD_MS
    }

    void "GET /api/account/{accountNameOwner} - concurrent point lookup latency"() {
        given:
        WARMUP_REQUESTS.times {
            restTemplate.exchange(url("/api/account/checking_perf"), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class)
        }

        when:
        AtomicInteger counter = new AtomicInteger(0)
        long wallStart = System.currentTimeMillis()
        List<Long> latencies = runConcurrent {
            String name = seedAccountNames[counter.getAndIncrement() % seedAccountNames.size()]
            restTemplate.exchange(url("/api/account/${name}"), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class)
        }
        printReport("GET /api/account/{accountNameOwner}", latencies, System.currentTimeMillis() - wallStart)

        then:
        def p = percentiles(latencies)
        p.p95 < P95_THRESHOLD_MS
        p.p99 < P99_THRESHOLD_MS
    }

    void "GET /api/account/totals - concurrent aggregate latency"() {
        given:
        WARMUP_REQUESTS.times {
            restTemplate.exchange(url("/api/account/totals"), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class)
        }

        when:
        long wallStart = System.currentTimeMillis()
        List<Long> latencies = runConcurrent {
            restTemplate.exchange(url("/api/account/totals"), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class)
        }
        printReport("GET /api/account/totals", latencies, System.currentTimeMillis() - wallStart)

        then:
        def p = percentiles(latencies)
        p.p95 < P95_THRESHOLD_MS
        p.p99 < P99_THRESHOLD_MS
    }

    void "POST /api/account - concurrent write latency"() {
        given:
        WARMUP_REQUESTS.times {
            Account account = new Account()
            account.accountNameOwner = randomAlphaName("warm")
            account.accountType = AccountType.Credit
            restTemplate.exchange(url("/api/account"), HttpMethod.POST,
                new HttpEntity<>(mapper.writeValueAsString(account), authHeaders()), String.class)
        }

        when:
        long wallStart = System.currentTimeMillis()
        List<Long> latencies = runConcurrent {
            Account account = new Account()
            account.accountNameOwner = randomAlphaName("new")
            account.accountType = AccountType.Credit
            restTemplate.exchange(url("/api/account"), HttpMethod.POST,
                new HttpEntity<>(mapper.writeValueAsString(account), authHeaders()), String.class)
        }
        printReport("POST /api/account", latencies, System.currentTimeMillis() - wallStart)

        then:
        def p = percentiles(latencies)
        p.p95 < P95_THRESHOLD_MS
        p.p99 < P99_THRESHOLD_MS
    }

    void "GET /api/account/payment/required - concurrent payment query latency"() {
        given:
        WARMUP_REQUESTS.times {
            restTemplate.exchange(url("/api/account/payment/required"), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class)
        }

        when:
        long wallStart = System.currentTimeMillis()
        List<Long> latencies = runConcurrent {
            restTemplate.exchange(url("/api/account/payment/required"), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class)
        }
        printReport("GET /api/account/payment/required", latencies, System.currentTimeMillis() - wallStart)

        then:
        def p = percentiles(latencies)
        p.p95 < P95_THRESHOLD_MS
        p.p99 < P99_THRESHOLD_MS
    }
}
