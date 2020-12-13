package finance

import finance.domain.Category
import finance.services.CategoryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@ActiveProfiles("ora")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DatabaseInsertSpec extends Specification {

    @Autowired
    protected CategoryService categoryService

    void 'test category - valid insert'() {
        given:
        Category category = new Category()
        category.category = UUID.randomUUID().toString().substring(0,8)
        category.activeStatus = false

        when:
        def result = categoryService.insertCategory(category)

        then:
        result
        0 * _
    }
}
