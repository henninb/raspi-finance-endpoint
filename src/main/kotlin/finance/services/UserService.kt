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
) {

    fun signIn(username: String, rawPassword: String): Optional<User> {
        // Retrieve the user by username
        val userOptional = userRepository.findByUsername(username)
        if (userOptional.isPresent) {
            val user = userOptional.get()
            // Validate the raw password against the stored hash
            if (passwordEncoder.matches(rawPassword, user.password)) {
                return Optional.of(user)
            }
        }
        return Optional.empty()
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
}
