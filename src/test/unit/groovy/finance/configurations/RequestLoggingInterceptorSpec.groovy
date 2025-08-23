package finance.configurations

import org.springframework.web.servlet.ModelAndView
import spock.lang.Specification

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class RequestLoggingInterceptorSpec extends Specification {

    RequestLoggingInterceptor requestLoggingInterceptor = new RequestLoggingInterceptor()

    def "preHandle should log graphql requests"() {
        given:
        def request = Mock(HttpServletRequest)
        def response = Mock(HttpServletResponse)
        def handler = new Object()

        when:
        def result = requestLoggingInterceptor.preHandle(request, response, handler)

        then:
        2 * request.getRequestURI() >> "/graphql"
        result == true
    }

    def "preHandle should not log non-graphql requests"() {
        given:
        def request = Mock(HttpServletRequest)
        def response = Mock(HttpServletResponse)
        def handler = new Object()

        when:
        def result = requestLoggingInterceptor.preHandle(request, response, handler)

        then:
        1 * request.getRequestURI() >> "/api/test"
        0 * _
        result == true
    }

    def "postHandle should log graphql responses"() {
        given:
        def request = Mock(HttpServletRequest)
        def response = Mock(HttpServletResponse)
        def handler = new Object()
        def modelAndView = Mock(ModelAndView)

        when:
        requestLoggingInterceptor.postHandle(request, response, handler, modelAndView)

        then:
        1 * request.getRequestURI() >> "/graphql"
        1 * response.getStatus() >> 200
    }

    def "postHandle should not log non-graphql responses"() {
        given:
        def request = Mock(HttpServletRequest)
        def response = Mock(HttpServletResponse)
        def handler = new Object()
        def modelAndView = Mock(ModelAndView)

        when:
        requestLoggingInterceptor.postHandle(request, response, handler, modelAndView)

        then:
        1 * request.getRequestURI() >> "/api/test"
        0 * _
    }
}
