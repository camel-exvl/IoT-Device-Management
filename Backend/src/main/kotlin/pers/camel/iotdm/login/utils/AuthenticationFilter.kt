package pers.camel.iotdm.login.utils

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import pers.camel.iotdm.login.repo.UserRepo


class AuthenticationFilter : AbstractAuthenticationProcessingFilter {
    private val postOnly = true
    private val userRepo: UserRepo

    constructor(userRepo: UserRepo) : super(DEFAULT_ANT_PATH_REQUEST_MATCHER) {
        this.userRepo = userRepo
    }

    constructor(userRepo: UserRepo, authenticationManager: AuthenticationManager?) : super(
        DEFAULT_ANT_PATH_REQUEST_MATCHER,
        authenticationManager
    ) {
        this.userRepo = userRepo
    }

    @Throws(AuthenticationException::class)
    override fun attemptAuthentication(request: HttpServletRequest, response: HttpServletResponse): Authentication {
        if (postOnly && request.method != "POST") {
            throw AuthenticationServiceException("Authentication method not supported: " + request.method)
        }
        val text = request.inputStream.bufferedReader().readText()
        val objectMapper = ObjectMapper()
        val map = objectMapper.readValue(text, Map::class.java)
        if (map["username"] == null) {
            throw BadCredentialsException("Username is null")
        } else if (map["password"] == null) {
            throw BadCredentialsException("Password is null")
        }

        val username = map["username"] as String
        val password = map["password"]
        val user = userRepo.findByUsername(username) ?: throw BadCredentialsException("User not found")
        val authRequest = UsernamePasswordAuthenticationToken.unauthenticated(
            user.id,
            password
        )
        // Allow subclasses to set the "details" property
        setDetails(request, authRequest)
        return authenticationManager.authenticate(authRequest)
    }

    /**
     * Provided so that subclasses may configure what is put into the authentication
     * request's details property.
     * @param request that an authentication request is being created for
     * @param authRequest the authentication request object that should have its details
     * set
     */
    private fun setDetails(request: HttpServletRequest?, authRequest: UsernamePasswordAuthenticationToken) {
        authRequest.details = authenticationDetailsSource.buildDetails(request)
    }

    companion object {
        private val DEFAULT_ANT_PATH_REQUEST_MATCHER = AntPathRequestMatcher(
            "/api/user/login",
            "POST"
        )
    }
}

