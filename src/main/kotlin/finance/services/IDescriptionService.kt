package finance.services

import finance.domain.Description
import java.util.*

interface IDescriptionService {
    fun insertDescription(description: Description): Description

    fun deleteByDescriptionName(descriptionName: String): Boolean

    fun fetchAllDescriptions(): List<Description>

    fun findByDescriptionName(descriptionName: String): Optional<Description>
}
