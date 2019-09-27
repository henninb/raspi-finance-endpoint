package finance.configs

//import org.dom4j.dom.DOMNodeHelper.setPrefix
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Description
import org.springframework.context.annotation.Profile
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver


//@Profile("donotuse")
//@Configuration
open class ThymeleafConfiguration {
//
//    @Bean
//    @Description("Thymeleaf template resolver serving HTML 5 transactions")
//    open fun emailTemplateResolver(): ClassLoaderTemplateResolver {
//        val emailTemplateResolver = ClassLoaderTemplateResolver()
//        emailTemplateResolver.prefix = "classpath:/templates/"
//        emailTemplateResolver.suffix = ".html"
//        emailTemplateResolver.setTemplateMode("HTML5")
//        //emailTemplateResolver.characterEncoding = CharEncoding.UTF_8
//        emailTemplateResolver.order = 1
//        return emailTemplateResolver
//    }
}