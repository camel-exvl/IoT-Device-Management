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
import org.springframework.web.bind.annotation.*
import pers.camel.iotdm.ResponseStructure
import pers.camel.iotdm.login.utils.RememberMeService

@RestController
@CrossOrigin(origins = ["http://localhost:8000"])
@RequestMapping("/api/user")
@Tag(name = "User", description = "User management")
class UserController(
    @Autowired val userRepo: UserRepo,
    val rememberMeService: RememberMeService
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
            if (!validateUsername(createUserData.username)) {
                log.warn("Create user: $createUserData failed: username length should be between 6 and 20")
                ret.success = false
                ret.code = HttpStatus.BAD_REQUEST.value()
                ret.errorMessage = "username length should be between 6 and 20"
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.BAD_REQUEST)
            }
            if (!validateEmail(createUserData.email)) {
                log.warn("Create user: $createUserData failed: invalid email format")
                ret.success = false
                ret.code = HttpStatus.BAD_REQUEST.value()
                ret.errorMessage = "invalid email format"
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.BAD_REQUEST)
            }
            if (!validatePassword(createUserData.password)) {
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
        var userId: String = "", var username: String = "", var email: String = ""
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
            val data = rememberMeService.autoLogin(request, response)
            if (data != null) {
                val username = (data.principal as User).username
                val user = userRepo.findByUsername(username)
                return if (user != null) {
                    log.info("Get user info success: $username")
                    ret.success = true
                    ret.code = HttpStatus.OK.value()
                    ret.data = UserInfo(userId = user.id.toString(), username = user.username, email = user.email)
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

    data class ModifyUserData(
        var username: String = "", var email: String = ""
    )

    @Operation(summary = "Modify user info")
    @ApiResponses(
        value = [ApiResponse(responseCode = "200", description = "OK"), ApiResponse(
            responseCode = "400",
            description = "Request Body Invalid"
        ), ApiResponse(responseCode = "404", description = "User not found"), ApiResponse(
            responseCode = "409",
            description = "Same username or email already exists"
        ), ApiResponse(responseCode = "500", description = "Internal Server Error")]
    )
    @PutMapping("/modify")
    fun modify(
        @RequestBody modifyUserDataInput: ModifyUserData, request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<Nothing>> {
        val ret = ResponseStructure<Nothing>()
        try {
            val loginData = rememberMeService.autoLogin(request, response)
            if (loginData != null) {
                val username = (loginData.principal as User).username
                val user = userRepo.findByUsername(username)
                if (user != null) {
                    // validate username and email
                    if (!validateUsername(modifyUserDataInput.username)) {
                        log.warn("Modify user info: $modifyUserDataInput failed: username length should be between 6 and 20")
                        ret.success = false
                        ret.code = HttpStatus.BAD_REQUEST.value()
                        ret.errorMessage = "username length should be between 6 and 20"
                        return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.BAD_REQUEST)
                    }
                    if (!validateEmail(modifyUserDataInput.email)) {
                        log.warn("Modify user info: $modifyUserDataInput failed: invalid email format")
                        ret.success = false
                        ret.code = HttpStatus.BAD_REQUEST.value()
                        ret.errorMessage = "invalid email format"
                        return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.BAD_REQUEST)
                    }

                    // check if username or email already exists
                    val userWithSameUsername = userRepo.findByUsername(modifyUserDataInput.username)
                    if (userWithSameUsername != null && userWithSameUsername.id != user.id) {
                        log.warn("Modify user info: $modifyUserDataInput failed: username already exists")
                        ret.success = false
                        ret.code = HttpStatus.CONFLICT.value()
                        ret.errorMessage = "username already exists"
                        return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.CONFLICT)
                    }
                    val userWithSameEmail = userRepo.findByEmail(modifyUserDataInput.email)
                    if (userWithSameEmail != null && userWithSameEmail.id != user.id) {
                        log.warn("Modify user info: $modifyUserDataInput failed: email already exists")
                        ret.success = false
                        ret.code = HttpStatus.CONFLICT.value()
                        ret.errorMessage = "email already exists"
                        return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.CONFLICT)
                    }

                    // delete remember-me cookie
                    rememberMeService.logout(request, response, loginData)

                    // update user info
                    user.username = modifyUserDataInput.username
                    user.email = modifyUserDataInput.email
                    userRepo.save(user)

                    log.info("Modify user info: $modifyUserDataInput success")
                    ret.success = true
                    ret.code = HttpStatus.OK.value()
                    return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.OK)
                } else {
                    log.error("Modify user info: $modifyUserDataInput failed: user $username not found")
                    ret.success = false
                    ret.code = HttpStatus.NOT_FOUND.value()
                    ret.errorMessage = "user not found"
                    return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.NOT_FOUND)
                }
            } else {
                log.error("Modify user info: $modifyUserDataInput failed: user not found")
                ret.success = false
                ret.code = HttpStatus.NOT_FOUND.value()
                ret.errorMessage = "user not found"
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.NOT_FOUND)
            }
        } catch (e: Exception) {
            log.error("Modify user info: $modifyUserDataInput failed: $e")
            ret.success = false
            ret.code = HttpStatus.INTERNAL_SERVER_ERROR.value()
            ret.errorMessage = "internal server error"
            return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    data class ModifyPasswordData(
        var oldPassword: String = "", var newPassword: String = ""
    )

    @Operation(summary = "Modify password")
    @ApiResponses(
        value = [ApiResponse(responseCode = "200", description = "OK"), ApiResponse(
            responseCode = "400",
            description = "Request Body Invalid"
        ), ApiResponse(responseCode = "401", description = "Wrong password"), ApiResponse(
            responseCode = "500",
            description = "Internal Server Error"
        )]
    )
    @PutMapping("/modifyPassword")
    fun modifyPassword(
        @RequestBody modifyPasswordData: ModifyPasswordData, request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<Nothing>> {
        val ret = ResponseStructure<Nothing>()
        try {
            val loginData = rememberMeService.autoLogin(request, response)
            if (loginData != null) {
                val username = (loginData.principal as User).username
                val user = userRepo.findByUsername(username)
                if (user != null) {
                    // validate password
                    if (!validatePassword(modifyPasswordData.newPassword)) {
                        log.warn("Modify password: $modifyPasswordData failed: password length should be between 6 and 20")
                        ret.success = false
                        ret.code = HttpStatus.BAD_REQUEST.value()
                        ret.errorMessage = "password length should be between 6 and 20"
                        return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.BAD_REQUEST)
                    }

                    // check if old password is correct
                    val passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
                    if (!passwordEncoder.matches(modifyPasswordData.oldPassword, user.password)) {
                        log.warn("Modify password: $modifyPasswordData failed: wrong password")
                        ret.success = false
                        ret.code = HttpStatus.UNAUTHORIZED.value()
                        ret.errorMessage = "wrong password"
                        return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.UNAUTHORIZED)
                    }

                    // delete remember-me cookie
                    rememberMeService.logout(request, response, loginData)

                    // update password
                    user.password = passwordEncoder.encode(modifyPasswordData.newPassword)
                    userRepo.save(user)

                    log.info("Modify password: $modifyPasswordData success")
                    ret.success = true
                    ret.code = HttpStatus.OK.value()
                    return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.OK)
                } else {
                    log.error("Modify password: $modifyPasswordData failed: user $username not found")
                    ret.success = false
                    ret.code = HttpStatus.NOT_FOUND.value()
                    ret.errorMessage = "user not found"
                    return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.NOT_FOUND)
                }
            } else {
                log.error("Modify password: $modifyPasswordData failed: user not found")
                ret.success = false
                ret.code = HttpStatus.NOT_FOUND.value()
                ret.errorMessage = "user not found"
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.NOT_FOUND)
            }
        } catch (e: Exception) {
            log.error("Modify password: $modifyPasswordData failed: $e")
            ret.success = false
            ret.code = HttpStatus.INTERNAL_SERVER_ERROR.value()
            ret.errorMessage = "internal server error"
            return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @Operation(summary = "Delete user")
    @ApiResponses(
        value = [ApiResponse(responseCode = "200", description = "OK"), ApiResponse(
            responseCode = "500",
            description = "Internal Server Error"
        )]
    )
    @DeleteMapping("/delete")
    fun delete(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<Nothing>> {
        val ret = ResponseStructure<Nothing>()
        try {
            val loginData = rememberMeService.autoLogin(request, response)
            if (loginData != null) {
                val username = (loginData.principal as User).username
                val user = userRepo.findByUsername(username)
                return if (user != null) {
                    userRepo.delete(user)
                    log.info("Delete user: $username success")
                    ret.success = true
                    ret.code = HttpStatus.OK.value()
                    ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.OK)
                } else {
                    log.error("Delete user: $username failed: user not found")
                    ret.success = false
                    ret.code = HttpStatus.NOT_FOUND.value()
                    ret.errorMessage = "user not found"
                    ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.NOT_FOUND)
                }
            } else {
                log.error("Delete user failed: user not found")
                ret.success = false
                ret.code = HttpStatus.NOT_FOUND.value()
                ret.errorMessage = "user not found"
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.NOT_FOUND)
            }
        } catch (e: Exception) {
            log.error("Delete user failed: $e")
            ret.success = false
            ret.code = HttpStatus.INTERNAL_SERVER_ERROR.value()
            ret.errorMessage = "internal server error"
            return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}