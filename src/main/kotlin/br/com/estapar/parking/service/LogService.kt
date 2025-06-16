package com.estapar.parking.service

import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

@Singleton
open class LogService {
    private val logger = LoggerFactory.getLogger(LogService::class.java)

    fun registrarOperacao(tipoEvento: String, tipoEntidade: String, idEntidade: Long, descricao: String,
                          detalhes: Map<String, Any?> = emptyMap()) {
        logger.info("EVENTO: $tipoEvento | ENTIDADE: $tipoEntidade | ID: $idEntidade | DESCRIÇÃO: $descricao | DETALHES: $detalhes")

        // Implementar registro no banco de dados através da tabela system_events
        try {
            // Código para salvar no banco
        } catch (e: Exception) {
            logger.error("Erro ao registrar evento no banco de dados: ${e.message}")
        }
    }

    fun registrarErro(origem: String, erro: Throwable, detalhes: Map<String, Any?> = emptyMap()) {
        logger.error("ERRO: $origem | MENSAGEM: ${erro.message} | DETALHES: $detalhes", erro)

        // Implementar registro de erro no banco
    }

    fun registrarAlerta(origem: String, mensagem: String, detalhes: Map<String, Any?> = emptyMap()) {
        logger.warn("ALERTA: $origem | MENSAGEM: $mensagem | DETALHES: $detalhes")

        // Implementar registro de alerta no banco
    }
}