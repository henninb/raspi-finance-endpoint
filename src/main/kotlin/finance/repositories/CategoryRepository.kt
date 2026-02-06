package finance.repositories

import finance.domain.Category
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface CategoryRepository : JpaRepository<Category, Long> {
    fun findByCategoryName(categoryName: String): Optional<Category>

    fun findByCategoryId(categoryId: Long): Optional<Category>

    fun findByActiveStatusOrderByCategoryName(activeStatus: Boolean): List<Category>

    // Paginated query for active categories
    fun findAllByActiveStatusOrderByCategoryName(
        activeStatus: Boolean,
        pageable: Pageable,
    ): Page<Category>

    // --- Owner-scoped methods for multi-tenancy (Phase 4) ---

    fun findByOwnerAndCategoryName(
        owner: String,
        categoryName: String,
    ): Optional<Category>

    fun findByOwnerAndCategoryId(
        owner: String,
        categoryId: Long,
    ): Optional<Category>

    fun findByOwnerAndActiveStatusOrderByCategoryName(
        owner: String,
        activeStatus: Boolean,
    ): List<Category>

    fun findAllByOwnerAndActiveStatusOrderByCategoryName(
        owner: String,
        activeStatus: Boolean,
        pageable: Pageable,
    ): Page<Category>
}
