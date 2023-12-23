package pers.camel.iotdm.login.repo

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import pers.camel.iotdm.login.entity.User

@Repository
interface UserRepo : MongoRepository<User, String> {
    fun findByUsername(username: String): User?
    fun findByEmail(email: String): User?
    fun insert(user: User): User
}