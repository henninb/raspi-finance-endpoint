package finance.configurations

import org.slf4j.MDC
import spock.lang.Specification

class SqlQueryLoggingInterceptorSpec extends Specification {

    SqlQueryLoggingInterceptor interceptor

    def setup() {
        interceptor = new SqlQueryLoggingInterceptor()
        MDC.clear()
    }

    def cleanup() {
        MDC.clear()
    }

    def "inspect returns SQL wrapped with QueryID comment"() {
        given:
        String sql = "SELECT * FROM accounts"

        when:
        String result = interceptor.inspect(sql)

        then:
        result.contains("/* QueryID:")
        result.contains("SELECT * FROM accounts")
        result.startsWith("/* QueryID:")
    }

    def "inspect uses correlationId from MDC when present"() {
        given:
        MDC.put("correlationId", "my-trace-id")
        String sql = "SELECT * FROM transactions"

        when:
        String result = interceptor.inspect(sql)

        then:
        result.contains("my-trace-id")
    }

    def "inspect uses N/A when no correlationId in MDC"() {
        given:
        MDC.remove("correlationId")
        String sql = "SELECT id FROM accounts"

        when:
        String result = interceptor.inspect(sql)

        then:
        result.contains("N/A")
    }

    def "inspect masks password values in SQL"() {
        given:
        String sql = "UPDATE users SET password='supersecret' WHERE id=1"

        when:
        String result = interceptor.inspect(sql)

        then:
        result.contains("password='***'")
        !result.contains("supersecret")
    }

    def "inspect masks token values in SQL"() {
        given:
        String sql = "INSERT INTO sessions (token='abc123xyz') WHERE user_id=5"

        when:
        String result = interceptor.inspect(sql)

        then:
        result.contains("token='***'")
        !result.contains("abc123xyz")
    }

    def "inspect masks secret values in SQL"() {
        given:
        String sql = "UPDATE config SET secret='mysecretvalue' WHERE id=1"

        when:
        String result = interceptor.inspect(sql)

        then:
        result.contains("secret='***'")
        !result.contains("mysecretvalue")
    }

    def "inspect produces unique QueryIDs for each call"() {
        given:
        String sql = "SELECT 1"

        when:
        String result1 = interceptor.inspect(sql)
        String result2 = interceptor.inspect(sql)

        then:
        result1 != result2
    }

    def "inspect handles empty SQL string"() {
        given:
        String sql = ""

        when:
        String result = interceptor.inspect(sql)

        then:
        noExceptionThrown()
        result.contains("/* QueryID:")
    }

    def "inspect case-insensitively masks password"() {
        given:
        String sql = "UPDATE users SET PASSWORD='MyPass123' WHERE id=1"

        when:
        String result = interceptor.inspect(sql)

        then:
        !result.contains("MyPass123")
    }
}
