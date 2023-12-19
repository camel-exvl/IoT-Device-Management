package pers.camel.iotdm.message

import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface MessageRepo : MongoRepository<Message, String> {
    fun insert(message: Message): Message
    fun findAllByDeviceID(deviceID: ObjectId, pageable: Pageable): Page<Message>
    fun deleteAllByDeviceID(deviceID: ObjectId)
}