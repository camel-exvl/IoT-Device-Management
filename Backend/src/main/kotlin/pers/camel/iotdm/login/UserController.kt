package pers.camel.iotdm.login

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
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
class UserController(
    @Autowired val userRepo: UserRepo,
    val rememberMeServices: RememberMeServices
) {
    private final val log = LogFactory.getLog(UserController::class.java)

    data class CreateUserData(
        var username: String = "", var email: String = "", var password: String = ""
    )

    @Operation(summary = "Create a new user")
    @ApiResponses(
        value = [ApiResponse(responseCode = "201", description = "Created"), ApiResponse(
            responseCode = "400",
            description = "Request Body Invalid"
        ), ApiResponse(responseCode = "409", description = "Same username or email already exists"), ApiResponse(
            responseCode = "500",
            description = "Internal Server Error"
        )]
    )
    @PostMapping("/create")
    fun create(@RequestBody createUserData: CreateUserData): ResponseEntity<ResponseStructure<Nothing>> {
        val ret = ResponseStructure<Nothing>()
        try {
            // validate username, email and password
            if (createUserData.username.length < 6 || createUserData.username.length > 20) {
                log.warn("Create user: $createUserData failed: username length should be between 6 and 20")
                ret.success = false
                ret.code = HttpStatus.BAD_REQUEST.value()
                ret.errorMessage = "username length should be between 6 and 20"
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.BAD_REQUEST)
            }
            val emailRegex = Regex("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+\$")
            if (!emailRegex.matches(createUserData.email)) {
                log.warn("Create user: $createUserData failed: invalid email format")
                ret.success = false
                ret.code = HttpStatus.BAD_REQUEST.value()
                ret.errorMessage = "invalid email format"
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.BAD_REQUEST)
            }
            if (createUserData.password.length < 6 || createUserData.password.length > 20) {
                log.warn("Create user: $createUserData failed: password length should be between 6 and 20")
                ret.success = false
                ret.code = HttpStatus.BAD_REQUEST.value()
                ret.errorMessage = "password length should be between 6 and 20"
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.BAD_REQUEST)
            }

            // hash password
            val passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
            createUserData.password = passwordEncoder.encode(createUserData.password)

            val user = User(
                username = createUserData.username, email = createUserData.email, password = createUserData.password
            )
            userRepo.insert(user)
            log.info("Create user: $user success")
            ret.success = true
            ret.code = HttpStatus.CREATED.value()
            return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.CREATED)
        } catch (e: DuplicateKeyException) {
            val duplicateKey = e.message?.substringAfter("IoT.user index: ")?.substringBefore(" dup key")
            return if (duplicateKey != null) {
                log.warn("Create user: $createUserData failed: Duplicate key: $duplicateKey. $e")
                ret.success = false
                ret.code = HttpStatus.CONFLICT.value()
                ret.errorMessage = "$duplicateKey already exists"
                ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.CONFLICT)
            } else {
                log.error("Create user: $createUserData failed: $e")
                ret.success = false
                ret.code = HttpStatus.INTERNAL_SERVER_ERROR.value()
                ret.errorMessage = "internal server error"
                ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
            }
        } catch (e: Exception) {
            log.error("Create user: $createUserData failed: $e")
            ret.success = false
            ret.code = HttpStatus.INTERNAL_SERVER_ERROR.value()
            ret.errorMessage = "internal server error"
            return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    data class LoginData(
        var username: String = "", var password: String = "", var rememberMe: Boolean = false
    )

    @Operation(summary = "Login")
    @ApiResponses(
        value = [ApiResponse(responseCode = "200", description = "OK"), ApiResponse(
            responseCode = "404",
            description = "User not found"
        ), ApiResponse(responseCode = "401", description = "Wrong password"), ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
        )]
    )
    @PostMapping("/login")
    fun login(
        @RequestBody data: LoginData,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<Nothing>> {
        val ret = ResponseStructure<Nothing>()
        return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.MOVED_PERMANENTLY)
    }


    data class UserInfo(
        var username: String = "", var email: String = ""
    )

    @Operation(summary = "Get user info")
    @ApiResponses(
        value = [ApiResponse(
            responseCode = "200", description = "OK"
        ), ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content = [Content(schema = Schema(implementation = ResponseStructure::class))]
        )]
    )
    @GetMapping("/current")
    fun current(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<UserInfo>> {
        val ret = ResponseStructure<UserInfo>()
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
                    ResponseEntity<ResponseStructure<UserInfo>>(ret, HttpStatus.OK)
                } else {
                    log.error("Get user info failed: user $username not found")
                    ret.success = false
                    ret.code = HttpStatus.NOT_FOUND.value()
                    ret.errorMessage = "user not found"
                    ResponseEntity<ResponseStructure<UserInfo>>(ret, HttpStatus.NOT_FOUND)
                }
            } else {
                log.error("Get user info failed: user not found")
                ret.success = false
                ret.code = HttpStatus.NOT_FOUND.value()
                ret.errorMessage = "user not found"
                return ResponseEntity<ResponseStructure<UserInfo>>(ret, HttpStatus.NOT_FOUND)
            }
        } catch (e: Exception) {
            log.error("Get user info failed: $e")
            ret.success = false
            ret.code = HttpStatus.INTERNAL_SERVER_ERROR.value()
            ret.errorMessage = "internal server error"
            return ResponseEntity<ResponseStructure<UserInfo>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @Operation(summary = "Logout")
    @ApiResponses(
        value = [ApiResponse(responseCode = "200", description = "OK"), ApiResponse(
            responseCode = "500",
            description = "Internal Server Error"
        )]
    )
    @GetMapping("/logout")
    fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<Nothing>> {
        val ret = ResponseStructure<Nothing>()
        return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.MOVED_PERMANENTLY)
    }
}