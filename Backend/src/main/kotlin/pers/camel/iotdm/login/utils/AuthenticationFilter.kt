package pers.camel.iotdm.login.utils

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter
import org.springframework.security.web.util.matcher.AntPathRequestMatcher


class AuthenticationFilter : AbstractAuthenticationProcessingFilter {
    private var postOnly = true

    constructor() : super(DEFAULT_ANT_PATH_REQUEST_MATCHER)

    constructor(authenticationManager: AuthenticationManager?) : super(
        DEFAULT_ANT_PATH_REQUEST_MATCHER,
        authenticationManager
    )

    @Throws(AuthenticationException::class)
    override fun attemptAuthentication(request: HttpServletRequest, response: HttpServletResponse): Authentication {
        if (postOnly && request.method != "POST") {
            throw AuthenticationServiceException("Authentication method not supported: " + request.method)
        }
        val text = request.inputStream.bufferedReader().readText()
        val objectMapper = ObjectMapper()
        val map = objectMapper.readValue(text, Map::class.java)

        val username = map["username"]
        val password = map["password"]
        val authRequest = UsernamePasswordAuthenticationToken.unauthenticated(
            username,
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

