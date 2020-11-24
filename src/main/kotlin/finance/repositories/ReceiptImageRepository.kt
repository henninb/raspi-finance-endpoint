package finance.repositories

import finance.domain.ReceiptImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface ReceiptImageRepository : JpaRepository<ReceiptImage, Long> {
}