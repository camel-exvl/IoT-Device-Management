package pers.camel.iotdm

data class ResponseStructure<T>(
    var success: Boolean = false,
    var errorMessage: String = "",
    var code: Int = 0,
    var data: T? = null
)
