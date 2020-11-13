package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.hibernate.annotations.Proxy
import javax.persistence.*
import javax.validation.constraints.Min

//@Entity(name = "ParmEntity")
//@Proxy(lazy = false)
//@Table(name = "t_receipt_image")
//@JsonIgnoreProperties(ignoreUnknown = true)
//data class ReceiptImage(
//        @Id
//        @GeneratedValue(strategy = GenerationType.IDENTITY)
//        @field:Min(value = 0L)
//        @JsonProperty
//        @Column(name = "receipt_image_id", nullable = false)
//        var receipt_image_id: Long,
//
//        @JsonProperty
//        @field:Min(value = 0L)
//        @Column(name = "transaction_id", nullable = false)
//        var transactionId: Long
//
//        ) {
//
//    constructor() : this(0L, 0L)
//
//    override fun toString(): String = mapper.writeValueAsString(this)
//
//    companion object {
//        @JsonIgnore
//        private val mapper = ObjectMapper()
//    }
//}