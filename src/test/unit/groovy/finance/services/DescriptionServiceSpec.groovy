package finance.services


import finance.domain.Description
import finance.helpers.DescriptionBuilder
import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidationException

@SuppressWarnings("GroovyAccessibility")
class DescriptionServiceSpec extends BaseServiceSpec {

    void setup() {
        descriptionService.validator = validatorMock
        descriptionService.meterService = meterService
    }

    void 'test - insert description'() {
        given:
        Description description = DescriptionBuilder.builder().build()
        Set<ConstraintViolation<Description>> constraintViolations = validator.validate(description)

        when:
        Description descriptionInserted = descriptionService.insertDescription(description)

        then:
        descriptionInserted.descriptionName == description.descriptionName
        1 * validatorMock.validate(description) >> constraintViolations
        1 * descriptionRepositoryMock.saveAndFlush(description) >> description
        0 * _
    }

    void 'test - insert description - empty descriptionName'() {
        given:
        Description description = DescriptionBuilder.builder().withDescription('').build()

        // Create mock constraint violation
        ConstraintViolation<Description> violation = Mock(ConstraintViolation)
        violation.invalidValue >> ""
        violation.message >> "size must be between 3 and 40"

        Set<ConstraintViolation<Description>> constraintViolations = [violation] as Set

        when:
        descriptionService.insertDescription(description)

        then:
        constraintViolations.size() == 1
        thrown(ValidationException)
        1 * validatorMock.validate(description) >> constraintViolations
        _ * _  // Allow any other interactions (logging, etc.)
    }

    void 'test - delete description'() {
        given:
        Description description = DescriptionBuilder.builder().build()

        when:
        descriptionService.deleteByDescriptionName(description.descriptionName)

        then:
        1 * descriptionRepositoryMock.findByDescriptionName(description.descriptionName) >> Optional.of(description)
        1 * descriptionRepositoryMock.delete(description)
        0 * _
    }
void 'mergeDescriptions creates target if missing, reassigns transactions, deactivates sources'() {
        given:
        String target = 'target'
        List<String> sources = ['s1','s2']

        when:
        Description result = descriptionService.mergeDescriptions(target, sources)

        then:
        1 * descriptionRepositoryMock.findByDescriptionName(target) >> Optional.empty()
        1 * validatorMock.validate(_ as Description) >> ([] as Set)
        1 * descriptionRepositoryMock.saveAndFlush({ it.descriptionName == target }) >> { Description d -> d }

        1 * transactionRepositoryMock.findByDescriptionAndActiveStatusOrderByTransactionDateDesc('s1', true) >> [
                finance.helpers.TransactionBuilder.builder().withDescription('s1').build()
        ]
        1 * transactionRepositoryMock.saveAndFlush({ it.description == target }) >> { tx -> tx }
        1 * descriptionRepositoryMock.findByDescriptionName('s1') >> Optional.of(DescriptionBuilder.builder().withDescription('s1').build())
        1 * descriptionRepositoryMock.saveAndFlush({ it.descriptionName == 's1' && it.activeStatus == false }) >> { Description d -> d }

        1 * transactionRepositoryMock.findByDescriptionAndActiveStatusOrderByTransactionDateDesc('s2', true) >> [
                finance.helpers.TransactionBuilder.builder().withDescription('s2').build(),
                finance.helpers.TransactionBuilder.builder().withDescription('s2').build()
        ]
        2 * transactionRepositoryMock.saveAndFlush({ it.description == target }) >> { tx -> tx }
        1 * descriptionRepositoryMock.findByDescriptionName('s2') >> Optional.of(DescriptionBuilder.builder().withDescription('s2').build())
        1 * descriptionRepositoryMock.saveAndFlush({ it.descriptionName == 's2' && it.activeStatus == false }) >> { Description d -> d }

        result.descriptionName == target
        0 * _
    }

    void 'mergeDescriptions uses existing target and does not recreate it'() {
        given:
        String target = 'existing'
        List<String> sources = ['s1']
        def existingTarget = DescriptionBuilder.builder().withDescription(target).build()

        when:
        Description result = descriptionService.mergeDescriptions(target, sources)

        then:
        1 * descriptionRepositoryMock.findByDescriptionName(target) >> Optional.of(existingTarget)

        1 * transactionRepositoryMock.findByDescriptionAndActiveStatusOrderByTransactionDateDesc('s1', true) >> [
                finance.helpers.TransactionBuilder.builder().withDescription('s1').build()
        ]
        1 * transactionRepositoryMock.saveAndFlush({ it.description == target }) >> { tx -> tx }
        1 * descriptionRepositoryMock.findByDescriptionName('s1') >> Optional.of(DescriptionBuilder.builder().withDescription('s1').build())
        1 * descriptionRepositoryMock.saveAndFlush({ it.descriptionName == 's1' && it.activeStatus == false }) >> { Description d -> d }

        result.descriptionName == target
        0 * _
    }



    void 'mergeDescriptions normalizes names and skips self-merge for identical source'() {
        given:
        String target = ' Amazon '
        List<String> sources = ['AMAZON', ' b ']
        def existingTarget = DescriptionBuilder.builder().withDescription('amazon').build()

        when:
        Description result = descriptionService.mergeDescriptions(target, sources)

        then:
        1 * descriptionRepositoryMock.findByDescriptionName('amazon') >> Optional.of(existingTarget)
        // Only process 'b' source
        1 * transactionRepositoryMock.findByDescriptionAndActiveStatusOrderByTransactionDateDesc('b', true) >> [
                finance.helpers.TransactionBuilder.builder().withDescription('b').build()
        ]
        1 * transactionRepositoryMock.saveAndFlush({ it.description == 'amazon' }) >> { tx -> tx }
        1 * descriptionRepositoryMock.findByDescriptionName('b') >> Optional.of(DescriptionBuilder.builder().withDescription('b').build())
        1 * descriptionRepositoryMock.saveAndFlush({ it.descriptionName == 'b' && it.activeStatus == false }) >> { Description d -> d }

        result.descriptionName == 'amazon'
        0 * _
    }

}
