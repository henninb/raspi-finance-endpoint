package finance.repositories

import finance.Application
import finance.domain.Category
import finance.helpers.SmartCategoryBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

import jakarta.validation.ConstraintViolationException

@ActiveProfiles("func")
@DataJpaTest
@ContextConfiguration(classes = [Application])
class CategoryJpaSpec extends Specification {
    @Autowired
    protected CategoryRepository categoryRepository

    @Autowired
    protected TestEntityManager entityManager

    void 'test category - valid insert'() {
        given:
        long before = categoryRepository.count()
        Category category = SmartCategoryBuilder.builderForOwner('brian')
            .withUniqueCategoryName('catvalid')
            .asActive()
            .buildAndValidate()

        when:
        Category categoryResult = entityManager.persist(category)

        then:
        categoryRepository.count() == before + 1
        categoryResult.categoryName == category.categoryName
        0 * _
    }

    void 'test category - valid insert, insert a second category with the same name'() {
        given:
        long before = categoryRepository.count()
        Category category1 = SmartCategoryBuilder.builderForOwner('brian')
            .withUniqueCategoryName('catsame')
            .asActive()
            .buildAndValidate()
        Category categoryResult1 = entityManager.persist(category1)

        Category category2 = SmartCategoryBuilder.builderForOwner('brian')
            .withCategoryName(category1.categoryName)
            .asActive()
            .build()

        when:
        Category categoryResult2 = entityManager.persist(category2)

        then:
        categoryRepository.count() >= before + 1
        categoryRepository.count() <= before + 2
        categoryResult2.categoryName == category2.categoryName
        categoryResult1.categoryName == category1.categoryName
        0 * _
    }

    void 'test category - empty category insert'() {
        given:
        Category category = SmartCategoryBuilder.builderForOwner('brian')
            .withUniqueCategoryName('emptycat')
            .build()
        category.categoryName = ''

        when:
        entityManager.persist(category)

        then:
        ConstraintViolationException ex = thrown()
        ex.message.contains('Validation failed for classes [finance.domain.Category]')
        0 * _
    }

    void 'test category - invalid category insert'() {
        given:
        Category category = SmartCategoryBuilder.builderForOwner('brian')
            .withUniqueCategoryName('invalidcat')
            .build()
        category.categoryName = 'add a space'

        when:
        entityManager.persist(category)

        then:
        ConstraintViolationException ex = thrown()
        ex.message.contains('Validation failed for classes [finance.domain.Category]')
        0 * _
    }

    void 'test category - capital letter category insert'() {
        given:
        Category category = SmartCategoryBuilder.builderForOwner('brian')
            .withUniqueCategoryName('capcat')
            .build()
        category.categoryName = 'Space'

        when:
        entityManager.persist(category)

        then:
        ConstraintViolationException ex = thrown()
        ex.message.contains('Validation failed for classes [finance.domain.Category]')
        0 * _
    }
}
