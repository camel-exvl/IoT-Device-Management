package pers.camel.iotdm.login

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.web.authentication.RememberMeServices
import org.springframework.web.bind.annotation.*
import pers.camel.iotdm.ResponseStructure

@RestController
@CrossOrigin(origins = ["http://localhost:8000"])
@RequestMapping("/api/user")
@Tag(name = "User", description = "User management")
class UserService(
    @Autowired val userRepo: UserRepo,
    val rememberMeServices: RememberMeServices
) {
    private final val log = LogFactory.getLog(UserService::class.java)

    data class CreateUserData(
        var username: String = "", var email: String = "", var password: String = ""
    )

    @Operation(summary = "Create a new user")
    @ApiResponses(
        value = [ApiResponse(
            responseCode = "201", description = "Created", content = [Content(mediaType = "application/json")]
        ), ApiResponse(
            responseCode = "400",
            description = "Request Body Invalid",
            content = [Content(mediaType = "application/json")]
        ), ApiResponse(
            responseCode = "409",
            description = "Same username or email already exists",
            content = [Content(mediaType = "application/json")]
        ), ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content = [Content(mediaType = "application/json")]
        )]
    )
    @PostMapping("/create")
    fun create(@RequestBody createUserData: CreateUserData): ResponseEntity<ResponseStructure> {
        val ret = ResponseStructure()
        try {
            // validate username, email and password
            if (createUserData.username.length < 6 || createUserData.username.length > 20) {
                log.warn("Create user: $createUserData failed: username length should be between 6 and 20")
                ret.success = false
                ret.code = HttpStatus.BAD_REQUEST.value()
                ret.errorMessage = "username length should be between 6 and 20"
                return ResponseEntity<ResponseStructure>(ret, HttpStatus.BAD_REQUEST)
            }
            val emailRegex = Regex("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+\$")
            if (!emailRegex.matches(createUserData.email)) {
                log.warn("Create user: $createUserData failed: invalid email format")
                ret.success = false
                ret.code = HttpStatus.BAD_REQUEST.value()
                ret.errorMessage = "invalid email format"
                return ResponseEntity<ResponseStructure>(ret, HttpStatus.BAD_REQUEST)
            }
            if (createUserData.password.length < 6 || createUserData.password.length > 20) {
                log.warn("Create user: $createUserData failed: password length should be between 6 and 20")
                ret.success = false
                ret.code = HttpStatus.BAD_REQUEST.value()
                ret.errorMessage = "password length should be between 6 and 20"
                return ResponseEntity<ResponseStructure>(ret, HttpStatus.BAD_REQUEST)
            }

            // hash password
            val passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
            createUserData.password = passwordEncoder.encode(createUserData.password)

            val userData = UserData(
                username = createUserData.username, email = createUserData.email, password = createUserData.password
            )
            userRepo.insert(userData)
            log.info("Create user: $userData success")
            ret.success = true
            ret.code = HttpStatus.CREATED.value()
            return ResponseEntity<ResponseStructure>(ret, HttpStatus.CREATED)
        } catch (e: DuplicateKeyException) {
            val duplicateKey = e.message?.substringAfter("IoT.user index: ")?.substringBefore(" dup key")
            return if (duplicateKey != null) {
                log.warn("Create user: $createUserData failed: Duplicate key: $duplicateKey. $e")
                ret.success = false
                ret.code = HttpStatus.CONFLICT.value()
                ret.errorMessage = "$duplicateKey already exists"
                ResponseEntity<ResponseStructure>(ret, HttpStatus.CONFLICT)
            } else {
                log.error("Create user: $createUserData failed: $e")
                ret.success = false
                ret.code = HttpStatus.INTERNAL_SERVER_ERROR.value()
                ret.errorMessage = "internal server error"
                ResponseEntity<ResponseStructure>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
            }
        } catch (e: Exception) {
            log.error("Create user: $createUserData failed: $e")
            ret.success = false
            ret.code = HttpStatus.INTERNAL_SERVER_ERROR.value()
            ret.errorMessage = "internal server error"
            return ResponseEntity<ResponseStructure>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    data class LoginData(
        var username: String = "", var password: String = "", var rememberMe: Boolean = false
    )

    @Operation(summary = "Login")
    @ApiResponses(
        value = [ApiResponse(
            responseCode = "200", description = "OK", content = [Content(mediaType = "application/json")]
        ), ApiResponse(
            responseCode = "404", description = "User not found", content = [Content(mediaType = "application/json")]
        ), ApiResponse(
            responseCode = "401", description = "Wrong password", content = [Content(mediaType = "application/json")]
        ), ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content = [Content(mediaType = "application/json")]
        )]
    )
    @PostMapping("/login")
    fun login(
        @RequestBody data: LoginData,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ResponseStructure> {
        val ret = ResponseStructure()
        return ResponseEntity<ResponseStructure>(ret, HttpStatus.MOVED_PERMANENTLY)
    }

    data class UserInfo(
        var username: String = "", var email: String = ""
    )

    @Operation(summary = "Get user info")
    @ApiResponses(
        value = [ApiResponse(
            responseCode = "200", description = "OK", content = [Content(mediaType = "application/json")]
        ), ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content = [Content(mediaType = "application/json")]
        )]
    )
    @GetMapping("/current")
    fun current(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ResponseStructure> {
        val ret = ResponseStructure()
        try {
            val data = rememberMeServices.autoLogin(request, response)
            if (data != null) {
                val username = (data.principal as User).username
                val user = userRepo.findByUsername(username)
                return if (user != null) {
                    log.info("Get user info success: $username")
                    ret.success = true
                    ret.code = HttpStatus.OK.value()
                    ret.data = UserInfo(username = user.username, email = user.email)
                    ResponseEntity<ResponseStructure>(ret, HttpStatus.OK)
                } else {
                    log.error("Get user info failed: user $username not found")
                    ret.success = false
                    ret.code = HttpStatus.INTERNAL_SERVER_ERROR.value()
                    ret.errorMessage = "internal server error"
                    ResponseEntity<ResponseStructure>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
                }
            } else {
                log.error("Get user info failed: user not found")
                ret.success = false
                ret.code = HttpStatus.INTERNAL_SERVER_ERROR.value()
                ret.errorMessage = "internal server error"
                return ResponseEntity<ResponseStructure>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
            }
        } catch (e: Exception) {
            log.error("Get user info failed: $e")
            ret.success = false
            ret.code = HttpStatus.INTERNAL_SERVER_ERROR.value()
            ret.errorMessage = "internal server error"
            return ResponseEntity<ResponseStructure>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @Operation(summary = "Logout")
    @ApiResponses(
        value = [ApiResponse(
            responseCode = "200", description = "OK", content = [Content(mediaType = "application/json")]
        ), ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content = [Content(mediaType = "application/json")]
        )]
    )
    @GetMapping("/logout")
    fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ResponseStructure> {
        val ret = ResponseStructure()
        return ResponseEntity<ResponseStructure>(ret, HttpStatus.MOVED_PERMANENTLY)
    }
}