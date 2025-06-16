package br.com.estapar.parking.service

import br.com.estapar.commons.util.LogService
import br.com.estapar.parking.entity.EventoSistema
import br.com.estapar.parking.repository.EventoSistemaRepository
import jakarta.inject.Singleton
import jakarta.transaction.Transactional

@Singleton
open class EventoSistemaService(
    private val eventoSistemaRepository: EventoSistemaRepository,
    private val logService: LogService
) {

    @Transactional
    open fun registrarEvento(
        tipoEvento: String,
        tipoEntidade: String,
        entidadeId: Long,
        descricao: String?,
        metadados: Map<String, Any>? = null
    ): EventoSistema {
        logService.debug("Registrando evento: $tipoEvento para $tipoEntidade:$entidadeId")

        val evento = EventoSistema(
            tipoEvento = tipoEvento,
            tipoEntidade = tipoEntidade,
            entidadeId = entidadeId,
            descricao = descricao,
            metadados = metadados
        )

        return eventoSistemaRepository.save(evento).also {
            logService.debug("Evento registrado com ID: ${it.id}")
        }
    }
}