package br.com.estapar.parking.entity

import io.micronaut.data.annotation.*
import io.micronaut.data.model.DataType
import java.time.ZonedDateTime

@MappedEntity("eventos_sistema")
data class EventoSistema(
    @field:Id
    @field:GeneratedValue
    val id: Long? = null,

    @field:MappedProperty("tipo_evento")
    val tipoEvento: String,

    @field:MappedProperty("tipo_entidade")
    val tipoEntidade: String,

    @field:MappedProperty("entidade_id")
    val entidadeId: Long,

    val descricao: String? = null,

    @field:TypeDef(type = DataType.JSON)
    @field:MappedProperty("metadados")
    val metadados: Map<String, Any>? = null,

    @field:DateCreated
    @field:MappedProperty("criado_em")
    val criadoEm: ZonedDateTime? = null,

    @field:MappedProperty("usuario_id")
    val usuarioId: String? = null
)