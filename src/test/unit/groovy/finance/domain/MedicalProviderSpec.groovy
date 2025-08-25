package finance.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import finance.helpers.MedicalProviderBuilder
import spock.lang.Shared
import spock.lang.Unroll

import jakarta.validation.ConstraintViolation

class MedicalProviderSpec extends BaseDomainSpec {

    @Shared
    protected String jsonPayload = '''
{
    "providerId": 0,
    "providerName": "test_provider",
    "providerType": "general",
    "specialty": "family_medicine",
    "npi": "1234567890",
    "taxId": "TAX123456789",
    "addressLine1": "123 main st",
    "addressLine2": "suite 100",
    "city": "anytown",
    "state": "ca",
    "zipCode": "12345",
    "country": "US",
    "phone": "5551234567",
    "fax": "5551234568",
    "email": "provider@example.com",
    "website": "https://example.com",
    "networkStatus": "in_network",
    "billingName": "test provider billing",
    "notes": "test provider notes",
    "activeStatus": true
}
'''

    @Shared
    protected String jsonPayloadInvalidProviderType = '''
{
    "providerId": 0,
    "providerName": "test_provider",
    "providerType": "invalid_type",
    "specialty": "family_medicine",
    "activeStatus": true
}
'''

    void 'test JSON deserialization to MedicalProvider'() {
        when:
        MedicalProvider provider = mapper.readValue(jsonPayload, MedicalProvider)

        then:
        provider.providerName == "test_provider"
        provider.providerType == MedicalProviderType.General
        provider.specialty == "family_medicine"
        provider.npi == "1234567890"
        provider.networkStatus == NetworkStatus.InNetwork
        provider.activeStatus == true
        0 * _
    }

    void 'test validation valid medical provider'() {
        given:
        MedicalProvider provider = MedicalProviderBuilder.builder().build()

        when:
        Set<ConstraintViolation<MedicalProvider>> violations = validator.validate(provider)

        then:
        violations.empty
        0 * _
    }

    @Unroll
    void 'test -- JSON deserialize to MedicalProvider with invalid payload'() {
        when:
        mapper.readValue(payload, MedicalProvider)

        then:
        Exception ex = thrown(exceptionThrown)
        ex.message.contains(message)
        0 * _

        where:
        payload                            | exceptionThrown          | message
        'non-jsonPayload'                  | JsonParseException       | 'Unrecognized token'
        '[]'                              | MismatchedInputException | 'Cannot deserialize value of type'
        '{providerName: "test"}'          | JsonParseException       | 'was expecting double-quote to start field name'
        '{"activeStatus": "abc"}'         | InvalidFormatException   | 'Cannot deserialize value of type'
        jsonPayloadInvalidProviderType    | InvalidFormatException   | 'Cannot deserialize value of type'
    }

    @Unroll
    void 'test validation invalid #invalidField has error #expectedError'() {
        given:
        MedicalProvider provider = new MedicalProviderBuilder()
                .withProviderName(providerName)
                .withProviderType(providerType)
                .withSpecialty(specialty)
                .withNpi(npi)
                .withEmail(email)
                .withZipCode(zipCode)
                .withPhone(phone)
                .withActiveStatus(activeStatus)
                .build()

        when:
        Set<ConstraintViolation<MedicalProvider>> violations = validator.validate(provider)

        then:
        violations.size() == errorCount
        violations.message.contains(expectedError)
        violations.iterator().next().invalidValue == provider.properties[invalidField]

        where:
        invalidField   | providerName | providerType              | specialty | npi          | email            | zipCode    | phone        | activeStatus | expectedError                                    | errorCount
        'providerName' | 'ab'         | MedicalProviderType.General | null     | null         | null             | null       | null         | true         | 'Provider name must be between 3 and 500 characters' | 1
        'providerName' | 'a' * 501    | MedicalProviderType.General | null     | null         | null             | null       | null         | true         | 'Provider name must be between 3 and 500 characters' | 1
        'specialty'    | 'test_provider' | MedicalProviderType.General | 'a' * 201 | null      | null             | null       | null         | true         | 'Specialty must be 200 characters or less'      | 1
        'npi'          | 'test_provider' | MedicalProviderType.General | null     | '123456789' | null             | null       | null         | true         | 'NPI must be exactly 10 digits'                | 1
        'npi'          | 'test_provider' | MedicalProviderType.General | null     | '12345678901' | null           | null       | null         | true         | 'NPI must be exactly 10 digits'                | 1
        'npi'          | 'test_provider' | MedicalProviderType.General | null     | '123456789a' | null            | null       | null         | true         | 'NPI must be exactly 10 digits'                | 1
        'email'        | 'test_provider' | MedicalProviderType.General | null     | null         | 'invalid-email'  | null       | null         | true         | 'Email must be valid format'                    | 1
        'zipCode'      | 'test_provider' | MedicalProviderType.General | null     | null         | null             | '1234'     | null         | true         | 'Zip code must be in format 12345 or 12345-6789' | 1
        'zipCode'      | 'test_provider' | MedicalProviderType.General | null     | null         | null             | '123456'   | null         | true         | 'Zip code must be in format 12345 or 12345-6789' | 1
        'zipCode'      | 'test_provider' | MedicalProviderType.General | null     | null         | null             | '12345-67890' | null      | true         | 'Zip code must be in format 12345 or 12345-6789' | 1
        'phone'        | 'test_provider' | MedicalProviderType.General | null     | null         | null             | null       | '123456789'  | true         | 'Phone number must be between 10 and 20 characters' | 1
        'phone'        | 'test_provider' | MedicalProviderType.General | null     | null         | null             | null       | '1' * 21     | true         | 'Phone number must be between 10 and 20 characters' | 1
    }

    void 'test MedicalProvider toString returns valid JSON'() {
        given:
        MedicalProvider provider = MedicalProviderBuilder.builder()
                .withProviderName("test_provider")
                .withProviderType(MedicalProviderType.Specialist)
                .build()

        when:
        String json = provider.toString()
        MedicalProvider parsed = mapper.readValue(json, MedicalProvider)

        then:
        parsed.providerName == "test_provider"
        parsed.providerType == MedicalProviderType.Specialist
        0 * _
    }

    void 'test MedicalProvider default constructor'() {
        when:
        MedicalProvider provider = new MedicalProvider()

        then:
        provider.providerId == 0L
        provider.providerName == ""
        provider.providerType == MedicalProviderType.General
        provider.specialty == null
        provider.npi == null
        provider.country == "US"
        provider.networkStatus == NetworkStatus.Unknown
        provider.activeStatus == true
        0 * _
    }
}