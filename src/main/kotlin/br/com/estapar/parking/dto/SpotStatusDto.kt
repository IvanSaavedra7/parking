package br.com.estapar.parking.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.serde.annotation.Serdeable
import java.math.BigDecimal
import java.time.ZonedDateTime

@Serdeable
data class SpotStatusRequestDto(
    val lat: Double,
    val lng: Double
)

@Serdeable
data class SpotStatusResponseDto(
    val ocupied: Boolean,

    @field:JsonProperty("license_plate")
    val licensePlate: String = "",

    @field:JsonProperty("price_until_now")
    val priceUntilNow: BigDecimal = BigDecimal.ZERO,

    @field:JsonProperty("entry_time")
    val entryTime: ZonedDateTime? = null,

    @field:JsonProperty("time_parked")
    val timeParked: ZonedDateTime? = null
)