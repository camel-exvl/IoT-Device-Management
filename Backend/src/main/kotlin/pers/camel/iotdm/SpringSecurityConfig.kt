package pers.camel.iotdm

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.stereotype.Service
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import pers.camel.iotdm.login.repo.UserRepo
import pers.camel.iotdm.login.utils.AuthenticationFilter
import pers.camel.iotdm.login.utils.HttpRequestFilter
import pers.camel.iotdm.login.utils.LoginAuthenticationProvider
import pers.camel.iotdm.login.utils.RememberMeService


@Configuration
@EnableWebSecurity
class SpringSecurityConfig(@Autowired private val userRepo: UserRepo) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val authenticationFilter = AuthenticationFilter(userRepo, authenticationManager())
        authenticationFilter.rememberMeServices = rememberMeServices()
        authenticationFilter.setAuthenticationSuccessHandler(SuccessHandler())
        authenticationFilter.setAuthenticationFailureHandler(FailureHandler())
        val objectMapper = ObjectMapper()
        http
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/api/user/login").permitAll()
                    .requestMatchers("/api/user/register").permitAll()
                    .requestMatchers("/api/message/create").permitAll()
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers("api-docs/**").permitAll()
                    .anyRequest().authenticated()
            }
            // TODO: set csrf
            .csrf { csrf ->
                csrf.disable()
            }
            .cors { cors ->
                cors.configurationSource(corsConfigurationSource())
            }
            .httpBasic { basic ->
                basic.authenticationEntryPoint { request, response, authException ->
                    response.contentType = "application/json;charset=UTF-8"
                    response.status = HttpServletResponse.SC_UNAUTHORIZED
                    val out = response.writer
                    val ret = ResponseStructure(false, "User not logged in", HttpStatus.UNAUTHORIZED.value(), null)
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
                    val ret = ResponseStructure(true, "", HttpStatus.OK.value(), null)
                    out.write(objectMapper.writeValueAsString(ret))
                    out.flush()
                    out.close()
                }.deleteCookies("remember-me")
            }
            .addFilterBefore(authenticationFilter, BasicAuthenticationFilter::class.java)
            .addFilterBefore(HttpRequestFilter(), AuthenticationFilter::class.java) // 用于包装请求，原始请求无法重复读取body 不知道是什么诡异设计
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("http://localhost:5173")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun authenticationManager(): AuthenticationManager {
        return ProviderManager(LoginAuthenticationProvider(userRepo), rememberMeAuthenticationProvider())
    }

    @Service
    class UserDetailsServiceImpl(@Autowired private val userRepo: UserRepo) : UserDetailsService {
        // username, password 实际上存储的是 id, password
        override fun loadUserByUsername(id: String): User {
            val user = userRepo.findById(id)
            if (user.isEmpty) {
                throw UsernameNotFoundException("User not found")
            }
            return User(user.get().id.toString(), user.get().password, createAuthorityList("USER"))
        }
    }

    @Value("\${rememberMe.key}")
    private lateinit var key: String

    @Value("\${rememberMe.cookieDomain}")
    private lateinit var cookieDomain: String

    @Bean
    fun rememberMeFilter(): RememberMeAuthenticationFilter {
        return RememberMeAuthenticationFilter(authenticationManager(), rememberMeServices())
    }

    @Bean
    fun rememberMeServices(): RememberMeService {
        return RememberMeService(key, UserDetailsServiceImpl(userRepo), cookieDomain)
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
            val ret = ResponseStructure(true, "", HttpStatus.OK.value(), null)
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
            val ret = ResponseStructure<Nothing>()
            val out = response.writer
            if (exception is BadCredentialsException) {
                when (exception.message) {
                    "User not found" -> {
                        response.status = HttpServletResponse.SC_NOT_FOUND
                        ret.success = false
                        ret.code = HttpStatus.NOT_FOUND.value()
                        ret.errorMessage = "User not found"
                    }

                    "Wrong password" -> {
                        response.status = HttpServletResponse.SC_UNAUTHORIZED
                        ret.success = false
                        ret.code = HttpStatus.UNAUTHORIZED.value()
                        ret.errorMessage = "Wrong password"
                    }

                    "Username is null" -> {
                        response.status = HttpServletResponse.SC_BAD_REQUEST
                        ret.success = false
                        ret.code = HttpStatus.BAD_REQUEST.value()
                        ret.errorMessage = "Username is null"
                    }

                    "Password is null" -> {
                        response.status = HttpServletResponse.SC_BAD_REQUEST
                        ret.success = false
                        ret.code = HttpStatus.BAD_REQUEST.value()
                        ret.errorMessage = "Password is null"
                    }

                    else -> {
                        response.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                        ret.success = false
                        ret.code = HttpStatus.INTERNAL_SERVER_ERROR.value()
                        ret.errorMessage = "Internal server error"
                    }
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