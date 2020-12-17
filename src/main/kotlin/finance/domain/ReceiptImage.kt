package finance.domain

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.hibernate.annotations.Proxy
import org.hibernate.annotations.Type
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

        @field:Min(value = 0L)
        @JsonIgnore
        @Column(name = "receipt_image_id", nullable = false)
        var receiptImageId: Long,

        @JsonIgnore
        @field:Min(value = 0L)
        @Column(name = "transaction_id", nullable = false)
        var transactionId: Long,

        @JsonProperty
        @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
        var activeStatus: Boolean = true
) {

    constructor() : this(0L, 0L, true)

    @JsonGetter("jpgImage")
    fun jsonGetterJpgImage(): String {
        return this.jpgImage.toString(Charsets.UTF_8)
    }

    @Lob
    @JsonProperty
    @Type(type = "org.hibernate.type.BinaryType")
    @Column(name = "jpg_image", nullable = false)
    lateinit var jpgImage: ByteArray

    override fun toString(): String = mapper.writeValueAsString(this)

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}