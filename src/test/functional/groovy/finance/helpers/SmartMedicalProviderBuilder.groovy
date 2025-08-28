package finance.helpers

import finance.domain.MedicalProvider
import finance.domain.MedicalProviderType
import finance.domain.NetworkStatus
import groovy.util.logging.Slf4j
import java.sql.Timestamp
import java.util.concurrent.atomic.AtomicInteger

@Slf4j
class SmartMedicalProviderBuilder {

    private static final AtomicInteger COUNTER = new AtomicInteger(0)

    private String testOwner
    private Long providerId = 0L
    private String providerName
    private MedicalProviderType providerType = MedicalProviderType.General
    private String specialty = null
    private String npi = null
    private String taxId = null
    private String addressLine1 = null
    private String addressLine2 = null
    private String city = null
    private String state = null
    private String zipCode = null
    private String country = 'US'
    private String phone = null
    private String fax = null
    private String email = null
    private String website = null
    private NetworkStatus networkStatus = NetworkStatus.Unknown
    private String billingName = null
    private String notes = null
    private Boolean activeStatus = true

    private SmartMedicalProviderBuilder(String testOwner) {
        this.testOwner = testOwner
        this.providerName = generateUniqueProviderName()
    }

    static SmartMedicalProviderBuilder builderForOwner(String testOwner) {
        return new SmartMedicalProviderBuilder(testOwner)
    }

    private String generateUniqueProviderName() {
        String counter = COUNTER.incrementAndGet().toString()
        String ownerPart = testOwner?.toString()?.toLowerCase()?.replaceAll(/[^a-z0-9]/, '') ?: 'test'
        if (ownerPart.isEmpty()) ownerPart = 'test'

        String base = "provider${counter}_${ownerPart}"

        // Ensure length constraints (3-500 chars)
        if (base.length() > 500) {
            String shortOwner = ownerPart.length() > 12 ? ownerPart[0..11] : ownerPart
            base = "prov${counter}_${shortOwner}"
        }
        if (base.length() < 3) base = 'provider'

        String cleaned = base.toLowerCase()
        log.debug("Generated provider name: ${cleaned} for test owner: ${testOwner}")
        return cleaned
    }

    MedicalProvider build() {
        MedicalProvider provider = new MedicalProvider(
            providerId: this.providerId,
            providerName: this.providerName,
            providerType: this.providerType,
            specialty: this.specialty,
            npi: this.npi,
            taxId: this.taxId,
            addressLine1: this.addressLine1,
            addressLine2: this.addressLine2,
            city: this.city,
            state: this.state,
            zipCode: this.zipCode,
            country: this.country,
            phone: this.phone,
            fax: this.fax,
            email: this.email,
            website: this.website,
            networkStatus: this.networkStatus,
            billingName: this.billingName,
            notes: this.notes,
            activeStatus: this.activeStatus
        )
        return provider
    }

    MedicalProvider buildAndValidate() {
        MedicalProvider provider = build()
        validateConstraints(provider)
        return provider
    }

    private void validateConstraints(MedicalProvider provider) {
        // Provider name: 3-500 characters
        if (provider.providerName == null || provider.providerName.length() < 3 || provider.providerName.length() > 500) {
            throw new IllegalStateException("Provider name '${provider.providerName}' violates length constraints (3-500 chars)")
        }

        // NPI: exactly 10 digits (if provided)
        if (provider.npi != null && !provider.npi.matches(/^[0-9]{10}$/)) {
            throw new IllegalStateException("NPI '${provider.npi}' must be exactly 10 digits")
        }

        // Specialty: max 200 characters (if provided)
        if (provider.specialty != null && provider.specialty.length() > 200) {
            throw new IllegalStateException("Specialty '${provider.specialty}' exceeds 200 character limit")
        }

        // Tax ID: max 20 characters (if provided)
        if (provider.taxId != null && provider.taxId.length() > 20) {
            throw new IllegalStateException("Tax ID '${provider.taxId}' exceeds 20 character limit")
        }

        // Address fields: max 200 characters (if provided)
        if (provider.addressLine1 != null && provider.addressLine1.length() > 200) {
            throw new IllegalStateException("Address line 1 '${provider.addressLine1}' exceeds 200 character limit")
        }
        if (provider.addressLine2 != null && provider.addressLine2.length() > 200) {
            throw new IllegalStateException("Address line 2 '${provider.addressLine2}' exceeds 200 character limit")
        }

        // City: max 100 characters (if provided)
        if (provider.city != null && provider.city.length() > 100) {
            throw new IllegalStateException("City '${provider.city}' exceeds 100 character limit")
        }

        // State: max 50 characters (if provided)
        if (provider.state != null && provider.state.length() > 50) {
            throw new IllegalStateException("State '${provider.state}' exceeds 50 character limit")
        }

        // Zip code: format validation (if provided)
        if (provider.zipCode != null && !provider.zipCode.matches(/^[0-9]{5}(-[0-9]{4})?$/)) {
            throw new IllegalStateException("Zip code '${provider.zipCode}' must be in format 12345 or 12345-6789")
        }

        // Country: max 50 characters (if provided)
        if (provider.country != null && provider.country.length() > 50) {
            throw new IllegalStateException("Country '${provider.country}' exceeds 50 character limit")
        }

        // Phone/Fax: 10-20 characters (if provided)
        if (provider.phone != null && (provider.phone.length() < 10 || provider.phone.length() > 20)) {
            throw new IllegalStateException("Phone '${provider.phone}' must be between 10 and 20 characters")
        }
        if (provider.fax != null && (provider.fax.length() < 10 || provider.fax.length() > 20)) {
            throw new IllegalStateException("Fax '${provider.fax}' must be between 10 and 20 characters")
        }

        // Email: max 200 characters and valid format (if provided)
        if (provider.email != null) {
            if (provider.email.length() > 200) {
                throw new IllegalStateException("Email '${provider.email}' exceeds 200 character limit")
            }
            // Basic email format validation
            if (!provider.email.contains('@') || !provider.email.contains('.')) {
                throw new IllegalStateException("Email '${provider.email}' must be valid format")
            }
        }

        // Website: max 500 characters (if provided)
        if (provider.website != null && provider.website.length() > 500) {
            throw new IllegalStateException("Website '${provider.website}' exceeds 500 character limit")
        }

        // Billing name: max 500 characters (if provided)
        if (provider.billingName != null && provider.billingName.length() > 500) {
            throw new IllegalStateException("Billing name '${provider.billingName}' exceeds 500 character limit")
        }

        // Notes: max 2000 characters (if provided)
        if (provider.notes != null && provider.notes.length() > 2000) {
            throw new IllegalStateException("Notes '${provider.notes}' exceeds 2000 character limit")
        }

        log.debug("MedicalProvider passed constraint validation: ${provider.providerName}")
    }

    // Fluent API - Basic properties
    SmartMedicalProviderBuilder withProviderName(String providerName) {
        this.providerName = providerName?.toLowerCase()
        return this
    }

    SmartMedicalProviderBuilder withUniqueProviderName(String prefix = 'provider') {
        this.providerName = generateUniqueProviderName(prefix)
        return this
    }

    private String generateUniqueProviderName(String prefix) {
        String cleanPrefix = prefix?.toLowerCase()?.replaceAll(/[^a-z0-9]/, '') ?: 'provider'
        if (cleanPrefix.isEmpty()) cleanPrefix = 'provider'

        String ownerPart = testOwner?.toLowerCase()?.replaceAll(/[^a-z0-9]/, '') ?: 'test'
        if (ownerPart.isEmpty()) ownerPart = 'test'

        String base = "${cleanPrefix}_${ownerPart}"
        if (base.length() > 500) {
            String shortOwner = ownerPart.length() > 12 ? ownerPart[0..11] : ownerPart
            base = "${cleanPrefix}_${shortOwner}"
        }
        if (base.length() < 3) base = 'provider'
        return base.toLowerCase()
    }

    SmartMedicalProviderBuilder withProviderType(MedicalProviderType providerType) {
        this.providerType = providerType
        return this
    }

    SmartMedicalProviderBuilder withSpecialty(String specialty) {
        this.specialty = specialty
        return this
    }

    SmartMedicalProviderBuilder withNPI(String npi) {
        this.npi = npi
        return this
    }

    SmartMedicalProviderBuilder withValidNPI() {
        // Generate a valid 10-digit NPI for testing
        String counter = COUNTER.get().toString().padLeft(10, '0')
        this.npi = counter
        return this
    }

    SmartMedicalProviderBuilder withTaxId(String taxId) {
        this.taxId = taxId
        return this
    }

    // Address fluent API
    SmartMedicalProviderBuilder withAddress(String addressLine1, String city, String state, String zipCode) {
        this.addressLine1 = addressLine1
        this.city = city
        this.state = state
        this.zipCode = zipCode
        return this
    }

    SmartMedicalProviderBuilder withAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1
        return this
    }

    SmartMedicalProviderBuilder withAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2
        return this
    }

    SmartMedicalProviderBuilder withCity(String city) {
        this.city = city
        return this
    }

    SmartMedicalProviderBuilder withState(String state) {
        this.state = state
        return this
    }

    SmartMedicalProviderBuilder withZipCode(String zipCode) {
        this.zipCode = zipCode
        return this
    }

    SmartMedicalProviderBuilder withCountry(String country) {
        this.country = country
        return this
    }

    // Contact fluent API
    SmartMedicalProviderBuilder withPhone(String phone) {
        this.phone = phone
        return this
    }

    SmartMedicalProviderBuilder withFax(String fax) {
        this.fax = fax
        return this
    }

    SmartMedicalProviderBuilder withEmail(String email) {
        this.email = email
        return this
    }

    SmartMedicalProviderBuilder withWebsite(String website) {
        this.website = website
        return this
    }

    // Provider details fluent API
    SmartMedicalProviderBuilder withNetworkStatus(NetworkStatus networkStatus) {
        this.networkStatus = networkStatus
        return this
    }

    SmartMedicalProviderBuilder withBillingName(String billingName) {
        this.billingName = billingName
        return this
    }

    SmartMedicalProviderBuilder withNotes(String notes) {
        this.notes = notes
        return this
    }

    SmartMedicalProviderBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }

    // Convenience methods for common test scenarios
    SmartMedicalProviderBuilder asGeneral() {
        this.providerType = MedicalProviderType.General
        return this
    }

    SmartMedicalProviderBuilder asSpecialist() {
        this.providerType = MedicalProviderType.Specialist
        return this
    }

    SmartMedicalProviderBuilder asHospital() {
        this.providerType = MedicalProviderType.Hospital
        return this
    }

    SmartMedicalProviderBuilder asPharmacy() {
        this.providerType = MedicalProviderType.Pharmacy
        return this
    }

    SmartMedicalProviderBuilder asInNetwork() {
        this.networkStatus = NetworkStatus.InNetwork
        return this
    }

    SmartMedicalProviderBuilder asOutOfNetwork() {
        this.networkStatus = NetworkStatus.OutOfNetwork
        return this
    }

    SmartMedicalProviderBuilder asUnknownNetwork() {
        this.networkStatus = NetworkStatus.Unknown
        return this
    }

    SmartMedicalProviderBuilder asActive() {
        this.activeStatus = true
        return this
    }

    SmartMedicalProviderBuilder asInactive() {
        this.activeStatus = false
        return this
    }

    // Common preset configurations
    SmartMedicalProviderBuilder asCompleteProvider() {
        this.specialty = 'family medicine'
        this.npi = '1234567890'
        this.taxId = 'TAX123'
        this.addressLine1 = '123 Main St'
        this.city = 'TestCity'
        this.state = 'TS'
        this.zipCode = '12345'
        this.phone = '5551234567'
        this.email = 'provider@test.com'
        this.networkStatus = NetworkStatus.InNetwork
        return this
    }

    SmartMedicalProviderBuilder asMinimalProvider() {
        // Only required fields
        return this
    }
}