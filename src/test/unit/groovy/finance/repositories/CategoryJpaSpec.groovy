package finance.repositories

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@ActiveProfiles("local")
@DataJpaTest
class CategoryJpaSpec extends Specification {
}