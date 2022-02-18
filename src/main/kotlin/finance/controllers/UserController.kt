package finance.controllers

import finance.domain.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/user")
class UserController @Autowired constructor() : BaseController() {

    @PostMapping("/signin")
    fun signIn( @RequestBody user: User): ResponseEntity<String> {
            return ResponseEntity.ok(mapper.writeValueAsString(user))
    }

    @PostMapping("/signup")
    fun signUp( @RequestBody user: User): ResponseEntity<String> {
        return ResponseEntity.ok(mapper.writeValueAsString(user))
    }
}