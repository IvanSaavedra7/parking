package br.com.estapar.parking.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Serdeable.Deserializable
@Introspected
data class EventTypeHolder(
    @field:JsonProperty("event_type")
    val eventType: String
)

// Classe base para todos os eventos
@Serdeable.Deserializable
abstract class BaseWebhookEventDto {
    @field:JsonProperty("event_type")
    lateinit var eventType: String

    @field:JsonProperty("license_plate")
    lateinit var licensePlate: String
}

@Serdeable.Deserializable
data class EntryEventDto(
    // Campos específicos do evento de entrada
    @field:JsonProperty("entry_time")
    @field:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private var _entryTime: LocalDateTime? = null
) : BaseWebhookEventDto() {
    // Propriedade calculada que converte LocalDateTime para ZonedDateTime quando necessário
    val entryTime: ZonedDateTime?
        get() = _entryTime?.atZone(ZoneId.systemDefault())
}

@Serdeable.Deserializable
data class ParkedEventDto(
    // Campos específicos do evento de estacionamento
    @field:JsonProperty("lat")
    var lat: Double? = null,

    @field:JsonProperty("lng")
    var lng: Double? = null
) : BaseWebhookEventDto()

@Serdeable.Deserializable
data class ExitEventDto(
    // Campos específicos do evento de saída
    @field:JsonProperty("exit_time")
    @field:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private var _exitTime: LocalDateTime? = null
) : BaseWebhookEventDto() {
    // Propriedade calculada que converte LocalDateTime para ZonedDateTime quando necessário
    val exitTime: ZonedDateTime?
        get() = _exitTime?.atZone(ZoneId.systemDefault())
}

enum class EventType {
    ENTRY, PARKED, EXIT
}