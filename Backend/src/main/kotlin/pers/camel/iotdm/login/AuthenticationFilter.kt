package pers.camel.iotdm.login

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
import org.springframework.util.Assert


class AuthenticationFilter : AbstractAuthenticationProcessingFilter {
    private var usernameParameter = SPRING_SECURITY_FORM_USERNAME_KEY
    private var passwordParameter = SPRING_SECURITY_FORM_PASSWORD_KEY
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
    protected fun setDetails(request: HttpServletRequest?, authRequest: UsernamePasswordAuthenticationToken) {
        authRequest.details = authenticationDetailsSource.buildDetails(request)
    }

    /**
     * Sets the parameter name which will be used to obtain the username from the login
     * request.
     * @param usernameParameter the parameter name. Defaults to "username".
     */
    fun setUsernameParameter(usernameParameter: String) {
        Assert.hasText(usernameParameter, "Username parameter must not be empty or null")
        this.usernameParameter = usernameParameter
    }

    /**
     * Sets the parameter name which will be used to obtain the password from the login
     * request..
     * @param passwordParameter the parameter name. Defaults to "password".
     */
    fun setPasswordParameter(passwordParameter: String) {
        Assert.hasText(passwordParameter, "Password parameter must not be empty or null")
        this.passwordParameter = passwordParameter
    }

    /**
     * Defines whether only HTTP POST requests will be allowed by this filter. If set to
     * true, and an authentication request is received which is not a POST request, an
     * exception will be raised immediately and authentication will not be attempted. The
     * <tt>unsuccessfulAuthentication()</tt> method will be called as if handling a failed
     * authentication.
     *
     *
     * Defaults to <tt>true</tt> but may be overridden by subclasses.
     */
    fun setPostOnly(postOnly: Boolean) {
        this.postOnly = postOnly
    }

    fun getUsernameParameter(): String {
        return usernameParameter
    }

    fun getPasswordParameter(): String {
        return passwordParameter
    }

    companion object {
        const val SPRING_SECURITY_FORM_USERNAME_KEY = "username"
        const val SPRING_SECURITY_FORM_PASSWORD_KEY = "password"
        private val DEFAULT_ANT_PATH_REQUEST_MATCHER = AntPathRequestMatcher(
            "/api/user/login",
            "POST"
        )
    }
}

