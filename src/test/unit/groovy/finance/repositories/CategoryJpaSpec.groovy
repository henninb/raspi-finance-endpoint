package finance.repositories

import finance.domain.Account
import finance.domain.Category
import finance.helpers.AccountBuilder
import finance.helpers.CategoryBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

import javax.persistence.PersistenceException
import javax.validation.ConstraintViolationException

@ActiveProfiles("unit")
@DataJpaTest
class CategoryJpaSpec extends Specification {
    @Autowired
    CategoryRepository categoryRepository

    @Autowired
    TestEntityManager entityManager

    def "test category - valid insert"() {
        given:
        Category category = CategoryBuilder.builder().build()

        when:
        def categoryResult = entityManager.persist(category)

        then:
        categoryRepository.count() == 1L
        categoryResult.category == category.category
        0 * _
    }

    def "test category - valid insert, insert a second category with the same name"() {
        given:
        Category category1 = CategoryBuilder.builder().build()
        Category category2 = CategoryBuilder.builder().build()
        def categoryResult1 = entityManager.persist(category1)
        category2.category = 'second'

        when:
        def categoryResult2 = entityManager.persist(category2)

        then:
        categoryRepository.count() == 2L
        categoryResult2.category == category2.category
        categoryResult1.category == category1.category
        0 * _
    }

    def "test category - empty category insert"() {
        given:
        Category category = CategoryBuilder.builder().build()
        category.category = ''

        when:
        entityManager.persist(category)

        then:
        ConstraintViolationException ex = thrown()
        ex.getMessage().contains('Validation failed for classes [finance.domain.Category]')
        0 * _
    }

    def "test category - invalid category insert"() {
        given:
        Category category = CategoryBuilder.builder().build()
        category.category = 'add a space'

        when:
        entityManager.persist(category)

        then:
        ConstraintViolationException ex = thrown()
        ex.getMessage().contains('Validation failed for classes [finance.domain.Category]')
        0 * _
    }

    def "test category - capital letter category insert"() {
        given:
        Category category = CategoryBuilder.builder().build()
        category.category = 'Space'

        when:
        entityManager.persist(category)

        then:
        ConstraintViolationException ex = thrown()
        ex.getMessage().contains('Validation failed for classes [finance.domain.Category]')
        0 * _
    }
}