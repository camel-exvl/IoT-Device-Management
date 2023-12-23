package pers.camel.iotdm.login.entity

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import pers.camel.iotdm.device.DeviceType

@Document("user")
data class User(
    @Field("username")
    @Indexed(unique = true)
    var username: String = "",
    @Field("email")
    @Indexed(unique = true)
    var email: String = "",
    @Field("password")
    var password: String = ""
) {
    @Id
    var id: ObjectId = ObjectId()

    @Field("devices")
    var devices: List<Device> = listOf()

    data class Device(
        @Field("name")
        var name: String = "",
        @Field("type")
        var type: Short = DeviceType.OTHER.value,
        @Field("description")
        var description: String = ""
    ) {
        @Id
        var id: ObjectId = ObjectId()

        @Field("messages")
        var messages: List<ObjectId> = listOf()
    }
}

