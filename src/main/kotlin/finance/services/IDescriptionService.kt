package finance.services

import finance.domain.Description
import java.util.*

interface IDescriptionService {
    fun insertDescription(description: Description): Description

    fun deleteByDescriptionName(descriptionName: String): Boolean

    fun fetchAllDescriptions(): List<Description>

    fun findByDescriptionName(descriptionName: String): Optional<Description>

    fun description(descriptionName: String): Optional<Description>

    fun updateDescription(description: Description): Description

    fun mergeDescriptions(targetName: String, sourceNames: List<String>): Description
}
