package com.gracetv.iptv.data.repository

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

object NetworkErrorMapper {
    fun map(throwable: Throwable): String {
        return when (throwable) {
            is UnknownHostException -> "Unable to reach the server. Check your internet connection or server address."
            is SocketTimeoutException -> "Connection timed out. The IPTV server took too long to respond."
            is ConnectException -> "Could not connect to the IPTV server. Verify the host and port."
            is SSLException -> "Secure HTTPS handshake failed. Verify the server's SSL certificate or protocol."
            is HttpException -> when (throwable.code()) {
                401, 403 -> "Authentication failed. Please check your username and password."
                404 -> "Endpoint or playlist not found on the server (HTTP 404)."
                in 500..599 -> "The IPTV server returned a temporary error (HTTP ${throwable.code()})."
                else -> "Server returned an unexpected response (HTTP ${throwable.code()})."
            }
            is JsonDataException, is JsonEncodingException ->
                "The server returned an unexpected response format. Verify this is an Xtream-compatible API."
            is IOException -> "Network error while communicating with the server. Please try again."
            else -> throwable.message?.take(180)?.ifBlank { null }
                ?: "An unexpected error occurred. Please try again."
        }
    }
}
