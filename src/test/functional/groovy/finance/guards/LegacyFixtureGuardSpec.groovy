package finance.guards

import spock.lang.Specification

class LegacyFixtureGuardSpec extends Specification {

    def "no legacy data.sql or schema.sql in functional resources"() {
        given:
        File functionalResources = new File(System.getProperty("user.dir"), "src/test/functional/resources")

        when:
        List<File> offenders = []
        if (functionalResources.exists()) {
            functionalResources.eachFileRecurse { f ->
                if (f.isFile() && (f.name.equalsIgnoreCase("data.sql") || f.name.equalsIgnoreCase("schema.sql"))) {
                    offenders << f
                }
            }
        }

        then:
        offenders.isEmpty()
    }

    def "functional profile must not reference legacy data.sql"() {
        given:
        File funcYml = new File(System.getProperty("user.dir"), "src/test/functional/resources/application-func.yml")

        expect:
        funcYml.exists()
        String text = funcYml.getText("UTF-8")
        !text.toLowerCase().contains("data.sql")
        !text.contains("spring.sql.init.data-locations")
    }
}

