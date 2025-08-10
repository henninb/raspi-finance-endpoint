package finance.repositories

import finance.Application
import finance.domain.Description
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ActiveProfiles("int")
@DataJpaTest
@ContextConfiguration(classes = [Application])
class DescriptionJpaSpec extends Specification {
    @Autowired
    protected DescriptionRepository descriptionRepository

    @Autowired
    protected TestEntityManager entityManager

    void 'find a description that does not exist'() {
        when:
        Optional<Description> result = descriptionRepository.findById(0L)

        then:
        result == Optional.empty()
    }
}
