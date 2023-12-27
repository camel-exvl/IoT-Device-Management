package pers.camel.iotdm.login.utils

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices


class RememberMeService : TokenBasedRememberMeServices {

    constructor(key: String, userDetailsService: UserDetailsService, cookieDomain: String) : super(
        key,
        userDetailsService
    ) {
        super.setAlwaysRemember(true)
        super.setCookieName("remember-me")
        super.setCookieDomain(cookieDomain)
        super.setUseSecureCookie(true)
//        super.setUseSecureCookie(false)
    }

    constructor(
        key: String,
        userDetailsService: UserDetailsService,
        encodingAlgorithm: RememberMeTokenAlgorithm,
        cookieDomain: String
    ) : super(key, userDetailsService, encodingAlgorithm) {
        super.setAlwaysRemember(true)
        super.setCookieName("remember-me")
        super.setCookieDomain(cookieDomain)
        super.setUseSecureCookie(true)
//        super.setUseSecureCookie(false)
    }

    override fun onLoginSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        successfulAuthentication: Authentication
    ) {
        val text = request.inputStream.bufferedReader().readText()
        val objectMapper = ObjectMapper()
        val map = objectMapper.readValue(text, Map::class.java)

        val rememberMe = if (map["rememberMe"] == null) {
            false
        } else {
            map["rememberMe"] as Boolean
        }
        if (rememberMe) {
            super.setTokenValiditySeconds(60 * 60 * 24 * 7)
        } else {
            super.setTokenValiditySeconds(-1)
        }
        super.onLoginSuccess(request, response, successfulAuthentication)
    }

    override fun logout(request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication?) {
        super.logout(request, response, authentication)
        SecurityContextLogoutHandler().logout(request, null, authentication)    // logout from spring security
    }
}