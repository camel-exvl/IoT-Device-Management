package pers.camel.iotdm.login.utils

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import pers.camel.iotdm.login.repo.UserRepo

class LoginAuthenticationProvider(@Autowired private val userRepo: UserRepo) : AuthenticationProvider {
    override fun authenticate(authentication: Authentication): Authentication {
        val id = authentication.name
        val password = authentication.credentials.toString()
        val user = userRepo.findById(id)
        if (user.isEmpty) {
            throw BadCredentialsException("User not found")
        }
        val passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
        if (!passwordEncoder.matches(password, user.get().password)) {
            throw BadCredentialsException("Wrong password")
        }
        return UsernamePasswordAuthenticationToken(id, password, authentication.authorities)
    }

    override fun supports(authentication: Class<*>): Boolean {
        return authentication == UsernamePasswordAuthenticationToken::class.java
    }
}