package pers.camel.iotdm.login.utils

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices

class RememberMeService : TokenBasedRememberMeServices {
    constructor(key: String, userDetailsService: UserDetailsService) : super(key, userDetailsService) {
        super.setAlwaysRemember(true)
        super.setCookieName("remember-me")
    }

    constructor(
        key: String,
        userDetailsService: UserDetailsService,
        encodingAlgorithm: RememberMeTokenAlgorithm
    ) : super(key, userDetailsService, encodingAlgorithm) {
        super.setAlwaysRemember(true)
        super.setCookieName("remember-me")
    }

    override fun onLoginSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        successfulAuthentication: Authentication
    ) {
        val text = request.inputStream.bufferedReader().readText()
        val objectMapper = ObjectMapper()
        val map = objectMapper.readValue(text, Map::class.java)

        val rememberMe = map["rememberMe"] as Boolean
        if (rememberMe) {
            super.setTokenValiditySeconds(60 * 60 * 24 * 7)
        } else {
            super.setTokenValiditySeconds(-1)
        }
        super.onLoginSuccess(request, response, successfulAuthentication)
    }
}