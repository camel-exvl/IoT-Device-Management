package pers.camelzy.iotdm.login

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user")
class UserService(@Autowired val userRepo: UserRepo) {
    private val log = LogFactory.getLog(UserService::class.java)

    @GetMapping("/findByUsername")
    fun findByUsername(username: String): ResponseEntity<String> {
        return try {
            val userData = userRepo.findByUsername(username)
            if (userData != null) {
                log.info("Find user by username: $username success")
                ResponseEntity<String>(ObjectMapper().writeValueAsString(userData), HttpStatus.OK)
            } else {
                log.info("Find user by username: $username failed: not found")
                ResponseEntity<String>("", HttpStatus.NOT_FOUND)
            }
        } catch (e: Exception) {
            log.error("Find user by username: $username failed: $e")
            ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @GetMapping("/findByEmail")
    fun findByEmail(email: String): ResponseEntity<String> {
        return try {
            val userData = userRepo.findByEmail(email)
            if (userData != null) {
                log.info("Find user by email: $email success")
                ResponseEntity<String>(ObjectMapper().writeValueAsString(userData), HttpStatus.OK)
            } else {
                log.info("Find user by email: $email failed: not found")
                ResponseEntity<String>("", HttpStatus.NOT_FOUND)
            }
        } catch (e: Exception) {
            log.error("Find user by email: $email failed: $e")
            ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    // TODO： password should be encrypted while transmitting and storing
    @PostMapping("/insert")
    fun insert(@RequestBody user: String): ResponseEntity<String> {
        val userData = ObjectMapper().readValue(user, UserData::class.java)
        return try {
            userRepo.insert(userData)
            log.info("Insert user: $user success")
            ResponseEntity<String>("", HttpStatus.CREATED)
        } catch (e: DuplicateKeyException) {
            log.warn("Insert user: $user failed: duplicate key, $e")
            ResponseEntity<String>("", HttpStatus.CONFLICT)
        } catch (e: Exception) {
            log.error("Insert user: $user failed: $e")
            ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}