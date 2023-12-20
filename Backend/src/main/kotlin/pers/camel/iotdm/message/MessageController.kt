package pers.camel.iotdm.message

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.logging.LogFactory
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pers.camel.iotdm.ResponseStructure
import pers.camel.iotdm.login.UserRepo
import pers.camel.iotdm.login.getCurrentUser
import pers.camel.iotdm.login.utils.RememberMeService

@RestController
@RequestMapping("/api/message")
@Tag(name = "Message", description = "Message management")
class MessageController(
    @Autowired val userRepo: UserRepo,
    @Autowired val messageRepo: MessageRepo,
    @Autowired val rememberMeService: RememberMeService
) {
    private final val log = LogFactory.getLog(MessageController::class.java)

    @Operation(summary = "Create a message")
    @PostMapping("/create")
    fun create(
        @RequestBody message: Message, request: HttpServletRequest, response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<Nothing>> {
        return try {
            val user = userRepo.findById(message.userID.toString()).get()
            val device = user.devices.find { it.id == message.deviceID }
            if (device != null) {
                device.messages += message.id
                userRepo.save(user)
            } else {
                val ret = ResponseStructure(false, "Device not found.", HttpStatus.NOT_FOUND.value(), null)
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.NOT_FOUND)
            }

            messageRepo.insert(message)
            log.debug("Create message success.")
            val ret = ResponseStructure(true, "", HttpStatus.CREATED.value(), null)
            ResponseEntity(ret, HttpStatus.CREATED)
        } catch (e: Exception) {
            log.error("Create message failed: $e")
            val ret = ResponseStructure(
                false, "Internal server error.", HttpStatus.INTERNAL_SERVER_ERROR.value(), null
            )
            ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    data class MessageListData(
        val info: String,
        val value: Long,
        val alert: Boolean,
        val lng: Double,
        val lat: Double,
        val time: Long
    )

    data class MessageList(val messages: List<MessageListData>, val total: Long)

    @Operation(summary = "Get message list of a device")
    @GetMapping("/list")
    fun list(
        @RequestParam("deviceID") deviceID: String,
        @RequestParam("pageNum") pageNum: Int,
        @RequestParam("pageSize") pageSize: Int,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<MessageList>> {
        return try {
            val pageable = Pageable.ofSize(pageSize).withPage(pageNum)
            val messages = messageRepo.findAllByDeviceID(ObjectId(deviceID), pageable)
            val messageListData = messages.map {
                MessageListData(it.info, it.value, it.alert, it.lng, it.lat, it.time)
            }
            log.debug("Get message list success.")
            val ret = ResponseStructure(
                true, "", HttpStatus.OK.value(),
                MessageList(messageListData.content, messages.totalElements)
            )
            ResponseEntity(ret, HttpStatus.OK)
        } catch (e: Exception) {
            log.error("Get message list failed: $e")
            val ret = ResponseStructure<MessageList>(
                false,
                "Internal server error.",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                null
            )
            ResponseEntity<ResponseStructure<MessageList>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @Operation(summary = "Delete all messages of a device")
    @DeleteMapping("/delete")
    fun delete(
        @RequestParam("deviceID") deviceID: String,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<Nothing>> {
        return try {
            val user = getCurrentUser(request, response, userRepo, rememberMeService)
            val device = user.devices.find { it.id == ObjectId(deviceID) }
            if (device != null) {
                messageRepo.deleteAllByDeviceID(ObjectId(deviceID))
                device.messages = listOf()
                userRepo.save(user)
                log.debug("Delete messages success.")
                val ret = ResponseStructure(true, "", HttpStatus.OK.value(), null)
                ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.OK)
            } else {
                log.warn("Delete messages failed: device not found.")
                val ret = ResponseStructure(false, "Device not found.", HttpStatus.NOT_FOUND.value(), null)
                ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.NOT_FOUND)
            }
        } catch (e: Exception) {
            log.error("Delete messages failed: $e")
            val ret = ResponseStructure(
                false, "Internal server error.", HttpStatus.INTERNAL_SERVER_ERROR.value(), null
            )
            ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}