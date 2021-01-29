package finance.domain

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.ImageFormatTypeConverter
import finance.utils.ValidImage
import org.apache.logging.log4j.LogManager
import org.hibernate.annotations.Proxy
import org.hibernate.annotations.Type
import java.sql.Timestamp
import java.util.*
import javax.persistence.*
import javax.validation.constraints.Min

@Entity
@Proxy(lazy = false)
@Table(name = "t_receipt_image")
@JsonIgnoreProperties(ignoreUnknown = true)
data class ReceiptImage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "t_receipt_image_receipt_image_id_seq")
    @JsonProperty
    @field:Min(value = 0L)
    @Column(name = "receipt_image_id", nullable = false)
    var receiptImageId: Long,

    @JsonProperty
    @field:Min(value = 0L)
    @Column(name = "transaction_id", nullable = false)
    var transactionId: Long,

    @JsonProperty
    @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    var activeStatus: Boolean = true
) {

    constructor() : this(0L, 0L, true)

    @JsonIgnore
    @Column(name = "date_added", nullable = false)
    var dateAdded: Timestamp = Timestamp(Calendar.getInstance().time.time)

    @JsonIgnore
    @Column(name = "date_updated", nullable = false)
    var dateUpdated: Timestamp = Timestamp(Calendar.getInstance().time.time)

    @JsonGetter("image")
    fun jsonGetterJpgImage(): String {
        //https://cryptii.com/pipes/base64-to-hex
        //logger.info(this.image.toHexString())

        return Base64.getEncoder().encodeToString(this.image)
    }

    //TODO: 2021-01-09, temporary method
    private fun ByteArray.toHexString(): String {
        return this.joinToString("") {
            String.format("%02x", it)
        }
    }

    @JsonProperty
    @Column(name = "image_format_type", nullable = false)
    @Convert(converter = ImageFormatTypeConverter::class)
    var imageFormatType: ImageFormatType = ImageFormatType.Undefined

    @Lob
    @JsonProperty
    @Type(type = "org.hibernate.type.BinaryType")
    @field:ValidImage
    @Column(name = "image", nullable = false)
    lateinit var image: ByteArray

    @Lob
    @JsonProperty
    @field:ValidImage
    @Type(type = "org.hibernate.type.BinaryType")
    @Column(name = "thumbnail", nullable = false)
    lateinit var thumbnail: ByteArray

    override fun toString(): String {
        return mapper.writeValueAsString(this)
    }

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()

        @JsonIgnore
        private val logger = LogManager.getLogger()
    }
}