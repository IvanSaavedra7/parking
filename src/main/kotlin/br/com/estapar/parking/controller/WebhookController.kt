package br.com.estapar.parking.controller

import br.com.estapar.commons.util.LogService
import br.com.estapar.parking.dto.*
import br.com.estapar.parking.service.ParkingTransactionService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.serde.ObjectMapper

@Controller
class WebhookController(
    private val parkingTransactionService: ParkingTransactionService,
    private val logService: LogService,
    private val objectMapper: ObjectMapper
) {

    @Post("/webhook")
    fun processEvent(@Body payload: String): HttpResponse<*> {
        try {
            // Log do payload recebido
            logService.debug("Webhook recebido: $payload")

            // Deserializar para obter o tipo de evento
            val eventType = objectMapper.readValue(payload, EventTypeHolder::class.java).eventType

            // Processar de acordo com o tipo de evento
            return when (eventType) {
                EventType.ENTRY -> {
                    val event = objectMapper.readValue(payload, EntryEventDto::class.java)
                    logService.info("Processando evento ENTRY para placa ${event.licensePlate}")

                    val transaction = parkingTransactionService.processarEntradaVeiculo(event)
                    HttpResponse.ok(mapOf("status" to "success", "message" to "Entrada registrada com sucesso", "transaction_id" to transaction.id))
                }

                EventType.PARKED -> {
                    val event = objectMapper.readValue(payload, ParkedEventDto::class.java)
                    logService.info("Processando evento PARKED para placa ${event.licensePlate}")

                    val transaction = parkingTransactionService.processarEstacionamentoVeiculo(event)
                    HttpResponse.ok(mapOf("status" to "success", "message" to "Estacionamento registrado com sucesso", "transaction_id" to transaction.id))
                }

                EventType.EXIT -> {
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
            }
        } catch (e: Exception) {
            logService.error("Erro ao processar webhook", e)
            return HttpResponse.badRequest(mapOf("status" to "error", "message" to e.message))
        }
    }

    // Classe auxiliar para extrair o tipo de evento do payload JSON
    @io.micronaut.core.annotation.Introspected
    data class EventTypeHolder(
        val event_type: String
    ) {
        val eventType: EventType
            get() = EventType.valueOf(event_type)
    }
}