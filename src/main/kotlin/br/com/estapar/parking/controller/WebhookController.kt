package br.com.estapar.parking.controller

import br.com.estapar.commons.util.LogService
import br.com.estapar.parking.dto.*
import br.com.estapar.parking.service.ParkingTransactionService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.serde.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Produces

@Controller
class WebhookController(
    private val parkingTransactionService: ParkingTransactionService,
    private val logService: LogService,
    private val objectMapper: ObjectMapper
) {

    @Post("/webhook")
    @Produces(MediaType.APPLICATION_JSON)
    fun processEvent(@Body payload: String): HttpResponse<*> {
        try {
            // Log do payload recebido
            logService.debug("Webhook recebido: $payload")

            // Primeiro, converte para JsonNode para inspeção flexível
            val jsonNode = objectMapper.readValue(payload, JsonNode::class.java)

            // Verifica se existe o campo event_type
            if (!jsonNode.has("event_type")) {
                logService.error("Payload não contém campo 'event_type'")
                return HttpResponse.badRequest(mapOf("status" to "error", "message" to "Payload inválido, faltando 'event_type'"))
            }

            val eventTypeStr = jsonNode.get("event_type").asText()
            logService.info("Tipo de evento detectado: $eventTypeStr")

            // Processar de acordo com o tipo de evento
            return when (eventTypeStr) {
                "ENTRY" -> {
                    // Simplesmente deserializa o payload JSON original para o tipo específico
                    val event = objectMapper.readValue(payload, EntryEventDto::class.java)
                    logService.info("Processando evento ENTRY para placa ${event.licensePlate}")

                    val transaction = parkingTransactionService.processarEntradaVeiculo(event)
                    HttpResponse.ok(mapOf("status" to "success", "message" to "Entrada registrada com sucesso", "transaction_id" to transaction.id))
                }

                "PARKED" -> {
                    val event = objectMapper.readValue(payload, ParkedEventDto::class.java)
                    logService.info("Processando evento PARKED para placa ${event.licensePlate}")

                    val transaction = parkingTransactionService.processarEstacionamentoVeiculo(event)
                    HttpResponse.ok(mapOf("status" to "success", "message" to "Estacionamento registrado com sucesso", "transaction_id" to transaction.id))
                }

                "EXIT" -> {
                    val event = objectMapper.readValue(payload, ExitEventDto::class.java)
                    logService.info("Processando evento EXIT para placa ${event.licensePlate}")

                    val transaction = parkingTransactionService.processarSaidaVeiculo(event)
                    HttpResponse.ok(mapOf(
                        "status" to "success",
                        "message" to "Saída registrada com sucesso",
                        "transaction_id" to transaction.id,
                        "price" to transaction.precoFinal
                    ))
                }

                else -> {
                    logService.error("Tipo de evento desconhecido: $eventTypeStr")
                    HttpResponse.badRequest(mapOf("status" to "error", "message" to "Tipo de evento desconhecido"))
                }
            }
        } catch (e: Exception) {
            logService.error("Erro ao processar webhook", e)
            logService.error("Payload problemático: $payload")
            return HttpResponse.badRequest(mapOf("status" to "error", "message" to e.message))
        }
    }
}