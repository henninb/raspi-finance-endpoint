package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.hibernate.annotations.Proxy
import org.hibernate.annotations.Type
import javax.persistence.*
import javax.validation.constraints.Min

@Entity(name = "ReceiptImageEntity")
@Proxy(lazy = false)
@Table(name = "t_receipt_image")
@JsonIgnoreProperties(ignoreUnknown = true)
data class ReceiptImage(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @field:Min(value = 0L)
        @JsonProperty
        @Column(name = "receipt_image_id", nullable = false)
        var receipt_image_id: Long,

        @JsonProperty
        @field:Min(value = 0L)
        @Column(name = "transaction_id", nullable = false)
        var transactionId: Long,

        @Lob
        @JsonIgnore
        @Type(type = "org.hibernate.type.BinaryType")
        @Column(name = "receipt_image", nullable = false)
        var receiptImage: ByteArray?

        ) {

    constructor() : this(0L, 0L, ByteArray(0))

    override fun toString(): String = mapper.writeValueAsString(this)

//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (javaClass != other?.javaClass) return false
//
//        other as ReceiptImage
//
//        if (receipt_image_id != other.receipt_image_id) return false
//        if (transactionId != other.transactionId) return false
//
//        return true
//    }

    override fun hashCode(): Int = receipt_image_id.toInt()

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}