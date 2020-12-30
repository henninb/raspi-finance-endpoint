package finance.repositories

import finance.domain.ReceiptImage
import org.springframework.data.jpa.repository.JpaRepository

interface ReceiptImageRepository : JpaRepository<ReceiptImage, Long>