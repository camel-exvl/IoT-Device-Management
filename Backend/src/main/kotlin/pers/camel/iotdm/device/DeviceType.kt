package pers.camel.iotdm.device

enum class DeviceType(val value: Short) {
    SENSOR(0),
    SMARTHOME(1),
    ACTUATOR(2),
    CONTROLLER(3),
    GATEWAY(4),
    TERMINAL(5),
    EMBEDDED(6),
    OTHER(7)
}