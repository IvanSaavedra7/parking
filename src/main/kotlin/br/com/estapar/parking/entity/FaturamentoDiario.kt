package br.com.estapar.parking.entity

import io.micronaut.data.annotation.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZonedDateTime

@MappedEntity("faturamento_diario")
data class FaturamentoDiario(
    @field:Id
    @field:GeneratedValue
    val id: Long? = null,

    @field:MappedProperty("setor_id")
    val setorId: Long,

    val data: LocalDate,

    val valor: BigDecimal,

    @field:MappedProperty("quantidade_transacoes")
    val quantidadeTransacoes: Int,

    @field:MappedProperty("tempo_medio_permanencia_minutos")
    val tempoMedioPermanenciaMinutos: BigDecimal,

    @field:DateCreated
    @field:MappedProperty("criado_em")
    val criadoEm: ZonedDateTime? = null,

    @field:DateUpdated
    @field:MappedProperty("atualizado_em")
    val atualizadoEm: ZonedDateTime? = null
)