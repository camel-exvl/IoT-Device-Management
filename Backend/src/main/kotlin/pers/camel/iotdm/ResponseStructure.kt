package pers.camel.iotdm

data class ResponseStructure(
    var success: Boolean = false,
    var errorMessage: String = "",
    var code: Int = 0,
    var data: Any? = null
)
