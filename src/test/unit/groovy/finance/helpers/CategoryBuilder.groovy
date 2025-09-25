package finance.helpers

import finance.domain.Category
import java.sql.Timestamp
import java.util.*

class CategoryBuilder {
    Long categoryId = 0L
    String categoryName = 'foo'
    Boolean activeStatus = true
    Timestamp dateUpdated = new Timestamp(Calendar.getInstance().time.time)
    Long categoryCount = 0L

    static CategoryBuilder builder() {
        new CategoryBuilder()
    }

    Category build() {
        Category category = new Category().with {
            categoryId = this.categoryId
            categoryName = this.categoryName
            activeStatus = this.activeStatus
            dateUpdated = this.dateUpdated
            categoryCount = this.categoryCount
            it
        }
        category
    }

    CategoryBuilder withCategoryId(Long categoryId) {
        this.categoryId = categoryId
        this
    }

    CategoryBuilder withCategory(String category) {
        this.categoryName = category
        this
    }

    CategoryBuilder withCategoryName(String categoryName) {
        this.categoryName = categoryName
        this
    }

    CategoryBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        this
    }

    CategoryBuilder withDateUpdated(Timestamp dateUpdated) {
        this.dateUpdated = dateUpdated
        this
    }

    CategoryBuilder withCategoryCount(Long categoryCount) {
        this.categoryCount = categoryCount
        this
    }
}
