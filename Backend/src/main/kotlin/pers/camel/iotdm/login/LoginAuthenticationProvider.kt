package pers.camel.iotdm.login

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.factory.PasswordEncoderFactories

class LoginAuthenticationProvider(@Autowired private val userRepo: UserRepo) : AuthenticationProvider {
    override fun authenticate(authentication: Authentication): Authentication {
        val username = authentication.name
        val password = authentication.credentials.toString()
        val user = userRepo.findByUsername(username) ?: throw BadCredentialsException("User not found")
        val passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
        if (!passwordEncoder.matches(password, user.password)) {
            throw BadCredentialsException("Wrong password")
        }
        return UsernamePasswordAuthenticationToken(username, password, authentication.authorities)
    }

    override fun supports(authentication: Class<*>): Boolean {
        return authentication == UsernamePasswordAuthenticationToken::class.java
    }
}