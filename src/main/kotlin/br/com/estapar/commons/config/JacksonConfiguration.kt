package br.com.estapar.commons.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Replaces
import jakarta.inject.Singleton

@Factory
class JacksonConfiguration {

    @Singleton
    @Replaces(ObjectMapper::class)
    fun objectMapper(): ObjectMapper {
        return ObjectMapper()
            .registerModule(JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, true)
    }
}