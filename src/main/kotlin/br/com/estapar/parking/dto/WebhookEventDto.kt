package br.com.estapar.parking.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import java.time.ZonedDateTime

@Serdeable.Deserializable
@Introspected
sealed class WebhookEventDto {
    abstract val eventType: EventType
    abstract val licensePlate: String
}

@Serdeable.Deserializable
@Introspected
data class EventTypeHolder(
    @field:JsonProperty("event_type")
    val event_type: String
) {
    val eventType: EventType
        get() = EventType.valueOf(event_type)
}

@Serdeable.Deserializable
@Introspected
data class EntryEventDto(
    @field:JsonProperty("license_plate")
    override val licensePlate: String,

    @field:JsonProperty("entry_time")
    val entryTime: ZonedDateTime,

    @field:JsonProperty("event_type")
    override val eventType: EventType
) : WebhookEventDto()

@Serdeable.Deserializable
@Introspected
data class ParkedEventDto(
    @field:JsonProperty("license_plate")
    override val licensePlate: String,

    @field:JsonProperty("lat")
    val lat: Double,

    @field:JsonProperty("lng")
    val lng: Double,

    @field:JsonProperty("event_type")
    override val eventType: EventType
) : WebhookEventDto()

@Serdeable.Deserializable
@Introspected
data class ExitEventDto(
    @field:JsonProperty("license_plate")
    override val licensePlate: String,

    @field:JsonProperty("exit_time")
    val exitTime: ZonedDateTime,

    @field:JsonProperty("event_type")
    override val eventType: EventType
) : WebhookEventDto()

enum class EventType {
    ENTRY, PARKED, EXIT
}