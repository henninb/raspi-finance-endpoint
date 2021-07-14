package finance.controllers.resolver

import finance.domain.Description
import finance.services.DescriptionService
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
class DescriptionQueryResolver( private val descriptionService: DescriptionService ) : GraphQLQueryResolver {
    @Suppress("unused")
    fun descriptions() : List<Description> {
        return descriptionService.fetchAllDescriptions()
    }
}
