package pers.camel.iotdm

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class IoTDeviceManagementApplication

fun main(args: Array<String>) {
    try {
        runApplication<IoTDeviceManagementApplication>(*args)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
