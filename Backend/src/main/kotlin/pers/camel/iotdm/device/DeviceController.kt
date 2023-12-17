package pers.camel.iotdm.device

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pers.camel.iotdm.ResponseStructure
import pers.camel.iotdm.login.User
import pers.camel.iotdm.login.UserController
import pers.camel.iotdm.login.UserRepo

@RestController
@RequestMapping("/api/device")
@Tag(name = "device", description = "Device management")
class DeviceController(
    @Autowired val userRepo: UserRepo,
    @Autowired val userController: UserController
) {
    private final val log = LogFactory.getLog(UserController::class.java)

    data class DeviceListData(
        val id: String,
        val name: String,
        val type: Short,
        val description: String
    )

    @Operation(summary = "Search devices")
    @GetMapping("/search")
    fun devices(
        name: String?,
        type: Short?,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<List<DeviceListData>>> {
        val ret = ResponseStructure<List<DeviceListData>>()
        try {
            return try {
                val user = userController.getCurrentUser(request, response)
                val filter = user.devices.filter {
                    (name == null || it.name.contains(name)) &&
                            (type == null || it.type == type)
                }

                log.info("Get all devices success: ${user.id}")
                ret.success = true
                ret.code = HttpStatus.OK.value()
                ret.data = filter.map {
                    DeviceListData(
                        id = it.id.toString(),
                        name = it.name,
                        type = it.type,
                        description = it.description
                    )
                }
                ResponseEntity<ResponseStructure<List<DeviceListData>>>(ret, HttpStatus.OK)
            } catch (e: Exception) {
                log.error("Get all devices failed: User not found.")
                ret.success = false
                ret.code = HttpStatus.NOT_FOUND.value()
                ret.errorMessage = "User not found."
                ResponseEntity<ResponseStructure<List<DeviceListData>>>(ret, HttpStatus.NOT_FOUND)
            }
        } catch (e: Exception) {
            log.error("Get all devices failed: $e")
            ret.success = false
            ret.code = HttpStatus.INTERNAL_SERVER_ERROR.value()
            ret.errorMessage = "Internal server error."
            return ResponseEntity<ResponseStructure<List<DeviceListData>>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    data class CreateDeviceData(
        val name: String,
        val type: Short,
        val description: String?
    )

    private fun validateDeviceType(type: Short): Boolean {
        return type in 0..DeviceType.values().size
    }

    @Operation(summary = "Create a device")
    @PostMapping("/create")
    fun create(
        @RequestBody deviceData: CreateDeviceData,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<User.Device>> {
        val ret = ResponseStructure<User.Device>()
        try {
            try {
                val user = userController.getCurrentUser(request, response)

                if (!validateDeviceType(deviceData.type)) {
                    log.error("Create device failed: Invalid device type.")
                    ret.success = false
                    ret.code = HttpStatus.BAD_REQUEST.value()
                    ret.errorMessage = "Invalid device type."
                    return ResponseEntity<ResponseStructure<User.Device>>(ret, HttpStatus.BAD_REQUEST)
                }

                val device = User.Device(
                    name = deviceData.name,
                    type = deviceData.type,
                    description = deviceData.description ?: ""
                )
                user.devices = user.devices.plus(device)
                userRepo.save(user)

                log.info("Create device success: ${user.id}")
                ret.success = true
                ret.code = HttpStatus.OK.value()
                ret.data = device
                return ResponseEntity<ResponseStructure<User.Device>>(ret, HttpStatus.OK)
            } catch (e: Exception) {
                log.error("Create device failed: User not found.")
                ret.success = false
                ret.code = HttpStatus.NOT_FOUND.value()
                ret.errorMessage = "User not found."
                return ResponseEntity<ResponseStructure<User.Device>>(ret, HttpStatus.NOT_FOUND)
            }
        } catch (e: Exception) {
            log.error("Create device failed: $e")
            ret.success = false
            ret.code = HttpStatus.INTERNAL_SERVER_ERROR.value()
            ret.errorMessage = "Internal server error."
            return ResponseEntity<ResponseStructure<User.Device>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    data class ModifyDeviceData(
        val id: String,
        val name: String,
        val type: Short,
        val description: String
    )

    @Operation(summary = "Modify a device")
    @PutMapping("/modify")
    fun modify(
        @RequestBody deviceData: ModifyDeviceData,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<User.Device>> {
        val ret = ResponseStructure<User.Device>()
        try {
            try {
                val user = userController.getCurrentUser(request, response)

                if (!validateDeviceType(deviceData.type)) {
                    log.error("Modify device failed: Invalid device type.")
                    ret.success = false
                    ret.code = HttpStatus.BAD_REQUEST.value()
                    ret.errorMessage = "Invalid device type."
                    return ResponseEntity<ResponseStructure<User.Device>>(ret, HttpStatus.BAD_REQUEST)
                }

                val device =
                    user.devices.find { it.id.toString() == deviceData.id } ?: throw Exception("Device not found.")
                device.name = deviceData.name
                device.type = deviceData.type
                device.description = deviceData.description
                userRepo.save(user)

                log.info("Modify device success: ${device.id}")
                ret.success = true
                ret.code = HttpStatus.OK.value()
                ret.data = device
                return ResponseEntity<ResponseStructure<User.Device>>(ret, HttpStatus.OK)
            } catch (e: Exception) {
                log.error("Modify device failed: $e")
                ret.success = false
                ret.code = HttpStatus.NOT_FOUND.value()
                ret.errorMessage = e.message ?: "Not found."
                return ResponseEntity<ResponseStructure<User.Device>>(ret, HttpStatus.NOT_FOUND)
            }
        } catch (e: Exception) {
            log.error("Modify device failed: $e")
            ret.success = false
            ret.code = HttpStatus.INTERNAL_SERVER_ERROR.value()
            ret.errorMessage = "Internal server error."
            return ResponseEntity<ResponseStructure<User.Device>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @Operation(summary = "Delete a device")
    @DeleteMapping("/delete")
    fun delete(
        @RequestParam("id") id: String,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ResponseStructure<User.Device>> {
        val ret = ResponseStructure<User.Device>()
        try {
            try {
                val user = userController.getCurrentUser(request, response)

                val device = user.devices.find { it.id.toString() == id } ?: throw Exception("Device not found.")
                user.devices = user.devices.filter { it.id.toString() != id }
                userRepo.save(user)

                log.info("Delete device success: ${device.id}")
                ret.success = true
                ret.code = HttpStatus.OK.value()
                ret.data = device
                return ResponseEntity<ResponseStructure<User.Device>>(ret, HttpStatus.OK)
            } catch (e: Exception) {
                log.error("Delete device failed: $e")
                ret.success = false
                ret.code = HttpStatus.NOT_FOUND.value()
                ret.errorMessage = e.message ?: "Not found."
                return ResponseEntity<ResponseStructure<User.Device>>(ret, HttpStatus.NOT_FOUND)
            }
        } catch (e: Exception) {
            log.error("Delete device failed: $e")
            ret.success = false
            ret.code = HttpStatus.INTERNAL_SERVER_ERROR.value()
            ret.errorMessage = "Internal server error."
            return ResponseEntity<ResponseStructure<User.Device>>(ret, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}