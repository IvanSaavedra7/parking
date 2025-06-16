package br.com.estapar.commons.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import jakarta.inject.Singleton
import org.slf4j.MDC

@Singleton
open class LogService {
    companion object {
        private val logger = LoggerFactory.getLogger(LogService::class.java)
    }

    fun debug(message: String, vararg args: Any) {
        logger.debug(message, *args)
    }

    fun info(message: String, vararg args: Any) {
        logger.info(message, *args)
    }

    fun warn(message: String, vararg args: Any) {
        logger.warn(message, *args)
    }

    fun error(message: String, throwable: Throwable? = null, vararg args: Any) {
        if (throwable != null) {
            logger.error(message, throwable)
        } else {
            logger.error(message, *args)
        }
    }

    fun addToContext(key: String, value: String) {
        MDC.put(key, value)
    }

    fun removeFromContext(key: String) {
        MDC.remove(key)
    }

    fun clearContext() {
        MDC.clear()
    }
}