package finance.repositories

import finance.domain.ReceiptImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface ReceiptImageRepository : JpaRepository<ReceiptImage, Long> {

    @Modifying
    @Transactional
    @Query("INSERT INTO t_receipt_image(receipt_image, transaction_id) VALUES(?2, ?1)", nativeQuery = true)
    fun insertReceiptImageByTransactionId(transactionId: Long, receiptImage: ByteArray)

    @Modifying
    @Transactional
    @Query("UPDATE t_receipt_image set receipt_image=$2 where transaction_id = ?1", nativeQuery = true)
    fun updateReceiptImageByTransactionId(transactionId: Long, receiptImage: ByteArray)

    fun findByTransactionId(transactionId: Long): ReceiptImage
}