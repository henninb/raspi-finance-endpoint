package finance.controllers.resolver

import finance.domain.Category
import finance.services.CategoryService
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
class CategoryQueryResolver(private val categoryService: CategoryService) : GraphQLQueryResolver {
    @Suppress("unused")
    fun categories() : List<Category> {
        return categoryService.fetchAllActiveCategories()
    }
}
