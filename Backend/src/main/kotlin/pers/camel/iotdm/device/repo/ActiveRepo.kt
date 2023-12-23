package pers.camel.iotdm.device.repo

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import pers.camel.iotdm.device.entity.ActiveDevice

interface ActiveRepo : MongoRepository<ActiveDevice, String> {
    fun findByUserIDAndHour(userID: ObjectId, hour: Long): ActiveDevice?
    fun findAllByUserID(userID: ObjectId): List<ActiveDevice>
}