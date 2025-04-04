// To be deleted

//package finance.services
//
//import finance.repositories.UserRepository
//import org.springframework.security.core.userdetails.User
//import org.springframework.security.core.userdetails.UserDetails
//import org.springframework.security.core.userdetails.UserDetailsService
//import org.springframework.security.core.userdetails.UsernameNotFoundException
//import org.springframework.stereotype.Service
//import java.util.*
//
//@Service
//class JwtUserDetailService(userRepository: UserRepository) : UserDetailsService {
//    private val userRepository: UserRepository
//
//    @Throws(UsernameNotFoundException::class)
//    override fun loadUserByUsername(username: String): UserDetails {
//        val user: Optional<finance.domain.User> = userRepository.findByUsername(username)
//            ?: throw UsernameNotFoundException("User '$username' not found")
//        return User
//            .withUsername(username)
//            .password(user.get().password)
//            .authorities("admin")
//            .accountExpired(false)
//            .accountLocked(false)
//            .credentialsExpired(false)
//            .disabled(false)
//            .build()
//    }
//
//    init {
//        this.userRepository = userRepository
//    }
//}