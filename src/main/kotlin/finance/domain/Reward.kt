package finance.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import finance.utils.LowerCaseConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.Calendar

@Entity
@Table(
    name = "t_reward",
    uniqueConstraints = [
        UniqueConstraint(
            columnNames = ["account_id", "multiplier", "category"],
            name = "uk_reward_account_multiplier_category",
        ),
    ],
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Reward(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reward_id")
    var rewardId: Long = 0L,
    @Column(name = "account_id", nullable = false)
    var accountId: Long = 0L,
    @Column(name = "owner", nullable = false)
    @field:Size(max = 100, message = "Owner must be 100 characters or less")
    @field:Convert(converter = LowerCaseConverter::class)
    var owner: String = "",
    @Column(name = "multiplier", nullable = false, precision = 4, scale = 1)
    @field:DecimalMin(value = "1.0", message = "Multiplier must be at least 1.0")
    var multiplier: BigDecimal = BigDecimal.ONE,
    @Column(name = "category", nullable = false)
    @field:NotBlank
    @field:Size(min = 1, max = 50)
    @field:Convert(converter = LowerCaseConverter::class)
    var category: String = "",
    @Column(name = "cpp", nullable = false, precision = 6, scale = 4)
    @field:DecimalMin(value = "0.0001", message = "CPP must be at least 0.0001")
    var cpp: BigDecimal = BigDecimal("0.01"),
    @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    var activeStatus: Boolean = true,
) {
    @JsonCreator
    constructor() : this(0L, 0L, "", BigDecimal.ONE, "", BigDecimal("0.01"), true)

    @JsonIgnore
    @Column(name = "date_added", nullable = false)
    var dateAdded: Timestamp = Timestamp(Calendar.getInstance().time.time)

    @JsonIgnore
    @Column(name = "date_updated", nullable = false)
    var dateUpdated: Timestamp = Timestamp(Calendar.getInstance().time.time)
}
