package finance.services

import finance.domain.Category
import finance.domain.Description
import finance.helpers.CategoryBuilder
import finance.helpers.DescriptionBuilder
import org.w3c.dom.ls.LSResourceResolver
import org.xml.sax.ErrorHandler
import org.xml.sax.SAXException
import spock.lang.Specification

import javax.validation.ConstraintViolation
import javax.validation.Validation
import javax.validation.Validator
import javax.xml.transform.Result
import javax.xml.transform.Source


class DescriptionServiceSpec extends BaseServiceSpec {
    protected DescriptionService descriptionService = new DescriptionService(descriptionRepositoryMock, validatorMock, meterServiceMock)
    //Validator validator = Validation.buildDefaultValidatorFactory().getValidator()

    void setup() {
    }

    void 'test - insert description'() {
        given:
        Description description = DescriptionBuilder.builder().build()
        Set<ConstraintViolation<Description>> constraintViolations = validator.validate(description)

        when:
        descriptionService.insertDescription(description)

        then:
        1 * validatorMock.validate(description) >> constraintViolations
        1 * descriptionRepositoryMock.saveAndFlush(description)
        0 * _
    }
}
