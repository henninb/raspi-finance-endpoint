package finance.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.ImageFormatTypeConverter
import finance.utils.LowerCaseConverter
import finance.utils.ValidImage
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.apache.logging.log4j.LogManager
import java.sql.Timestamp
import java.util.Base64
import java.util.Calendar

@Entity
@Table(name = "t_receipt_image")
@JsonIgnoreProperties(ignoreUnknown = true)
data class ReceiptImage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "t_receipt_image_receipt_image_id_seq")
    @field:Min(value = 0L)
    @Column(name = "receipt_image_id", nullable = false)
    var receiptImageId: Long,
    @Column(name = "owner", nullable = false)
    @field:Size(max = 100, message = "Owner must be 100 characters or less")
    @field:Convert(converter = LowerCaseConverter::class)
    var owner: String = "",
    @field:Min(value = 0L)
    @Column(name = "transaction_id", nullable = false)
    var transactionId: Long,
    @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    var activeStatus: Boolean = true,
) {
    @JsonCreator
    constructor() : this(0L, "", 0L, true)

    @JsonIgnore
    @Column(name = "date_added", nullable = false)
    var dateAdded: Timestamp = Timestamp(Calendar.getInstance().time.time)

    @JsonIgnore
    @Column(name = "date_updated", nullable = false)
    var dateUpdated: Timestamp = Timestamp(Calendar.getInstance().time.time)

    @JsonGetter("image")
    fun jsonGetterJpgImage(): String {
        // https://cryptii.com/pipes/base64-to-hex
        // logger.info(this.image.toHexString())

        return Base64.getEncoder().encodeToString(this.image)
    }

    @JsonProperty
    @Column(name = "image_format_type", nullable = false)
    @Convert(converter = ImageFormatTypeConverter::class)
    var imageFormatType: ImageFormatType = ImageFormatType.Undefined

    // @Lob
    @JsonProperty
    // @Type(type = "org.hibernate.type.BinaryType")
    // @Type(type="org.hibernate.type.ImageType")
    @field:ValidImage
    @Column(name = "image", nullable = false)
    lateinit var image: ByteArray

    // @Lob
    @JsonProperty
    @field:ValidImage
    // @Type(type="org.hibernate.type.ImageType")
    // @Type(type = "org.hibernate.type.BinaryType")
    @Column(name = "thumbnail", nullable = false)
    lateinit var thumbnail: ByteArray

    override fun toString(): String = mapper.writeValueAsString(this)

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()

        @JsonIgnore
        private val logger = LogManager.getLogger()
    }
}
