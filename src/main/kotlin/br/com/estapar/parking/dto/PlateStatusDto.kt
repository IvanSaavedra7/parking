package br.com.estapar.parking.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.serde.annotation.Serdeable
import java.math.BigDecimal
import java.time.ZonedDateTime

@Serdeable
data class PlateStatusRequestDto(
    @field:JsonProperty("license_plate")
    val licensePlate: String
)

@Serdeable
data class PlateStatusResponseDto(
    @field:JsonProperty("license_plate")
    val licensePlate: String,

    @field:JsonProperty("price_until_now")
    val priceUntilNow: BigDecimal,

    @field:JsonProperty("entry_time")
    val entryTime: ZonedDateTime,

    @field:JsonProperty("time_parked")
    val timeParked: ZonedDateTime?,

    @field:JsonProperty("lat")
    val lat: Double?,

    @field:JsonProperty("lng")
    val lng: Double?
)