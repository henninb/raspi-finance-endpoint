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
        return new CategoryBuilder()
    }

    Category build() {
        Category category = new Category().with {
            categoryId = this.categoryId
            categoryName = this.categoryName
            activeStatus = this.activeStatus
            dateUpdated = this.dateUpdated
            categoryCount = this.categoryCount
            return it
        }
        return category
    }

    CategoryBuilder withCategoryId(Long categoryId) {
        this.categoryId = categoryId
        return this
    }

    CategoryBuilder withCategory(String category) {
        this.categoryName = category
        return this
    }

    CategoryBuilder withCategoryName(String categoryName) {
        this.categoryName = categoryName
        return this
    }

    CategoryBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }

    CategoryBuilder withDateUpdated(Timestamp dateUpdated) {
        this.dateUpdated = dateUpdated
        return this
    }

    CategoryBuilder withCategoryCount(Long categoryCount) {
        this.categoryCount = categoryCount
        return this
    }
}
