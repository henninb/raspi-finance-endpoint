//package finance.controllers.resolver
//
//import com.coxautodev.graphql.tools.GraphQLResolver
//import finance.domain.Description
//import finance.services.DescriptionService
//import org.springframework.stereotype.Component
//
//@Component
//class DescriptionResolver (
//    private val descriptionService: DescriptionService
//) : GraphQLResolver<Description> {
//
//    fun descriptions(activeStatus: Boolean): List<Description> {
//        return descriptionService.fetchAllDescriptions()
//    }
//}
