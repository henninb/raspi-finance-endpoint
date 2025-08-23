package finance.controllers

import finance.domain.Category
import finance.services.CategoryService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification
import spock.lang.Subject

class CategoryControllerSpec extends Specification {

    CategoryService categoryService = GroovyMock(CategoryService)

    @Subject
    CategoryController controller = new CategoryController(categoryService)

    def "categories returns list when found"() {
        given:
        List<Category> categories = [new Category(categoryId: 1L, activeStatus: true, categoryName: 'food')]

        when:
        ResponseEntity<List<Category>> response = controller.categories()

        then:
        1 * categoryService.categories() >> categories
        response.statusCode == HttpStatus.OK
        response.body == categories
    }

    def "categories throws NOT_FOUND when empty"() {
        when:
        controller.categories()

        then:
        1 * categoryService.categories() >> []
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
        ex.reason == 'No categories found'
    }

    def "category returns entity when present, else NOT_FOUND"() {
        given:
        String name = 'groceries'
        Category cat = new Category(categoryId: 5L, activeStatus: true, categoryName: name)

        when:
        ResponseEntity<Category> response = controller.category(name)

        then:
        1 * categoryService.category(name) >> Optional.of(cat)
        response.statusCode == HttpStatus.OK
        response.body == cat

        when:
        controller.category('missing')

        then:
        1 * categoryService.category('missing') >> Optional.empty()
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
        ex.reason == 'Category not found: missing'
    }

    def "updateCategory returns OK or maps errors"() {
        given:
        String name = 'home'
        Category patch = new Category(categoryId: 1L, activeStatus: true, categoryName: name)
        Category updated = new Category(categoryId: 1L, activeStatus: true, categoryName: name)

        when:
        ResponseEntity<Category> response = controller.updateCategory(name, patch)

        then:
        1 * categoryService.findByCategoryName(name) >> Optional.of(patch)
        1 * categoryService.updateCategory(patch) >> updated
        response.statusCode == HttpStatus.OK
        response.body == updated

        when:
        controller.updateCategory('unknown', patch)

        then:
        1 * categoryService.findByCategoryName('unknown') >> Optional.empty()
        0 * categoryService.updateCategory(_)
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
        ex.reason == 'Category not found: unknown'
    }

    def "insertCategory should create category successfully"() {
        given:
        Category input = new Category(categoryId: 0L, activeStatus: true, categoryName: 'new')
        Category created = new Category(categoryId: 1L, activeStatus: true, categoryName: 'new')

        when:
        ResponseEntity<Category> response = controller.insertCategory(input)

        then:
        1 * categoryService.insertCategory(input) >> created
        response.statusCode == HttpStatus.CREATED
        response.body == created
    }

    def "insertCategory maps DataIntegrityViolationException to CONFLICT"() {
        given:
        Category input = new Category(categoryId: 0L, activeStatus: true, categoryName: 'dupe')

        when:
        controller.insertCategory(input)

        then:
        1 * categoryService.insertCategory(input) >> { throw new DataIntegrityViolationException('duplicate') }
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.CONFLICT
        ex.reason == 'Duplicate category found.'
    }

    def "deleteCategory returns OK when found else NOT_FOUND"() {
        when:
        ResponseEntity<Category> response = controller.deleteCategory('to-delete')

        then:
        1 * categoryService.findByCategoryName('to-delete') >> Optional.of(new Category(categoryId: 10L, activeStatus: true, categoryName: 'to-delete'))
        1 * categoryService.deleteCategory('to-delete')
        response.statusCode == HttpStatus.OK

        when:
        controller.deleteCategory('nope')

        then:
        1 * categoryService.findByCategoryName('nope') >> Optional.empty()
        0 * categoryService.deleteCategory(_)
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
        ex.reason == 'Category not found: nope'
    }
}

