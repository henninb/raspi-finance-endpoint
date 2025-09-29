package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.LowerCaseConverter
import finance.utils.MedicalProviderTypeConverter
import finance.utils.NetworkStatusConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.sql.Timestamp
import java.util.Calendar

@Entity
@Table(
    name = "t_medical_provider",
    uniqueConstraints = [
        UniqueConstraint(
            columnNames = ["npi"],
            name = "uk_medical_provider_npi",
        ),
    ],
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class MedicalProvider(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "t_medical_provider_provider_id_seq")
    @param:JsonProperty
    @field:Min(value = 0L)
    @Column(name = "provider_id", nullable = false)
    var providerId: Long,
    @param:JsonProperty
    @Column(name = "provider_name", nullable = false)
    @field:Size(min = 3, max = 500, message = "Provider name must be between 3 and 500 characters")
    @field:Convert(converter = LowerCaseConverter::class)
    var providerName: String,
    @param:JsonProperty
    @Column(name = "provider_type", nullable = false)
    @Convert(converter = MedicalProviderTypeConverter::class)
    var providerType: MedicalProviderType,
    @param:JsonProperty
    @Column(name = "specialty")
    @field:Size(max = 200, message = "Specialty must be 200 characters or less")
    var specialty: String? = null,
    @param:JsonProperty
    @Column(name = "npi", unique = true)
    @field:Pattern(regexp = "^[0-9]{10}$", message = "NPI must be exactly 10 digits")
    var npi: String? = null,
    @param:JsonProperty
    @Column(name = "tax_id")
    @field:Size(max = 20, message = "Tax ID must be 20 characters or less")
    var taxId: String? = null,
    // Address information
    @param:JsonProperty
    @Column(name = "address_line1")
    @field:Size(max = 200, message = "Address line 1 must be 200 characters or less")
    var addressLine1: String? = null,
    @param:JsonProperty
    @Column(name = "address_line2")
    @field:Size(max = 200, message = "Address line 2 must be 200 characters or less")
    var addressLine2: String? = null,
    @param:JsonProperty
    @Column(name = "city")
    @field:Size(max = 100, message = "City must be 100 characters or less")
    var city: String? = null,
    @param:JsonProperty
    @Column(name = "state")
    @field:Size(max = 50, message = "State must be 50 characters or less")
    var state: String? = null,
    @param:JsonProperty
    @Column(name = "zip_code")
    @field:Pattern(regexp = "^[0-9]{5}(-[0-9]{4})?$", message = "Zip code must be in format 12345 or 12345-6789")
    var zipCode: String? = null,
    @param:JsonProperty
    @Column(name = "country", columnDefinition = "TEXT DEFAULT 'US'")
    @field:Size(max = 50, message = "Country must be 50 characters or less")
    var country: String = "US",
    // Contact information
    @param:JsonProperty
    @Column(name = "phone")
    @field:Size(min = 10, max = 20, message = "Phone number must be between 10 and 20 characters")
    var phone: String? = null,
    @param:JsonProperty
    @Column(name = "fax")
    @field:Size(min = 10, max = 20, message = "Fax number must be between 10 and 20 characters")
    var fax: String? = null,
    @param:JsonProperty
    @Column(name = "email")
    @field:Email(message = "Email must be valid format")
    @field:Size(max = 200, message = "Email must be 200 characters or less")
    var email: String? = null,
    @param:JsonProperty
    @Column(name = "website")
    @field:Size(max = 500, message = "Website must be 500 characters or less")
    var website: String? = null,
    // Provider details
    @param:JsonProperty
    @Column(name = "network_status", columnDefinition = "TEXT DEFAULT 'unknown'")
    @Convert(converter = NetworkStatusConverter::class)
    var networkStatus: NetworkStatus = NetworkStatus.Unknown,
    @param:JsonProperty
    @Column(name = "billing_name")
    @field:Size(max = 500, message = "Billing name must be 500 characters or less")
    var billingName: String? = null,
    @param:JsonProperty
    @Column(name = "notes")
    @field:Size(max = 2000, message = "Notes must be 2000 characters or less")
    var notes: String? = null,
    @param:JsonProperty
    @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    var activeStatus: Boolean = true,
) {
    constructor() : this(
        0L, "", MedicalProviderType.General, null, null, null,
        null, null, null, null, null, "US",
        null, null, null, null, NetworkStatus.Unknown, null, null, true,
    )

    @JsonProperty
    @Column(name = "date_added", nullable = false)
    var dateAdded: Timestamp = Timestamp(Calendar.getInstance().time.time)

    @JsonProperty
    @Column(name = "date_updated", nullable = false)
    var dateUpdated: Timestamp = Timestamp(Calendar.getInstance().time.time)

    override fun toString(): String {
        return mapper.writeValueAsString(this)
    }

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}
