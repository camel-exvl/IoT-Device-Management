package pers.camel.iotdm.message.repo

import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import pers.camel.iotdm.message.entity.Message

@Repository
interface MessageRepo : MongoRepository<Message, String> {
    fun insert(message: Message): Message
    fun findAllByDeviceIDOrderByTimeAsc(deviceID: ObjectId, pageable: Pageable): Page<Message>
    fun deleteAllByDeviceID(deviceID: ObjectId)
}