package br.com.estapar.parking.entity

import io.micronaut.data.annotation.*
import java.time.ZonedDateTime

@MappedEntity("veiculos")
data class Veiculo(
    @field:Id
    @field:GeneratedValue
    val id: Long? = null,

    val placa: String,

    @field:DateCreated
    @field:MappedProperty("criado_em")
    val criadoEm: ZonedDateTime? = null,

    @field:DateUpdated
    @field:MappedProperty("atualizado_em")
    val atualizadoEm: ZonedDateTime? = null
)