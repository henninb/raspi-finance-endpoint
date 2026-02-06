package finance.controllers

import finance.domain.User
import finance.services.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@Tag(name = "User Management", description = "User sign-in and registration operations")
@RestController
@RequestMapping("/api/user")
class UserController
    @Autowired
    constructor(
        private var userService: UserService,
    ) : BaseController() {
        // curl -X POST -H "Content-Type: application/json"  -d '{"username":"user","password":"pass"}' http://localhost:8443/user/signin
        @Operation(summary = "Sign in with username/password")
        @ApiResponses(
            value = [
                ApiResponse(responseCode = "200", description = "Signed in"),
                ApiResponse(responseCode = "401", description = "Invalid credentials"),
                ApiResponse(responseCode = "500", description = "Internal server error"),
            ],
        )
        @PostMapping("/signin")
        fun signIn(
            @RequestBody user: User,
        ): ResponseEntity<User> = ResponseEntity.ok(user)

        @Operation(summary = "Register new user")
        @ApiResponses(
            value = [
                ApiResponse(responseCode = "200", description = "User registered"),
                ApiResponse(responseCode = "409", description = "User already exists"),
                ApiResponse(responseCode = "500", description = "Internal server error"),
            ],
        )
        @PostMapping("/signup")
        fun signUp(
            @RequestBody user: User,
        ): ResponseEntity<String> =
            try {
                userService.signUp(user)
                ResponseEntity.ok(mapper.writeValueAsString(user))
            } catch (ex: IllegalArgumentException) {
                logger.error("Failed to sign up user due to validation error: ${ex.message}", ex)
                throw ResponseStatusException(HttpStatus.CONFLICT, "User already exists.")
            } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
                logger.error("Failed to sign up user due to data integrity violation: ${ex.message}", ex)
                throw ResponseStatusException(HttpStatus.CONFLICT, "User already exists.")
            } catch (ex: Exception) {
                logger.error("Unexpected error during user sign up: ${ex.message}", ex)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to sign up user: ${ex.message}", ex)
            }

//    @GetMapping(value = "/whoami")
//    public String whoami(HttpServletRequest req) throws JsonProcessingException {
//        return objectMapper.writeValueAsString(userService.whoami(req));
//    }
//
//    @GetMapping("/refresh")
//    public String refresh(HttpServletRequest req) {
//        return userService.refresh(req.getRemoteUser());
//    }
    }
