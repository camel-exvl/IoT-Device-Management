package pers.camel.iotdm.login

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepo : MongoRepository<UserData, String> {
    fun findByUsername(username: String): UserData?
    fun findByEmail(email: String): UserData?
    fun insert(user: UserData): UserData
}