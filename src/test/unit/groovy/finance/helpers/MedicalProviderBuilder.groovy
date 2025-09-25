package finance.helpers

import finance.domain.MedicalProvider
import finance.domain.MedicalProviderType
import finance.domain.NetworkStatus

class MedicalProviderBuilder {

    Long providerId = 0L
    String providerName = 'test_provider'
    MedicalProviderType providerType = MedicalProviderType.General
    String specialty = 'family_medicine'
    String npi = '1234567890'
    String taxId = 'TAX123456789'
    String addressLine1 = '123 main st'
    String addressLine2 = 'suite 100'
    String city = 'anytown'
    String state = 'ca'
    String zipCode = '12345'
    String country = 'US'
    String phone = '5551234567'
    String fax = '5551234568'
    String email = 'provider@example.com'
    String website = 'https://example.com'
    NetworkStatus networkStatus = NetworkStatus.InNetwork
    String billingName = 'test provider billing'
    String notes = 'test provider notes'
    Boolean activeStatus = true

    static MedicalProviderBuilder builder() {
        new MedicalProviderBuilder()
    }

    MedicalProvider build() {
        MedicalProvider provider = new MedicalProvider().with {
            providerId = this.providerId
            providerName = this.providerName
            providerType = this.providerType
            specialty = this.specialty
            npi = this.npi
            taxId = this.taxId
            addressLine1 = this.addressLine1
            addressLine2 = this.addressLine2
            city = this.city
            state = this.state
            zipCode = this.zipCode
            country = this.country
            phone = this.phone
            fax = this.fax
            email = this.email
            website = this.website
            networkStatus = this.networkStatus
            billingName = this.billingName
            notes = this.notes
            activeStatus = this.activeStatus
            it
        }
        provider
    }

    MedicalProviderBuilder withProviderId(Long providerId) {
        this.providerId = providerId
        this
    }

    MedicalProviderBuilder withProviderName(String providerName) {
        this.providerName = providerName
        this
    }

    MedicalProviderBuilder withProviderType(MedicalProviderType providerType) {
        this.providerType = providerType
        this
    }

    MedicalProviderBuilder withSpecialty(String specialty) {
        this.specialty = specialty
        this
    }

    MedicalProviderBuilder withNpi(String npi) {
        this.npi = npi
        this
    }

    MedicalProviderBuilder withTaxId(String taxId) {
        this.taxId = taxId
        this
    }

    MedicalProviderBuilder withAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1
        this
    }

    MedicalProviderBuilder withAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2
        this
    }

    MedicalProviderBuilder withCity(String city) {
        this.city = city
        this
    }

    MedicalProviderBuilder withState(String state) {
        this.state = state
        this
    }

    MedicalProviderBuilder withZipCode(String zipCode) {
        this.zipCode = zipCode
        this
    }

    MedicalProviderBuilder withCountry(String country) {
        this.country = country
        this
    }

    MedicalProviderBuilder withPhone(String phone) {
        this.phone = phone
        this
    }

    MedicalProviderBuilder withFax(String fax) {
        this.fax = fax
        this
    }

    MedicalProviderBuilder withEmail(String email) {
        this.email = email
        this
    }

    MedicalProviderBuilder withWebsite(String website) {
        this.website = website
        this
    }

    MedicalProviderBuilder withNetworkStatus(NetworkStatus networkStatus) {
        this.networkStatus = networkStatus
        this
    }

    MedicalProviderBuilder withBillingName(String billingName) {
        this.billingName = billingName
        this
    }

    MedicalProviderBuilder withNotes(String notes) {
        this.notes = notes
        this
    }

    MedicalProviderBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        this
    }
}
