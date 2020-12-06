package finance.helpers

import finance.domain.Category

class CategoryBuilder {
    String categoryName = 'foo'
    Boolean activeStatus = true

    static CategoryBuilder builder() {
        return new CategoryBuilder()
    }

    Category build() {
        Category category = new Category()
        category.category = categoryName
        category.activeStatus = activeStatus
        return category
    }

    CategoryBuilder category(String category) {
        this.categoryName = category
        return this
    }

    CategoryBuilder activeStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }
}
