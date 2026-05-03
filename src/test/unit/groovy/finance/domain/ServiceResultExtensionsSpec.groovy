package finance.domain

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Specification
import spock.lang.Unroll

class ServiceResultExtensionsSpec extends Specification {

    def "toOkResponse should return 200 OK for Success"() {
        given:
        def data = "test data"
        def result = ServiceResult.Success.of(data)

        when:
        ResponseEntity<String> response = ServiceResultExtensionsKt.toOkResponse(result)

        then:
        response.statusCode == HttpStatus.OK
        response.body == data
    }

    @Unroll
    def "toOkResponse should return #expectedStatus for #resultType"() {
        when:
        ResponseEntity response = ServiceResultExtensionsKt.toOkResponse(result)

        then:
        response.statusCode == expectedStatus

        where:
        resultType        | result                                                 | expectedStatus
        "NotFound"        | ServiceResult.NotFound.of("not found")                 | HttpStatus.NOT_FOUND
        "ValidationError" | ServiceResult.ValidationError.of([field: "error"])      | HttpStatus.BAD_REQUEST
        "BusinessError"   | ServiceResult.BusinessError.of("error", "code")        | HttpStatus.CONFLICT
        "SystemError"     | ServiceResult.SystemError.of(new Exception())          | HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "toCreatedResponse should return 201 Created for Success"() {
        given:
        def data = "test data"
        def result = ServiceResult.Success.of(data)

        when:
        ResponseEntity<String> response = ServiceResultExtensionsKt.toCreatedResponse(result)

        then:
        response.statusCode == HttpStatus.CREATED
        response.body == data
    }

    @Unroll
    def "toCreatedResponse should return #expectedStatus for #resultType"() {
        when:
        ResponseEntity response = ServiceResultExtensionsKt.toCreatedResponse(result)

        then:
        response.statusCode == expectedStatus

        where:
        resultType        | result                                                 | expectedStatus
        "NotFound"        | ServiceResult.NotFound.of("not found")                 | HttpStatus.NOT_FOUND
        "ValidationError" | ServiceResult.ValidationError.of([field: "error"])      | HttpStatus.BAD_REQUEST
        "BusinessError"   | ServiceResult.BusinessError.of("error", "code")        | HttpStatus.CONFLICT
        "SystemError"     | ServiceResult.SystemError.of(new Exception())          | HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "toListOkResponse should return 200 OK for Success"() {
        given:
        def data = ["item1", "item2"]
        def result = ServiceResult.Success.of(data)

        when:
        ResponseEntity<List<String>> response = ServiceResultExtensionsKt.toListOkResponse(result)

        then:
        response.statusCode == HttpStatus.OK
        response.body == data
    }

    @Unroll
    def "toListOkResponse should return #expectedStatus for #resultType"() {
        when:
        ResponseEntity response = ServiceResultExtensionsKt.toListOkResponse(result)

        then:
        response.statusCode == expectedStatus

        where:
        resultType        | result                                                 | expectedStatus
        "NotFound"        | ServiceResult.NotFound.of("not found")                 | HttpStatus.NOT_FOUND
        "ValidationError" | ServiceResult.ValidationError.of([field: "error"])      | HttpStatus.INTERNAL_SERVER_ERROR
        "BusinessError"   | ServiceResult.BusinessError.of("error", "code")        | HttpStatus.INTERNAL_SERVER_ERROR
        "SystemError"     | ServiceResult.SystemError.of(new Exception())          | HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "toPagedOkResponse should return 200 OK for Success"() {
        given:
        def data = new PageImpl<String>(["item1", "item2"])
        def result = ServiceResult.Success.of(data)
        def pageable = PageRequest.of(0, 10)

        when:
        ResponseEntity<Page<String>> response = ServiceResultExtensionsKt.toPagedOkResponse(result, pageable)

        then:
        response.statusCode == HttpStatus.OK
        response.body == data
    }

    @Unroll
    def "toPagedOkResponse should return #expectedStatus for #resultType"() {
        given:
        def pageable = PageRequest.of(0, 10)

        when:
        ResponseEntity response = ServiceResultExtensionsKt.toPagedOkResponse(result, pageable)

        then:
        response.statusCode == expectedStatus

        where:
        resultType        | result                                                 | expectedStatus
        "NotFound"        | ServiceResult.NotFound.of("not found")                 | HttpStatus.NOT_FOUND
        "ValidationError" | ServiceResult.ValidationError.of([field: "error"])      | HttpStatus.BAD_REQUEST
        "BusinessError"   | ServiceResult.BusinessError.of("error", "code")        | HttpStatus.BAD_REQUEST
        "SystemError"     | ServiceResult.SystemError.of(new Exception())          | HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "unwrapOrThrow should return data for Success"() {
        given:
        def data = "test data"
        def result = ServiceResult.Success.of(data)

        when:
        def unwrapped = ServiceResultExtensionsKt.unwrapOrThrow(result)

        then:
        unwrapped == data
    }

    @Unroll
    def "unwrapOrThrow should throw #expectedException for #resultType"() {
        when:
        ServiceResultExtensionsKt.unwrapOrThrow(result)

        then:
        thrown(expectedException)

        where:
        resultType        | result                                                 | expectedException
        "NotFound"        | ServiceResult.NotFound.of("not found")                 | EntityNotFoundException
        "ValidationError" | ServiceResult.ValidationError.of([field: "error"])      | ValidationException
        "BusinessError"   | ServiceResult.BusinessError.of("error", "code")        | DataIntegrityViolationException
        "SystemError"     | ServiceResult.SystemError.of(new Exception("fail"))    | RuntimeException
    }
}
