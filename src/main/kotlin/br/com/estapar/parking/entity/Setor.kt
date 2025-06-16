package br.com.estapar.parking.entity

import io.micronaut.data.annotation.*
import java.math.BigDecimal
import java.time.LocalTime
import java.time.ZonedDateTime

@MappedEntity("setores")
data class Setor(
    @field:Id
    @field:GeneratedValue
    val id: Long? = null,

    @field:MappedProperty("codigo_setor")
    val codigoSetor: String,

    @field:MappedProperty("preco_base")
    val precoBase: BigDecimal,

    @field:MappedProperty("capacidade_maxima")
    val capacidadeMaxima: Int,

    @field:MappedProperty("hora_abertura")
    val horaAbertura: LocalTime,

    @field:MappedProperty("hora_fechamento")
    val horaFechamento: LocalTime,

    @field:MappedProperty("limite_duracao_minutos")
    val limiteDuracaoMinutos: Int?,

    @field:DateCreated
    @field:MappedProperty("criado_em")
    val criadoEm: ZonedDateTime? = null,

    @field:DateUpdated
    @field:MappedProperty("atualizado_em")
    val atualizadoEm: ZonedDateTime? = null
)

@MappedEntity("vagas")
data class Vaga(
    @field:Id
    @field:GeneratedValue
    val id: Long? = null,

    @field:MappedProperty("id_externo")
    val idExterno: Long,

    @field:MappedProperty("setor_id")
    val setorId: Long,

    val latitude: BigDecimal,
    val longitude: BigDecimal,

    val status: String = "DISPONIVEL",

    @field:DateCreated
    @field:MappedProperty("criado_em")
    val criadoEm: ZonedDateTime? = null,

    @field:DateUpdated
    @field:MappedProperty("atualizado_em")
    val atualizadoEm: ZonedDateTime? = null
)

@MappedEntity("historico_ocupacao_setor")
data class HistoricoOcupacaoSetor(
    @field:Id
    @field:GeneratedValue
    val id: Long? = null,

    @field:MappedProperty("setor_id")
    val setorId: Long,

    val timestamp: ZonedDateTime = ZonedDateTime.now(),

    @field:MappedProperty("vagas_ocupadas")
    val vagasOcupadas: Int,

    @field:MappedProperty("total_vagas")
    val totalVagas: Int,

    @field:MappedProperty("percentual_ocupacao")
    val percentualOcupacao: BigDecimal,

    @field:MappedProperty("fator_preco_atual")
    val fatorPrecoAtual: BigDecimal
)