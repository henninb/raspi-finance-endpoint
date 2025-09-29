package finance.repositories

import finance.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {
    fun findByUsername(username: String): Optional<User>

    fun findByUsernameAndPassword(
        username: String,
        password: String,
    ): Optional<User>
}
