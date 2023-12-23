package pers.camel.iotdm.login

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.web.bind.annotation.*
import pers.camel.iotdm.ResponseStructure
import pers.camel.iotdm.login.entity.User
import pers.camel.iotdm.login.repo.UserRepo
import pers.camel.iotdm.login.utils.RememberMeService
import org.springframework.security.core.userdetails.User as SecurityUser

fun getCurrentUser(
    request: HttpServletRequest,
    response: HttpServletResponse,
    userRepo: UserRepo,
    rememberMeService: RememberMeService
): User {
    val data = rememberMeService.autoLogin(request, response)
    if (data != null) {
        val id = (data.principal as SecurityUser).username
        val user = userRepo.findById(id)
        return if (user.isEmpty) {
            throw Exception("User not found")
        } else {
            user.get()
        }
    } else {
        throw Exception("User not found")
    }
}

@RestController
@RequestMapping("/api/user")
@Tag(name = "User", description = "User management")
class UserController(
    @Autowired val userRepo: UserRepo, val rememberMeService: RememberMeService
) {
    private final val log = LogFactory.getLog(UserController::class.java)

    data class CreateUserData(
        var username: String = "", var email: String = "", var password: String = ""
    )

    private fun validateUsername(username: String): Boolean {
        return username.length in 6..20
    }

    private fun validateEmail(email: String): Boolean {
        val emailRegex = Regex("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+\$")
        return emailRegex.matches(email)
    }

    private fun validatePassword(password: String): Boolean {
        return password.length in 6..20
    }

    @Operation(summary = "Create a new user")
    @PostMapping("/register")
    fun register(@RequestBody createUserData: CreateUserData): ResponseEntity<ResponseStructure<Nothing>> {
        try {
            // validate username, email and password
            if (!validateUsername(createUserData.username)) {
                log.warn("Create user failed: username length should be between 6 and 20.")
                val ret = ResponseStructure(
                    false, "Username length should be between 6 and 20.", HttpStatus.BAD_REQUEST.value(), null
                )
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.BAD_REQUEST)
            }
            if (!validateEmail(createUserData.email)) {
                log.warn("Create user failed: ${createUserData.email} is not a valid email address.")
                val ret = ResponseStructure(
                    false, "Invalid email format.", HttpStatus.BAD_REQUEST.value(), null
                )
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.BAD_REQUEST)
            }
            if (!validatePassword(createUserData.password)) {
                log.warn("Create user failed: password length should be between 6 and 20.")
                val ret = ResponseStructure(
                    false, "Password length should be between 6 and 20.", HttpStatus.BAD_REQUEST.value(), null
                )
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.BAD_REQUEST)
            }

            // hash password
            val passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
            createUserData.password = passwordEncoder.encode(createUserData.password)

            val user = User(
                username = createUserData.username, email = createUserData.email, password = createUserData.password
            )
            userRepo.insert(user)
            log.debug("Create user: $user success.")
            val ret = ResponseStructure(true, "", HttpStatus.CREATED.value(), null)
            return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.CREATED)
        } catch (e: DuplicateKeyException) {
            val duplicateKey = e.message?.substringAfter("IoT.user index: ")?.substringBefore(" dup key")
            return if (duplicateKey != null) {
                log.warn("Create user failed: Duplicate key: $duplicateKey. $e")
                val ret = ResponseStructure(
                    false, "$duplicateKey already exists.", HttpStatus.CONFLICT.value(), null
                )
                ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.CONFLICT)
            } else {
                log.error("Create user failed: $e")
                val ret = ResponseStructure(
                    false, "Internal server error.", HttpStatus.INTERNAL_SERVER_ERROR.value(), null
                )
                ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
            }
        } catch (e: Exception) {
            log.error("Create user failed: $e")
            val ret = ResponseStructure(
                false, "Internal server error.", HttpStatus.INTERNAL_SERVER_ERROR.value(), null
            )
            return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    data class LoginData(
        var username: String, var password: String, var rememberMe: Boolean = false
    )

    @Operation(summary = "Login")
    @PostMapping("/login")
    fun login(
        @RequestBody data: LoginData, request: HttpServletRequest, response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<Nothing>> {
        val ret = ResponseStructure<Nothing>()
        return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.MOVED_PERMANENTLY)
    }

    data class UserInfo(
        var userId: String, var username: String, var email: String
    )

    @Operation(summary = "Get user info")
    @GetMapping("/current")
    fun current(
        request: HttpServletRequest, response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<UserInfo>> {
        try {
            return try {
                val user = getCurrentUser(request, response, userRepo, rememberMeService)
                log.debug("Get user info success: ${user.id}")
                val ret = ResponseStructure<UserInfo>(true, "", HttpStatus.OK.value(), null)
                ret.data = UserInfo(userId = user.id.toString(), username = user.username, email = user.email)
                ResponseEntity<ResponseStructure<UserInfo>>(ret, HttpStatus.OK)
            } catch (e: Exception) {
                log.error("Get user info failed: User not found.")
                val ret = ResponseStructure<UserInfo>(false, "User not found.", HttpStatus.NOT_FOUND.value(), null)
                ResponseEntity<ResponseStructure<UserInfo>>(ret, HttpStatus.NOT_FOUND)
            }
        } catch (e: Exception) {
            log.error("Get user info failed: $e")
            val ret = ResponseStructure<UserInfo>(
                false, "Internal server error.", HttpStatus.INTERNAL_SERVER_ERROR.value(), null
            )
            return ResponseEntity<ResponseStructure<UserInfo>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @Operation(summary = "Logout")
    @GetMapping("/logout")
    fun logout(
        request: HttpServletRequest, response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<Nothing>> {
        val ret = ResponseStructure<Nothing>()
        return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.MOVED_PERMANENTLY)
    }

    data class ModifyUserData(
        var username: String = "", var email: String = ""
    )

    @Operation(summary = "Modify user info")
    @PutMapping("/modify")
    fun modify(
        @RequestBody modifyUserData: ModifyUserData, request: HttpServletRequest, response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<Nothing>> {
        try {
            try {
                val user = getCurrentUser(request, response, userRepo, rememberMeService)

                // validate username and email
                if (!validateUsername(modifyUserData.username)) {
                    log.warn("Modify user info: $modifyUserData failed: username length should be between 6 and 20.")
                    val ret = ResponseStructure(
                        false, "Username length should be between 6 and 20.", HttpStatus.BAD_REQUEST.value(), null
                    )
                    return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.BAD_REQUEST)
                }
                if (!validateEmail(modifyUserData.email)) {
                    log.warn("Modify user info failed: ${modifyUserData.email} is not a valid email address.")
                    val ret = ResponseStructure(
                        false, "Invalid email format.", HttpStatus.BAD_REQUEST.value(), null
                    )
                    return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.BAD_REQUEST)
                }

                // check if username or email already exists
                val userWithSameUsername = userRepo.findByUsername(modifyUserData.username)
                val userWithSameEmail = userRepo.findByEmail(modifyUserData.email)
                if (userWithSameUsername != null && userWithSameUsername.id != user.id) {
                    log.warn("Modify user info failed: Username ${modifyUserData.username} already exists.")
                    val ret = ResponseStructure(
                        false, "Username already exists.", HttpStatus.CONFLICT.value(), null
                    )
                    return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.CONFLICT)
                }
                if (userWithSameEmail != null && userWithSameEmail.id != user.id) {
                    log.warn("Modify user info failed: Email ${modifyUserData.email} already exists.")
                    val ret = ResponseStructure(
                        false, "Email already exists.", HttpStatus.CONFLICT.value(), null
                    )
                    return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.CONFLICT)
                }

                // update user info
                user.username = modifyUserData.username
                user.email = modifyUserData.email
                userRepo.save(user)

                log.debug("Modify user info: success.")
                val ret = ResponseStructure(true, "", HttpStatus.OK.value(), null)
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.OK)
            } catch (e: Exception) {
                log.error("Modify user info failed: User not found.")
                val ret = ResponseStructure(
                    false, "User not found.", HttpStatus.NOT_FOUND.value(), null
                )
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.NOT_FOUND)
            }
        } catch (e: Exception) {
            log.error("Modify user info failed: $e")
            val ret = ResponseStructure(
                false, "Internal server error.", HttpStatus.INTERNAL_SERVER_ERROR.value(), null
            )
            return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    data class ModifyPasswordData(
        var oldPassword: String = "", var newPassword: String = ""
    )

    @Operation(summary = "Modify password")
    @PutMapping("/modifyPassword")
    fun modifyPassword(
        @RequestBody modifyPasswordData: ModifyPasswordData, request: HttpServletRequest, response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<Nothing>> {
        try {
            try {
                val user = getCurrentUser(request, response, userRepo, rememberMeService)

                // validate password
                if (!validatePassword(modifyPasswordData.newPassword)) {
                    log.warn("Modify password failed: Password length should be between 6 and 20.")
                    val ret = ResponseStructure(
                        false, "Password length should be between 6 and 20.", HttpStatus.BAD_REQUEST.value(), null
                    )
                    return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.BAD_REQUEST)
                }

                // check if old password is correct
                val passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
                if (!passwordEncoder.matches(modifyPasswordData.oldPassword, user.password)) {
                    log.warn("Modify password failed: Wrong password.")
                    val ret = ResponseStructure(
                        false, "Wrong password.", HttpStatus.UNAUTHORIZED.value(), null
                    )
                    return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.UNAUTHORIZED)
                }

                // delete remember-me cookie
                rememberMeService.logout(request, response, null)

                // update password
                user.password = passwordEncoder.encode(modifyPasswordData.newPassword)
                userRepo.save(user)

                log.debug("Modify password success.")
                val ret = ResponseStructure(true, "", HttpStatus.OK.value(), null)
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.OK)
            } catch (e: Exception) {
                log.error("Modify password failed: User not found.")
                val ret = ResponseStructure(
                    false, "User not found.", HttpStatus.NOT_FOUND.value(), null
                )
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.NOT_FOUND)
            }
        } catch (e: Exception) {
            log.error("Modify password failed: $e")
            val ret = ResponseStructure(
                false, "Internal server error.", HttpStatus.INTERNAL_SERVER_ERROR.value(), null
            )
            return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @Operation(summary = "Delete user")
    @DeleteMapping("/delete")
    fun delete(
        request: HttpServletRequest, response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<Nothing>> {
        try {
            try {
                val user = getCurrentUser(request, response, userRepo, rememberMeService)

                // delete remember-me cookie
                rememberMeService.logout(request, response, null)

                // delete user
                userRepo.delete(user)

                log.debug("Delete user success: ${user.id}")
                val ret = ResponseStructure(true, "", HttpStatus.OK.value(), null)
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.OK)
            } catch (e: Exception) {
                log.error("Delete user failed: User not found.")
                val ret = ResponseStructure(
                    false, "User not found.", HttpStatus.NOT_FOUND.value(), null
                )
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.NOT_FOUND)
            }
        } catch (e: Exception) {
            log.error("Delete user failed: $e")
            val ret = ResponseStructure(
                false, "Internal server error.", HttpStatus.INTERNAL_SERVER_ERROR.value(), null
            )
            return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}