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
import pers.camel.iotdm.device.entity.ActiveDevice
import pers.camel.iotdm.device.repo.ActiveRepo
import pers.camel.iotdm.login.getCurrentUser
import pers.camel.iotdm.login.repo.UserRepo
import pers.camel.iotdm.login.utils.RememberMeService
import pers.camel.iotdm.message.entity.Message
import pers.camel.iotdm.message.repo.MessageRepo

@RestController
@RequestMapping("/api/message")
@Tag(name = "Message", description = "Message management")
class MessageController(
    @Autowired val userRepo: UserRepo,
    @Autowired val messageRepo: MessageRepo,
    @Autowired val activeRepo: ActiveRepo,
    @Autowired val rememberMeService: RememberMeService
) {
    private final val log = LogFactory.getLog(MessageController::class.java)

    @Operation(summary = "Create a message")
    @PostMapping("/create")
    fun create(
        @RequestBody message: Message, request: HttpServletRequest, response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<Nothing>> {
        synchronized(this) {
            return try {
                // insert message ID into device
                val user = userRepo.findById(message.userID.toString()).get()
                val device = user.devices.find { it.id == message.deviceID }
                if (device != null) {
                    device.messages += message.id
                    userRepo.save(user)
                } else {
                    log.warn("Create message error: Device not found.")
                    val ret = ResponseStructure(false, "Device not found.", HttpStatus.NOT_FOUND.value(), null)
                    return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.NOT_FOUND)
                }

                messageRepo.insert(message)

                // update active device if necessary
                val hour = message.time / 3600000
                // if last message is not in the same hour, update active device
                if (device.messages.size == 1 || messageRepo.findById(device.messages[device.messages.size - 2].toString())
                        .get().time / 3600000 != hour
                ) {
                    val activeDevice = activeRepo.findByUserIDAndHour(message.userID, hour)
                    if (activeDevice != null) {
                        activeDevice.activeDevice += message.deviceID
                        activeRepo.save(activeDevice)
                    } else {
                        activeRepo.insert(
                            ActiveDevice(
                                message.userID,
                                hour,
                                setOf(message.deviceID),
                                (hour + 1) * 3600000
                            )
                        )
                    }
                }

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
    }

    data class MessageListData(
        val id: String,
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
        @RequestParam("timeAsc") timeAsc: Boolean,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<MessageList>> {
        return try {
            val pageable = Pageable.ofSize(pageSize).withPage(pageNum)
            val messages = if (timeAsc) {
                messageRepo.findAllByDeviceIDOrderByTimeAsc(ObjectId(deviceID), pageable)
            } else {
                messageRepo.findAllByDeviceIDOrderByTimeDesc(ObjectId(deviceID), pageable)
            }
            val messageListData = messages.map {
                MessageListData(it.id.toString(), it.info, it.value, it.alert, it.lng, it.lat, it.time)
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
    @DeleteMapping("/delete/all")
    fun deleteAll(
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

    @Operation(summary = "Delete bulk messages")
    @DeleteMapping("/delete/bulk")
    fun deleteBulk(
        @RequestParam("deviceID") deviceID: String,
        @RequestParam("messageID") messageID: List<String>,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<Nothing>> {
        return try {
            val user = getCurrentUser(request, response, userRepo, rememberMeService)
            val device = user.devices.find { it.id == ObjectId(deviceID) }
            if (device != null) {
                messageID.forEach {
                    device.messages -= ObjectId(it)
                }
                userRepo.save(user)
            } else {
                log.warn("Delete messages failed: device not found.")
                val ret = ResponseStructure(false, "Device not found.", HttpStatus.NOT_FOUND.value(), null)
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.NOT_FOUND)
            }
            messageRepo.deleteAllById(messageID)
            log.debug("Delete messages success.")
            val ret = ResponseStructure(true, "", HttpStatus.OK.value(), null)
            ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.OK)
        } catch (e: Exception) {
            log.error("Delete messages failed: $e")
            val ret = ResponseStructure(
                false, "Internal server error.", HttpStatus.INTERNAL_SERVER_ERROR.value(), null
            )
            ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}