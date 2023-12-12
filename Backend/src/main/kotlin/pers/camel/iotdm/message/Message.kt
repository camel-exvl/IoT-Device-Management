package pers.camel.iotdm.message

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document("message")
data class Message(
    @Field("deviceID")
    val deviceID: ObjectId = ObjectId(),
    @Field("info")
    val info: String = "",
    @Field("value")
    val value: Long = 0,
    @Field("alert")
    val alert: Boolean = false,
    @Field("lng")
    val lng: Double = 0.0,
    @Field("lat")
    val lat: Double = 0.0,
    @Field("timestamp")
    val time: Long = System.currentTimeMillis()
) {
    @Id
    var id: ObjectId = ObjectId()
}
