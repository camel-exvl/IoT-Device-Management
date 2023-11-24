package pers.camel.iotdm.login.utils

import jakarta.servlet.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import org.springframework.util.StreamUtils
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader


class HttpRequestFilter : Filter {
    @Throws(IOException::class, ServletException::class)
    override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, chain: FilterChain) {
        val cachedRequestHttpServletRequest = CachedRequestHttpServletRequest(servletRequest as HttpServletRequest)
        chain.doFilter(cachedRequestHttpServletRequest, servletResponse)
    }

    private class CachedRequestHttpServletRequest(request: HttpServletRequest) : HttpServletRequestWrapper(request) {
        private val cachedBody: ByteArray

        init {
            cachedBody = StreamUtils.copyToByteArray(request.inputStream)
        }

        override fun getInputStream(): ServletInputStream {
            val inputStream = ByteArrayInputStream(cachedBody)
            return object : ServletInputStream() {
                override fun isFinished(): Boolean {
                    return inputStream.available() == 0
                }

                override fun isReady(): Boolean {
                    return true
                }

                override fun read(): Int {
                    return inputStream.read()
                }

                override fun setReadListener(readListener: ReadListener) {
                    throw UnsupportedOperationException()
                }
            }
        }

        override fun getReader(): BufferedReader {
            return BufferedReader(InputStreamReader(ByteArrayInputStream(cachedBody)))
        }
    }
}

