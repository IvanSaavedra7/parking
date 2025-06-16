package br.com.estapar.parking.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.serde.annotation.Serdeable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZonedDateTime

@Serdeable
data class RevenueRequestDto(
    val date: String,
    val sector: String
)

@Serdeable
data class RevenueResponseDto(
    val amount: BigDecimal,
    val currency: String = "BRL",
    val timestamp: ZonedDateTime = ZonedDateTime.now()
)