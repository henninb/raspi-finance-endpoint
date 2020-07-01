package finance.helpers


import finance.domain.Category

import java.sql.Timestamp

class CategoryBuilder {
    String categoryName = "foo"
    Timestamp dateUpdated = new Timestamp(1553645394000)
    Timestamp dateAdded = new Timestamp(1553645394000)

    static CategoryBuilder builder() {
        return new CategoryBuilder()
    }

    Category build() {
        Category category = new Category()
        category.category = categoryName
        //category.dateAdded = dateAdded
        //category.dateUpdated = dateUpdated
        return category
    }

    CategoryBuilder category(category) {
        this.categoryName = category
        return this
    }
}
