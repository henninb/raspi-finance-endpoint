package finance.services


import finance.domain.User
import finance.repositories.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.*


@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : BaseService() {

    fun signIn(user: User): Optional<User> {
        // Retrieve the user by username
        val userOptional = userRepository.findByUsername(user.username)
        
        // Always perform password check to prevent timing attacks
        val dbUser = userOptional.orElse(User().apply { 
            password = "\$2a\$12\$dummy.hash.to.prevent.timing.attacks.with.constant.time.processing" 
        })
        
        // Always perform password check regardless of user existence
        val passwordMatches = passwordEncoder.matches(user.password, dbUser.password)
        
        return if (userOptional.isPresent && passwordMatches) {
            userOptional
        } else {
            Optional.empty()
        }
    }

    fun signUp(user: User): User {
        // Check if the username is already taken
        if (userRepository.findByUsername(user.username).isPresent) {
            throw IllegalArgumentException("Username already exists")
        }

        // Hash the raw password securely using BCrypt
        val hashedPassword = passwordEncoder.encode(user.password)
        user.password = hashedPassword
        return userRepository.saveAndFlush(user)
    }

//    fun findUserByUsername(username: String): User? =
//        userRepository.findByUsername(username).orElse(null)

    fun findUserByUsername(username: String): User? =
        userRepository.findByUsername(username)
            .orElse(null)
            ?.apply { password = "" }
}
