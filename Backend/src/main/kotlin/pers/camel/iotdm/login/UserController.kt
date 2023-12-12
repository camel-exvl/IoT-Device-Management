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
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.web.bind.annotation.*
import pers.camel.iotdm.ResponseStructure
import pers.camel.iotdm.login.User
import pers.camel.iotdm.login.utils.RememberMeService
import org.springframework.security.core.userdetails.User as SecurityUser

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
                log.warn("Create user failed: username length should be between 6 and 20.")
                ret.success = false
                ret.code = HttpStatus.BAD_REQUEST.value()
                ret.errorMessage = "Username length should be between 6 and 20."
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.BAD_REQUEST)
            }
            if (!validateEmail(createUserData.email)) {
                log.warn("Create user failed: ${createUserData.email} is not a valid email address.")
                ret.success = false
                ret.code = HttpStatus.BAD_REQUEST.value()
                ret.errorMessage = "Invalid email format."
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.BAD_REQUEST)
            }
            if (!validatePassword(createUserData.password)) {
                log.warn("Create user failed: password length should be between 6 and 20.")
                ret.success = false
                ret.code = HttpStatus.BAD_REQUEST.value()
                ret.errorMessage = "Password length should be between 6 and 20."
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.BAD_REQUEST)
            }

            // hash password
            val passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
            createUserData.password = passwordEncoder.encode(createUserData.password)

            val user = User(
                username = createUserData.username, email = createUserData.email, password = createUserData.password
            )
            userRepo.insert(user)
            log.info("Create user: $user success.")
            ret.success = true
            ret.code = HttpStatus.CREATED.value()
            return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.CREATED)
        } catch (e: DuplicateKeyException) {
            val duplicateKey = e.message?.substringAfter("IoT.user index: ")?.substringBefore(" dup key")
            return if (duplicateKey != null) {
                log.warn("Create user failed: Duplicate key: $duplicateKey. $e")
                ret.success = false
                ret.code = HttpStatus.CONFLICT.value()
                ret.errorMessage = "$duplicateKey already exists."
                ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.CONFLICT)
            } else {
                log.error("Create user failed: $e")
                ret.success = false
                ret.code = HttpStatus.INTERNAL_SERVER_ERROR.value()
                ret.errorMessage = "Internal server error."
                ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
            }
        } catch (e: Exception) {
            log.error("Create user failed: $e")
            ret.success = false
            ret.code = HttpStatus.INTERNAL_SERVER_ERROR.value()
            ret.errorMessage = "Internal server error."
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

    fun getCurrentUser(request: HttpServletRequest, response: HttpServletResponse): User {
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

    data class UserInfo(
        var userId: ObjectId = ObjectId(), var username: String = "", var email: String = ""
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
            return try {
                val user = getCurrentUser(request, response)
                ret.data = UserInfo(userId = user.id, username = user.username, email = user.email)
                log.info("Get user info success: ${user.id}")
                ret.success = true
                ret.code = HttpStatus.OK.value()
                ResponseEntity<ResponseStructure<UserInfo>>(ret, HttpStatus.OK)
            } catch (e: Exception) {
                log.error("Get user info failed: user not found.")
                ret.success = false
                ret.code = HttpStatus.NOT_FOUND.value()
                ret.errorMessage = "User not found."
                ResponseEntity<ResponseStructure<UserInfo>>(ret, HttpStatus.NOT_FOUND)
            }
        } catch (e: Exception) {
            log.error("Get user info failed: $e")
            ret.success = false
            ret.code = HttpStatus.INTERNAL_SERVER_ERROR.value()
            ret.errorMessage = "Internal server error."
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
        @RequestBody modifyUserData: ModifyUserData, request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<Nothing>> {
        val ret = ResponseStructure<Nothing>()
        try {
            try {
                val user = getCurrentUser(request, response)

                // validate username and email
                if (!validateUsername(modifyUserData.username)) {
                    log.warn("Modify user info: $modifyUserData failed: username length should be between 6 and 20.")
                    ret.success = false
                    ret.code = HttpStatus.BAD_REQUEST.value()
                    ret.errorMessage = "Username length should be between 6 and 20."
                    return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.BAD_REQUEST)
                }
                if (!validateEmail(modifyUserData.email)) {
                    log.warn("Modify user info failed: ${modifyUserData.email} is not a valid email address.")
                    ret.success = false
                    ret.code = HttpStatus.BAD_REQUEST.value()
                    ret.errorMessage = "Invalid email format."
                    return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.BAD_REQUEST)
                }

                // check if username or email already exists
                val userWithSameUsername = userRepo.findByUsername(modifyUserData.username)
                val userWithSameEmail = userRepo.findByEmail(modifyUserData.email)
                if (userWithSameUsername != null && userWithSameUsername.id != user.id) {
                    log.warn("Modify user info failed: username ${modifyUserData.username} already exists.")
                    ret.success = false
                    ret.code = HttpStatus.CONFLICT.value()
                    ret.errorMessage = "Username already exists."
                    return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.CONFLICT)
                }
                if (userWithSameEmail != null && userWithSameEmail.id != user.id) {
                    log.warn("Modify user info failed: email ${modifyUserData.email} already exists.")
                    ret.success = false
                    ret.code = HttpStatus.CONFLICT.value()
                    ret.errorMessage = "Email already exists."
                    return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.CONFLICT)
                }

                // update user info
                user.username = modifyUserData.username
                user.email = modifyUserData.email
                userRepo.save(user)

                log.info("Modify user info: success.")
                ret.success = true
                ret.code = HttpStatus.OK.value()
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.OK)
            } catch (e: Exception) {
                log.error("Modify user info failed: user not found.")
                ret.success = false
                ret.code = HttpStatus.NOT_FOUND.value()
                ret.errorMessage = "User not found."
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.NOT_FOUND)
            }
        } catch (e: Exception) {
            log.error("Modify user info failed: $e")
            ret.success = false
            ret.code = HttpStatus.INTERNAL_SERVER_ERROR.value()
            ret.errorMessage = "Internal server error."
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
            try {
                val user = getCurrentUser(request, response)

                // validate password
                if (!validatePassword(modifyPasswordData.newPassword)) {
                    log.warn("Modify password failed: password length should be between 6 and 20.")
                    ret.success = false
                    ret.code = HttpStatus.BAD_REQUEST.value()
                    ret.errorMessage = "Password length should be between 6 and 20."
                    return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.BAD_REQUEST)
                }

                // check if old password is correct
                val passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
                if (!passwordEncoder.matches(modifyPasswordData.oldPassword, user.password)) {
                    log.warn("Modify password failed: wrong password.")
                    ret.success = false
                    ret.code = HttpStatus.UNAUTHORIZED.value()
                    ret.errorMessage = "Wrong password."
                    return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.UNAUTHORIZED)
                }

                // delete remember-me cookie
                rememberMeService.logout(request, response, null)

                // update password
                user.password = passwordEncoder.encode(modifyPasswordData.newPassword)
                userRepo.save(user)

                log.info("Modify password success.")
                ret.success = true
                ret.code = HttpStatus.OK.value()
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.OK)
            } catch (e: Exception) {
                log.error("Modify password failed: user not found.")
                ret.success = false
                ret.code = HttpStatus.NOT_FOUND.value()
                ret.errorMessage = "User not found."
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.NOT_FOUND)
            }
        } catch (e: Exception) {
            log.error("Modify password failed: $e")
            ret.success = false
            ret.code = HttpStatus.INTERNAL_SERVER_ERROR.value()
            ret.errorMessage = "Internal server error."
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
            try {
                val user = getCurrentUser(request, response)

                // delete remember-me cookie
                rememberMeService.logout(request, response, null)

                // delete user
                userRepo.delete(user)

                log.info("Delete user success: ${user.id}")
                ret.success = true
                ret.code = HttpStatus.OK.value()
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.OK)
            } catch (e: Exception) {
                log.error("Delete user failed: user not found.")
                ret.success = false
                ret.code = HttpStatus.NOT_FOUND.value()
                ret.errorMessage = "User not found."
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.NOT_FOUND)
            }
        } catch (e: Exception) {
            log.error("Delete user failed: $e")
            ret.success = false
            ret.code = HttpStatus.INTERNAL_SERVER_ERROR.value()
            ret.errorMessage = "Internal server error."
            return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}