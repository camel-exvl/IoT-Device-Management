package pers.camelzy.iotdm.login

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/user")
@Tag(name = "User", description = "User management")
class UserService(@Autowired val userRepo: UserRepo) {
    private val log = LogFactory.getLog(UserService::class.java)

    data class CreateUserData(
        var username: String = "",
        var email: String = "",
        var password: String = ""
    )

    @Operation(summary = "Create a new user")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Created", content = [Content()]),
            ApiResponse(
                responseCode = "400",
                description = "Request Body Invalid",
                content = [Content(mediaType = "application/json")]
            ),
            ApiResponse(
                responseCode = "409",
                description = "Same username or email already exists",
                content = [Content(mediaType = "application/json")]
            ),
            ApiResponse(responseCode = "500", description = "Internal Server Error", content = [Content()])
        ]
    )
    @PostMapping("/create")
    fun create(
        @RequestBody createUserData: CreateUserData
    ): ResponseEntity<String> {
        val ret = mutableMapOf<String, String>()
        try {
            // validate username, email and password
            if (createUserData.username.length < 6 || createUserData.username.length > 20) {
                log.warn("Create user: $createUserData failed: username length should be between 6 and 20")
                ret["msg"] = "username length should be between 6 and 20"
                return ResponseEntity<String>(ObjectMapper().writeValueAsString(ret), HttpStatus.BAD_REQUEST)
            }
            val emailRegex = Regex("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+\$")
            if (!emailRegex.matches(createUserData.email)) {
                log.warn("Create user: $createUserData failed: invalid email format")
                ret["msg"] = "invalid email format"
                return ResponseEntity<String>(ObjectMapper().writeValueAsString(ret), HttpStatus.BAD_REQUEST)
            }
            if (createUserData.password.length < 6 || createUserData.password.length > 20) {
                log.warn("Create user: $createUserData failed: password length should be between 6 and 20")
                ret["msg"] = "password length should be between 6 and 20"
                return ResponseEntity<String>(ObjectMapper().writeValueAsString(ret), HttpStatus.BAD_REQUEST)
            }

            // hash password
            val passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
            createUserData.password = passwordEncoder.encode(createUserData.password)

            val userData = UserData(
                username = createUserData.username,
                email = createUserData.email,
                password = createUserData.password
            )
            userRepo.insert(userData)
            log.info("Create user: $userData success")
            return ResponseEntity<String>("", HttpStatus.CREATED)
        } catch (e: DuplicateKeyException) {
            val duplicateKey = e.message?.substringAfter("IoT.user index: ")?.substringBefore(" dup key")
            return if (duplicateKey != null) {
                log.warn("Create user: $createUserData failed: Duplicate key: $duplicateKey. $e")
                ResponseEntity<String>("{\"msg\": \"$duplicateKey already exists\"}", HttpStatus.CONFLICT)
            } else {
                ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR)
            }
        } catch (e: Exception) {
            log.error("Create user: $createUserData failed: $e")
            return ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    data class LoginData(
        var username: String = "",
        var password: String = ""
    )

    @Operation(summary = "Login")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "OK", content = [Content()]),
            ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = [Content(mediaType = "application/json")]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Wrong password",
                content = [Content(mediaType = "application/json")]
            ),
            ApiResponse(responseCode = "500", description = "Internal Server Error", content = [Content()])
        ]
    )
    @PostMapping("/login")
    fun login(@RequestBody data: LoginData): ResponseEntity<String> {
        val ret = mutableMapOf<String, String>()
        try {
            val userData = userRepo.findByUsername(data.username)
            return if (userData == null) {
                log.warn("Login failed: user $data.username not found")
                ret["msg"] = "user not found"
                ResponseEntity<String>(ObjectMapper().writeValueAsString(ret), HttpStatus.NOT_FOUND)
            } else {
                val passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
                if (passwordEncoder.matches(data.password, userData.password)) {
                    log.info("Login success: $data.username")
                    ResponseEntity<String>("", HttpStatus.OK)
                } else {
                    log.warn("Login failed: wrong password for user $data.username")
                    ret["msg"] = "wrong password"
                    ResponseEntity<String>(ObjectMapper().writeValueAsString(ret), HttpStatus.UNAUTHORIZED)
                }
            }
        } catch (e: Exception) {
            log.error("Login failed: $e")
            return ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}