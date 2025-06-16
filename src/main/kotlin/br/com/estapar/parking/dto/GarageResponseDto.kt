package br.com.estapar.parking.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class GarageResponseDto(
    val garage: List<SectorDto>,
    val spots: List<SpotDto>
)

@Serdeable
data class SectorDto(
    val sector: String,
    val basePrice: Double,
    @JsonProperty("max_capacity")
    val maxCapacity: Int,
    @JsonProperty("open_hour")
    val openHour: String,
    @JsonProperty("close_hour")
    val closeHour: String,
    @JsonProperty("duration_limit_minutes")
    val durationLimitMinutes: Int?
)

@Serdeable
data class SpotDto(
    val id: Long,
    val sector: String,
    val lat: Double,
    val lng: Double
)