package finance.resolvers

import finance.services.DescriptionService
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import finance.domain.Description
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.function.Predicate
import java.util.stream.Collectors


@Component
class GraphQLDataFetchers(private val descriptionService: DescriptionService) {
//class GraphQLDataFetcher {

//    @Autowired
//    private val descriptionService: DescriptionService? = null

    val descriptions: DataFetcher<List<Description>>
        get() = DataFetcher<List<Description>> { _x ->
            val listEntities: List<Description> = descriptionService.fetchAllDescriptions()
            listEntities
        }
}