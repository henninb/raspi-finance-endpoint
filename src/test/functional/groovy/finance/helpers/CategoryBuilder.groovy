package finance.helpers

import finance.domain.Category

class CategoryBuilder {
    String categoryName = 'foo'
    Boolean activeStatus = true

    static CategoryBuilder builder() {
        return new CategoryBuilder()
    }

    Category build() {
        Category category = new Category().with {
            category = this.categoryName
            activeStatus = this.activeStatus
            return it
        }
        return category
    }

    CategoryBuilder withCategory(String category) {
        this.categoryName = category
        return this
    }

    CategoryBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }
}
