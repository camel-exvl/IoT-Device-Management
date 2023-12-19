package pers.camel.iotdm.message

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document("message")
data class Message(
    @Field("userID") val userID: ObjectId,
    @Field("deviceID") val deviceID: ObjectId,
    @Field("info") val info: String,
    @Field("value") val value: Long,
    @Field("alert") val alert: Boolean,
    @Field("lng") val lng: Double,
    @Field("lat") val lat: Double,
    @Field("timestamp") val time: Long
) {
    @Id
    var id: ObjectId = ObjectId()
}
