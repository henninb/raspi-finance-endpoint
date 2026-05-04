package finance.configurations

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import spock.lang.Specification

import javax.sql.DataSource

class FunctionalTestBootstrapSpec extends Specification {

    DataSource dataSource = Mock(DataSource)

    def "init calls flyway migrate when flyway bean is provided"() {
        given:
        Flyway flyway = Mock(Flyway)
        flyway.migrate() >> Mock(MigrateResult)
        def bootstrap = new FunctionalTestBootstrap(flyway, dataSource)

        when:
        bootstrap.init()

        then:
        1 * flyway.migrate()
    }

    def "init handles flyway migrate exception gracefully when flyway bean present"() {
        given:
        Flyway flyway = Mock(Flyway)
        flyway.migrate() >> { throw new RuntimeException("migration failed") }
        def bootstrap = new FunctionalTestBootstrap(flyway, dataSource)

        when:
        bootstrap.init()

        then:
        noExceptionThrown()
    }

    def "init with null flyway handles datasource exception gracefully"() {
        given:
        dataSource.getConnection() >> { throw new Exception("no connection available") }
        def bootstrap = new FunctionalTestBootstrap(null, dataSource)

        when:
        bootstrap.init()

        then:
        noExceptionThrown()
    }

    def "init with null flyway attempts programmatic flyway setup"() {
        given:
        def bootstrap = new FunctionalTestBootstrap(null, dataSource)

        when:
        bootstrap.init()

        then:
        noExceptionThrown()
    }
}
