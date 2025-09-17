package finance.services

import finance.domain.Category
import finance.helpers.CategoryBuilder

class CategoryServiceAdditionalSpec extends BaseServiceSpec {

    void setup() {
        categoryService.validator = validatorMock
        categoryService.meterService = meterService
    }

    void "categories sets categoryCount from repository and returns list"() {
        given:
        def c1 = CategoryBuilder.builder().withCategory('food').build()
        def c2 = CategoryBuilder.builder().withCategory('travel').build()

        when:
        def result = categoryService.categories()

        then:
        1 * categoryRepositoryMock.findByActiveStatusOrderByCategoryName(true) >> [c1, c2]
        1 * transactionRepositoryMock.countByCategoryName('food') >> 3L
        1 * transactionRepositoryMock.countByCategoryName('travel') >> 1L
        result*.categoryName == ['food','travel']
        result[0].categoryCount == 3L
        result[1].categoryCount == 1L
    }

    void "category returns Optional present and empty"() {
        given:
        def c = CategoryBuilder.builder().withCategory('utilities').build()

        when:
        def present = categoryService.category('utilities')
        def empty = categoryService.category('missing')

        then:
        1 * categoryRepositoryMock.findByCategoryName('utilities') >> Optional.of(c)
        1 * categoryRepositoryMock.findByCategoryName('missing') >> Optional.empty()
        present.isPresent()
        empty.isEmpty()
    }

    void "findByCategoryName proxies to repo"() {
        given:
        def c = CategoryBuilder.builder().withCategory('home').build()

        when:
        def res = categoryService.findByCategoryName('home')

        then:
        1 * categoryRepositoryMock.findByCategoryName('home') >> Optional.of(c)
        res.isPresent()
    }

    void "deleteCategory returns false when not found"() {
        when:
        def deleted = categoryService.deleteCategory('missing')

        then:
        1 * categoryRepositoryMock.findByCategoryName('missing') >> Optional.empty()
        !deleted
    }

    void "mergeCategories throws when a category missing"() {
        when:
        categoryService.mergeCategories('keep','missing')

        then:
        1 * categoryRepositoryMock.findByCategoryName('keep') >> Optional.of(CategoryBuilder.builder().withCategory('keep').build())
        1 * categoryRepositoryMock.findByCategoryName('missing') >> Optional.empty()
        thrown(RuntimeException)
    }

    void "mergeCategories success reassigns transactions and deactivates source count merged"() {
        given:
        def keep = finance.helpers.CategoryBuilder.builder().withCategory('keep').build()
        keep.categoryCount = 3
        def drop = finance.helpers.CategoryBuilder.builder().withCategory('drop').build()
        drop.categoryCount = 2
        def t1 = finance.helpers.TransactionBuilder.builder().withCategory('drop').build()
        def t2 = finance.helpers.TransactionBuilder.builder().withCategory('drop').build()

        when:
        def merged = categoryService.mergeCategories('keep','drop')

        then:
        1 * categoryRepositoryMock.findByCategoryName('keep') >> Optional.of(keep)
        1 * categoryRepositoryMock.findByCategoryName('drop') >> Optional.of(drop)
        1 * transactionRepositoryMock.findByCategoryAndActiveStatusOrderByTransactionDateDesc('drop', true) >> [t1, t2]
        2 * transactionRepositoryMock.saveAndFlush({ it.category == 'keep' }) >> { it[0] }
        1 * categoryRepositoryMock.saveAndFlush({ it.is(keep) && it.categoryCount == 5 }) >> { it[0] }
        merged.is(keep)
        !drop.activeStatus
    }
}
