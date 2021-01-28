package finance.services

import finance.domain.Description
import java.util.*

interface IDescriptionService {
    fun insertDescription(description: Description): Boolean

    fun deleteByDescriptionName(description: String): Boolean

    fun fetchAllDescriptions(): List<Description>

    fun findByDescriptionName(descriptionName: String): Optional<Description>
}
