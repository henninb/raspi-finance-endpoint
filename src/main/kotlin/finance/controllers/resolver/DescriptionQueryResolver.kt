//package finance.controllers.resolver
//
//import com.coxautodev.graphql.tools.GraphQLQueryResolver
//import finance.domain.Description
//import finance.services.DescriptionService
//import org.springframework.stereotype.Component
//
//@Component
//class DescriptionQueryResolver( private val descriptionService: DescriptionService ) : GraphQLQueryResolver {
//    @Suppress("unused")
//    fun descriptions(): List<Description> {
//        return descriptionService.fetchAllDescriptions()
//    }
//}
