//package finance.controllers
//
//import finance.domain.User
//import finance.services.UserService
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.http.ResponseEntity
//import org.springframework.web.bind.annotation.*
//
//@CrossOrigin
//@RestController
//@RequestMapping("/user", "/api/user")
//class UserController @Autowired constructor(private var userService: UserService) : BaseController() {
//
//    //curl -X POST -H "Content-Type: application/json"  -d '{"username":"user","password":"pass"}' http://localhost:8443/user/signin
//    @PostMapping("/signin")
//    fun signIn( @RequestBody user: User): ResponseEntity<User> {
//        return ResponseEntity.ok(user)
//    }
//
//    @PostMapping("/signup")
//    fun signUp( @RequestBody user: User): ResponseEntity<String> {
//        return ResponseEntity.ok(mapper.writeValueAsString(user))
//    }
//
////    @GetMapping(value = "/whoami")
////    public String whoami(HttpServletRequest req) throws JsonProcessingException {
////        return objectMapper.writeValueAsString(userService.whoami(req));
////    }
////
////    @GetMapping("/refresh")
////    public String refresh(HttpServletRequest req) {
////        return userService.refresh(req.getRemoteUser());
////    }
//}