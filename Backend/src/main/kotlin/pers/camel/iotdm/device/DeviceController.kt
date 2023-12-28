package pers.camel.iotdm.device

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.logging.LogFactory
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pers.camel.iotdm.ResponseStructure
import pers.camel.iotdm.device.repo.ActiveRepo
import pers.camel.iotdm.login.entity.User
import pers.camel.iotdm.login.getCurrentUser
import pers.camel.iotdm.login.repo.UserRepo
import pers.camel.iotdm.login.utils.RememberMeService
import pers.camel.iotdm.message.repo.MessageRepo

@RestController
@RequestMapping("/api/device")
@Tag(name = "Device", description = "Device management")
class DeviceController(
    @Autowired val userRepo: UserRepo,
    @Autowired val messageRepo: MessageRepo,
    @Autowired val activeRepo: ActiveRepo,
    @Autowired val rememberMeService: RememberMeService
) {
    private final val log = LogFactory.getLog(DeviceController::class.java)

    data class DeviceTypeData(
        val type: Short, val num: Int
    )

    data class DeviceStatistics(
        val deviceCount: Int, val activeDeviceCount: Int, val messageCount: Int, val deviceType: List<DeviceTypeData>
    )

    @Operation(summary = "Get device statistics")
    @GetMapping("/statistics")
    fun statistics(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<DeviceStatistics>> {
        try {
            val user = getCurrentUser(request, response, userRepo, rememberMeService)
            val deviceCount = user.devices.size
            var activeDeviceCount =
                activeRepo.findByUserIDAndHour(user.id, System.currentTimeMillis() / 3600000)?.activeDevice?.size
            if (activeDeviceCount == null) {
                activeDeviceCount = 0
            }
            val messageCount = user.devices.sumOf { it.messages.size }
            val deviceType = user.devices.groupBy { it.type }.map { DeviceTypeData(it.key, it.value.size) }

            log.debug("Get device statistics success: ${user.id}")
            val ret = ResponseStructure<DeviceStatistics>(true, "", HttpStatus.OK.value(), null)
            ret.data = DeviceStatistics(deviceCount, activeDeviceCount, messageCount, deviceType)
            return ResponseEntity<ResponseStructure<DeviceStatistics>>(ret, HttpStatus.OK)
        } catch (e: Exception) {
            log.error("Get device statistics failed: $e")
            val ret = ResponseStructure<DeviceStatistics>(
                false, "Internal server error.", HttpStatus.INTERNAL_SERVER_ERROR.value(), null
            )
            return ResponseEntity<ResponseStructure<DeviceStatistics>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    data class ActiveDeviceNums(
        val time: Long, var activeNum: Long
    )

    @Operation(summary = "Get active device numbers")
    @GetMapping("/active")
    fun activeNums(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<List<ActiveDeviceNums>>> {
        try {
            val user = getCurrentUser(request, response, userRepo, rememberMeService)

            // initialize active device numbers in the last 24 hours
            val activeDeviceNums = mutableListOf<ActiveDeviceNums>()
            val currentTime = System.currentTimeMillis() / 3600000 * 3600000
            for (i in 0..23) {
                activeDeviceNums.add(ActiveDeviceNums(currentTime - 3600000 * (23 - i), 0))
            }

            // get active device numbers
            activeRepo.findAllByUserID(user.id).forEach {
                val hour = it.hour
                if (hour >= System.currentTimeMillis() / 3600000 - 23) {
                    activeDeviceNums[(hour - System.currentTimeMillis() / 3600000 + 23).toInt()].activeNum =
                        it.activeDevice.size.toLong()
                }
            }

            log.debug("Get active device numbers success: ${user.id}")
            val ret = ResponseStructure<List<ActiveDeviceNums>>(true, "", HttpStatus.OK.value(), null)
            ret.data = activeDeviceNums
            return ResponseEntity<ResponseStructure<List<ActiveDeviceNums>>>(ret, HttpStatus.OK)
        } catch (e: Exception) {
            log.error("Get device statistics failed: $e")
            val ret = ResponseStructure<List<ActiveDeviceNums>>(
                false,
                "Internal server error.",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                null
            )
            return ResponseEntity<ResponseStructure<List<ActiveDeviceNums>>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    data class DeviceListData(
        val id: String, val name: String, val type: Short, val description: String
    )

    @Operation(summary = "Search devices")
    @GetMapping("/search")
    fun search(
        name: String?, type: Short?, request: HttpServletRequest, response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<List<DeviceListData>>> {
        try {
            return try {
                val user = getCurrentUser(request, response, userRepo, rememberMeService)
                val filter = user.devices.filter {
                    (name == null || it.name.contains(name)) && (type == null || it.type == type)
                }

                log.debug("Get all devices success: ${user.id}")
                val ret = ResponseStructure<List<DeviceListData>>(true, "", HttpStatus.OK.value(), null)
                ret.data = filter.map {
                    DeviceListData(
                        id = it.id.toString(), name = it.name, type = it.type, description = it.description
                    )
                }
                ResponseEntity<ResponseStructure<List<DeviceListData>>>(ret, HttpStatus.OK)
            } catch (e: Exception) {
                log.error("Get all devices failed: User not found.")
                val ret = ResponseStructure<List<DeviceListData>>(
                    false, "User not found.", HttpStatus.NOT_FOUND.value(), null
                )
                ResponseEntity<ResponseStructure<List<DeviceListData>>>(ret, HttpStatus.NOT_FOUND)
            }
        } catch (e: Exception) {
            log.error("Get all devices failed: $e")
            val ret = ResponseStructure<List<DeviceListData>>(
                false, "Internal server error.", HttpStatus.INTERNAL_SERVER_ERROR.value(), null
            )
            return ResponseEntity<ResponseStructure<List<DeviceListData>>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    data class CreateDeviceData(
        val name: String, val type: Short, val description: String?
    )

    private fun validateDeviceType(type: Short): Boolean {
        return type in 0..DeviceType.values().size
    }

    @Operation(summary = "Create a device")
    @PostMapping("/create")
    fun create(
        @RequestBody deviceData: CreateDeviceData, request: HttpServletRequest, response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<Nothing>> {
        synchronized(this) {
            try {
                try {

                    val user = getCurrentUser(request, response, userRepo, rememberMeService)

                    if (!validateDeviceType(deviceData.type)) {
                        log.warn("Create device failed: Invalid device type.")
                        val ret = ResponseStructure(
                            false, "Invalid device type.", HttpStatus.BAD_REQUEST.value(), null
                        )
                        return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.BAD_REQUEST)
                    }

                    val device = User.Device(
                        name = deviceData.name, type = deviceData.type, description = deviceData.description ?: ""
                    )
                    user.devices = user.devices.plus(device)
                    userRepo.save(user)

                    log.debug("Create device success: ${user.id}")
                    val ret = ResponseStructure(true, "", HttpStatus.OK.value(), null)
                    return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.OK)
                } catch (e: Exception) {
                    log.error("Create device failed: User not found.")
                    val ret = ResponseStructure(false, "User not found.", HttpStatus.NOT_FOUND.value(), null)
                    return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.NOT_FOUND)
                }
            } catch (e: Exception) {
                log.error("Create device failed: $e")
                val ret = ResponseStructure(
                    false, "Internal server error.", HttpStatus.INTERNAL_SERVER_ERROR.value(), null
                )
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
            }
        }
    }

    data class ModifyDeviceData(
        val id: String, val name: String, val type: Short, val description: String
    )

    @Operation(summary = "Modify a device")
    @PutMapping("/modify")
    fun modify(
        @RequestBody deviceData: ModifyDeviceData, request: HttpServletRequest, response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<Nothing>> {
        synchronized(this) {
            try {
                try {
                    val user = getCurrentUser(request, response, userRepo, rememberMeService)

                    if (!validateDeviceType(deviceData.type)) {
                        log.warn("Modify device failed: Invalid device type.")
                        val ret = ResponseStructure(false, "Invalid device type.", HttpStatus.BAD_REQUEST.value(), null)
                        return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.BAD_REQUEST)
                    }

                    val device =
                        user.devices.find { it.id.toString() == deviceData.id } ?: throw Exception("Device not found.")
                    device.name = deviceData.name
                    device.type = deviceData.type
                    device.description = deviceData.description
                    userRepo.save(user)

                    log.debug("Modify device success: ${device.id}")
                    val ret = ResponseStructure(true, "", HttpStatus.OK.value(), null)
                    return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.OK)
                } catch (e: Exception) {
                    log.error("Modify device failed: $e")
                    val ret = ResponseStructure(false, e.message ?: "Not found.", HttpStatus.NOT_FOUND.value(), null)
                    return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.NOT_FOUND)
                }
            } catch (e: Exception) {
                log.error("Modify device failed: $e")
                val ret =
                    ResponseStructure(false, "Internal server error.", HttpStatus.INTERNAL_SERVER_ERROR.value(), null)
                return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
            }
        }
    }

    @Operation(summary = "Delete a device")
    @DeleteMapping("/delete")
    fun delete(
        @RequestParam("id") id: String, request: HttpServletRequest, response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<Nothing>> {
        try {
            return try {
                val user = getCurrentUser(request, response, userRepo, rememberMeService)

                // delete messages
                messageRepo.deleteAllByDeviceID(ObjectId(id))

                // get all active device and delete the device from the active device list
                activeRepo.findAll().forEach {
                    it.activeDevice = it.activeDevice.filter { it.toString() != id }.toSet()
                    activeRepo.save(it)
                }

                val device = user.devices.find { it.id.toString() == id } ?: throw Exception("Device not found.")
                user.devices = user.devices.filter { it.id.toString() != id }
                userRepo.save(user)

                log.debug("Delete device success: ${device.id}")
                val ret = ResponseStructure(true, "", HttpStatus.OK.value(), null)
                ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.OK)
            } catch (e: Exception) {
                log.error("Delete device failed: $e")
                val ret = ResponseStructure(false, e.message ?: "Not found.", HttpStatus.NOT_FOUND.value(), null)
                ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.NOT_FOUND)
            }
        } catch (e: Exception) {
            log.error("Delete device failed: $e")
            val ret = ResponseStructure(false, "Internal server error.", HttpStatus.INTERNAL_SERVER_ERROR.value(), null)
            return ResponseEntity<ResponseStructure<Nothing>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}