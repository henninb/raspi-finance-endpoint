package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.FamilyRelationshipConverter
import finance.utils.LowerCaseConverter
import java.sql.Date
import java.sql.Timestamp
import java.util.*
import jakarta.persistence.*
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Entity
@Table(
    name = "t_family_member",
    uniqueConstraints = [UniqueConstraint(
        columnNames = ["owner", "member_name"],
        name = "uk_family_member_owner_name"
    )]
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class FamilyMember(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "t_family_member_family_member_id_seq")
    @param:JsonProperty
    @Column(name = "family_member_id", nullable = false)
    var familyMemberId: Long,

    @param:JsonProperty
    @Column(name = "owner", nullable = false)
    @field:Size(min = 3, max = 100, message = "Owner must be between 3 and 100 characters")
    @field:Convert(converter = LowerCaseConverter::class)
    var owner: String,

    @param:JsonProperty
    @Column(name = "member_name", nullable = false)
    @field:Size(min = 1, max = 100, message = "Member name must be between 1 and 100 characters")
    @field:Convert(converter = LowerCaseConverter::class)
    var memberName: String,

    @param:JsonProperty
    @Column(name = "relationship", nullable = false)
    @Convert(converter = FamilyRelationshipConverter::class)
    var relationship: FamilyRelationship,

    @param:JsonProperty
    @Column(name = "date_of_birth")
    var dateOfBirth: Date? = null,

    @param:JsonProperty
    @Column(name = "insurance_member_id")
    @field:Size(max = 50, message = "Insurance member ID must be 50 characters or less")
    var insuranceMemberId: String? = null,

    @param:JsonProperty
    @Column(name = "ssn_last_four")
    @field:Pattern(regexp = "^[0-9]{4}$", message = "SSN last four must be exactly 4 digits")
    var ssnLastFour: String? = null,

    @param:JsonProperty
    @Column(name = "medical_record_number")
    @field:Size(max = 50, message = "Medical record number must be 50 characters or less")
    var medicalRecordNumber: String? = null,

    @param:JsonProperty
    @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    var activeStatus: Boolean = true

) {

    constructor() : this(
        0L, "", "", FamilyRelationship.Self, null, null, null, null, true
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