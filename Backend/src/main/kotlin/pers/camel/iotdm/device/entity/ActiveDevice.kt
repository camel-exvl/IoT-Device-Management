package pers.camel.iotdm.device.entity

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document("activeDevice")
data class ActiveDevice(
    @Field("userID") val userID: ObjectId,
    @Field("timestamp") val hour: Long,
    @Field("activeNum") var activeNum: Long,
    @Field("expire")
    @Indexed(expireAfter = "1d")
    val expire: Long
) {
    @Id
    var id: ObjectId = ObjectId()
}
