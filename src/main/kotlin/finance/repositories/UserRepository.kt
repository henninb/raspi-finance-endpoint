package finance.repositories


import finance.domain.User
import jakarta.security.enterprise.credential.Password
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserRepository : JpaRepository<User, Long> {
    fun findByUsername(username: String): Optional<User>
    fun findByUsernameAndPassword(username: String, password: String): Optional<User>
}