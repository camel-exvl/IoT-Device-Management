package pers.camelzy.iotdm.login

import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/user")
class UserService(@Autowired val userRepo: UserRepo) {
    private val log = LogFactory.getLog(UserService::class.java)

    @PostMapping("/create")
    fun create(
        @RequestParam params: Map<String, String>
    ): ResponseEntity<String> {
        val userData = UserData(
            params["username"] ?: "",
            params["email"] ?: "",
            params["password"] ?: ""
        )

        return try {
            // validate username, email and password
            if (userData.username.length < 6 || userData.username.length > 20) {
                log.warn("Create user: $userData failed: username length should be between 6 and 20")
                return ResponseEntity<String>(
                    "{\"msg\": \"username length should be between 6 and 20\"}",
                    HttpStatus.BAD_REQUEST
                )
            }
            val emailRegex = Regex("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+\$")
            if (!emailRegex.matches(userData.email)) {
                log.warn("Create user: $userData failed: invalid email format")
                return ResponseEntity<String>("{\"msg\": \"invalid email format\"}", HttpStatus.BAD_REQUEST)
            }
            if (userData.password.length < 6 || userData.password.length > 20) {
                log.warn("Create user: $userData failed: password length should be between 6 and 20")
                return ResponseEntity<String>(
                    "{\"msg\": \"password length should be between 6 and 20\"}",
                    HttpStatus.BAD_REQUEST
                )
            }

            // hash password
            val passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
            userData.password = passwordEncoder.encode(userData.password)

            userRepo.insert(userData)
            log.info("Create user: $userData success")
            ResponseEntity<String>("", HttpStatus.CREATED)
        } catch (e: DuplicateKeyException) {
            val duplicateKey = e.message?.substringAfter("IoT.user index: ")?.substringBefore(" dup key")
            if (duplicateKey != null) {
                log.warn("Create user: $userData failed: Duplicate key: $duplicateKey. $e")
                ResponseEntity<String>("{\"msg\": \"$duplicateKey already exists\"}", HttpStatus.CONFLICT)
            } else {
                ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR)
            }
        } catch (e: Exception) {
            log.error("Create user: $userData failed: $e")
            ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @PostMapping("/login")
    fun login(@RequestParam params: Map<String, String>): ResponseEntity<String> {
        val username = params["username"] ?: ""
        val password = params["password"] ?: ""

        return try {
            val userData = userRepo.findByUsername(username)
            if (userData == null) {
                log.warn("Login failed: user $username not found")
                ResponseEntity<String>("{\"msg\": \"user not found\"}", HttpStatus.NOT_FOUND)
            } else {
                val passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
                if (passwordEncoder.matches(password, userData.password)) {
                    log.info("Login success: $username")
                    ResponseEntity<String>("", HttpStatus.OK)
                } else {
                    log.warn("Login failed: wrong password for user $username")
                    ResponseEntity<String>("{\"msg\": \"wrong password\"}", HttpStatus.UNAUTHORIZED)
                }
            }
        } catch (e: Exception) {
            log.error("Login failed: $e")
            ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}