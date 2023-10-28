package pers.camelzy.iotdm

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class IoTDeviceManagementApplication

fun main(args: Array<String>) {
    runApplication<IoTDeviceManagementApplication>(*args)
}
