package finance.services


import finance.repositories.UserRepository
//import org.springframework.security.authentication.AuthenticationManager
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
//import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Service

@Service
class UserService(
                  private  var userRepository: UserRepository,
                  ) {



    fun signIn(username: String, password: String): String {
//        return try {
//            //authenticationManager.authenticate(UsernamePasswordAuthenticationToken(username, password))
//            //jwtTokenProvider.createToken(username)
//        } catch (e: AuthenticationException) {
//            //throw new CustomException("Invalid username/password supplied", HttpStatus.UNPROCESSABLE_ENTITY);
//            throw RuntimeException("Invalid username/password supplied.")
//        }
        return "jwt token"
    }
}