package br.com.estapar.parking.entity

import io.micronaut.data.annotation.*
import java.math.BigDecimal
import java.time.ZonedDateTime

@MappedEntity("transacoes_estacionamento")
data class TransacaoEstacionamento(
    @field:Id
    @field:GeneratedValue
    val id: Long? = null,

    @field:MappedProperty("veiculo_id")
    val veiculoId: Long,

    @field:MappedProperty("setor_id")
    val setorId: Long,

    @field:MappedProperty("vaga_id")
    val vagaId: Long? = null,

    @field:MappedProperty("hora_entrada")
    val horaEntrada: ZonedDateTime,

    @field:MappedProperty("hora_estacionamento")
    val horaEstacionamento: ZonedDateTime? = null,

    @field:MappedProperty("hora_saida")
    val horaSaida: ZonedDateTime? = null,

    @field:MappedProperty("duracao_minutos")
    val duracaoMinutos: Int? = null,

    @field:MappedProperty("preco_base")
    val precoBase: BigDecimal,

    @field:MappedProperty("fator_preco")
    val fatorPreco: BigDecimal,

    @field:MappedProperty("preco_final")
    val precoFinal: BigDecimal? = null,

    val status: String,

    @field:DateCreated
    @field:MappedProperty("criado_em")
    val criadoEm: ZonedDateTime? = null,

    @field:DateUpdated
    @field:MappedProperty("atualizado_em")
    val atualizadoEm: ZonedDateTime? = null
)