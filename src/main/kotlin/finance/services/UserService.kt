package finance.services


import finance.domain.User
import finance.repositories.UserRepository
//import org.springframework.security.authentication.AuthenticationManager
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
//import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Service
import java.util.*

@Service
class UserService(
                  private  var userRepository: UserRepository,
                  ) {



    fun signIn(username: String, password: String): Optional<User> {
        return  userRepository.findByUsernameAndPassword(username, password)
//        return try {
//            //authenticationManager.authenticate(UsernamePasswordAuthenticationToken(username, password))
//            //jwtTokenProvider.createToken(username)
//        } catch (e: AuthenticationException) {
//            //throw new CustomException("Invalid username/password supplied", HttpStatus.UNPROCESSABLE_ENTITY);
//            throw RuntimeException("Invalid username/password supplied.")
//        }
        //return "jwt token"
    }
}