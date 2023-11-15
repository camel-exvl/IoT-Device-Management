package pers.camel.iotdm

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.RememberMeAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.authority.AuthorityUtils.createAuthorityList
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.stereotype.Service
import pers.camel.iotdm.login.AuthenticationFilter
import pers.camel.iotdm.login.LoginAuthenticationProvider
import pers.camel.iotdm.login.UserRepo


@Configuration
@EnableWebSecurity
class SpringSecurityConfig(@Autowired private val userRepo: UserRepo) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val authenticationFilter = AuthenticationFilter(authenticationManager())
        authenticationFilter.rememberMeServices = rememberMeServices()
        authenticationFilter.setAuthenticationSuccessHandler(SuccessHandler())
        authenticationFilter.setAuthenticationFailureHandler(FailureHandler())
        val objectMapper = ObjectMapper()
        http
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/api/user/login").permitAll()
                    .requestMatchers("/api/user/create").permitAll()
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers("api-docs/**").permitAll()
                    .anyRequest().authenticated()
            }
            // TODO: set csrf
            .csrf { csrf ->
                csrf.disable()
            }
            .httpBasic { basic ->
                basic.authenticationEntryPoint { request, response, authException ->
                    response.contentType = "application/json;charset=UTF-8"
                    response.status = HttpServletResponse.SC_UNAUTHORIZED
                    val out = response.writer
                    val ret = ResponseStructure()
                    ret.success = false
                    ret.code = HttpStatus.UNAUTHORIZED.value()
                    ret.errorMessage = "User not logged in"
                    out.write(objectMapper.writeValueAsString(ret))
                    out.flush()
                    out.close()
                }
            }
            .rememberMe { remember ->
                remember.rememberMeServices(rememberMeServices())
            }
            .logout { logout ->
                logout.logoutUrl("/api/user/logout").logoutSuccessHandler { request, response, authentication ->
                    response.contentType = "application/json;charset=UTF-8"
                    response.status = HttpServletResponse.SC_OK
                    val out = response.writer
                    val ret = ResponseStructure()
                    ret.success = true
                    ret.code = HttpStatus.OK.value()
                    out.write(objectMapper.writeValueAsString(ret))
                    out.flush()
                    out.close()
                }.deleteCookies("remember-me")
            }
            .addFilterBefore(authenticationFilter, BasicAuthenticationFilter::class.java)
        return http.build()
    }

    @Bean
    fun authenticationManager(): AuthenticationManager {
        return ProviderManager(LoginAuthenticationProvider(userRepo), rememberMeAuthenticationProvider())
    }

    @Service
    class UserDetailsServiceImpl(@Autowired private val userRepo: UserRepo) : UserDetailsService {
        override fun loadUserByUsername(username: String): User {
            val user = userRepo.findByUsername(username) ?: throw UsernameNotFoundException("User not found")
            return User(user.username, user.password, createAuthorityList("USER"))
        }
    }

    private val key = "remember-me"

    @Bean
    fun rememberMeFilter(): RememberMeAuthenticationFilter {
        return RememberMeAuthenticationFilter(authenticationManager(), rememberMeServices())
    }

    @Bean
    fun rememberMeServices(): TokenBasedRememberMeServices {
        val rememberMeServices = TokenBasedRememberMeServices(key, UserDetailsServiceImpl(userRepo))
        rememberMeServices.setAlwaysRemember(true)
        rememberMeServices.setTokenValiditySeconds(60 * 60 * 24 * 7)
        rememberMeServices.setCookieName("remember-me")
        return rememberMeServices
    }

    @Bean
    fun rememberMeAuthenticationProvider(): RememberMeAuthenticationProvider {
        return RememberMeAuthenticationProvider(key)
    }

    class SuccessHandler : AuthenticationSuccessHandler {
        override fun onAuthenticationSuccess(
            request: HttpServletRequest,
            response: HttpServletResponse,
            authentication: Authentication
        ) {
            response.contentType = "application/json;charset=UTF-8"
            response.status = HttpServletResponse.SC_OK
            val out = response.writer
            val ret = ResponseStructure()
            ret.success = true
            ret.code = HttpStatus.OK.value()
            out.write(ObjectMapper().writeValueAsString(ret))
            out.flush()
            out.close()
        }
    }

    class FailureHandler : AuthenticationFailureHandler {

        private val log = LogFactory.getLog(FailureHandler::class.java)

        override fun onAuthenticationFailure(
            request: HttpServletRequest,
            response: HttpServletResponse,
            exception: AuthenticationException
        ) {
            log.warn("Authentication failure: ${exception.message}")
            response.contentType = "application/json;charset=UTF-8"
            val ret = ResponseStructure()
            val out = response.writer
            if (exception is BadCredentialsException) {
                if (exception.message == "User not found") {
                    response.status = HttpServletResponse.SC_NOT_FOUND
                    ret.success = false
                    ret.code = HttpStatus.NOT_FOUND.value()
                    ret.errorMessage = "user not found"
                } else {
                    response.status = HttpServletResponse.SC_UNAUTHORIZED
                    ret.success = false
                    ret.code = HttpStatus.UNAUTHORIZED.value()
                    ret.errorMessage = "wrong password"
                }
            } else {
                response.status = HttpServletResponse.SC_UNAUTHORIZED
                ret.success = false
                ret.code = HttpStatus.UNAUTHORIZED.value()
                ret.errorMessage = "User not logged in"
            }
            out.write(ObjectMapper().writeValueAsString(ret))
            out.flush()
            out.close()
        }
    }
}