package pers.camelzy.iotdm.login

import com.fasterxml.jackson.annotation.JsonProperty
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document("user")
data class UserData(
    @JsonProperty("username")
    @Field("username")
    @Indexed(unique = true)
    var username: String = "",

    @JsonProperty("email")
    @Field("email")
    @Indexed(unique = true)
    var email: String = "",

    @JsonProperty("password")
    @Field("password")
    var password: String = ""
) {
    @Id
    var id: ObjectId = ObjectId()
}