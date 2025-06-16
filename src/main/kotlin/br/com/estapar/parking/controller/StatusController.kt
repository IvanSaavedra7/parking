package br.com.estapar.parking.controller

import br.com.estapar.commons.util.LogService
import br.com.estapar.parking.dto.*
import br.com.estapar.parking.service.FaturamentoService
import br.com.estapar.parking.service.SpotStatusService
import br.com.estapar.parking.service.VehicleStatusService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.http.annotation.Error
import io.micronaut.http.hateoas.JsonError
import io.micronaut.http.HttpStatus

@Controller
class StatusController(
    private val vehicleStatusService: VehicleStatusService,
    private val spotStatusService: SpotStatusService,
    private val faturamentoService: FaturamentoService,
    private val logService: LogService
) {

    @Post("/plate-status")
    fun getPlateStatus(@Body request: PlateStatusRequestDto): HttpResponse<PlateStatusResponseDto> {
        try {
            logService.info("Consultando status da placa: ${request.licensePlate}")
            val response = vehicleStatusService.consultarStatusPlaca(request.licensePlate)
            return HttpResponse.ok(response)
        } catch (e: IllegalArgumentException) {
            logService.warn("Placa não encontrada: ${request.licensePlate} - ${e.message}")
            throw e
        } catch (e: Exception) {
            logService.error("Erro ao consultar status da placa: ${request.licensePlate}", e)
            throw e
        }
    }

    @Post("/spot-status")
    fun getSpotStatus(@Body request: SpotStatusRequestDto): HttpResponse<SpotStatusResponseDto> {
        try {
            logService.info("Consultando status da vaga em coordenadas: (${request.lat}, ${request.lng})")
            val response = spotStatusService.consultarStatusVaga(request.lat, request.lng)
            return HttpResponse.ok(response)
        } catch (e: IllegalArgumentException) {
            logService.warn("Vaga não encontrada: (${request.lat}, ${request.lng}) - ${e.message}")
            throw e
        } catch (e: Exception) {
            logService.error("Erro ao consultar status da vaga: (${request.lat}, ${request.lng})", e)
            throw e
        }
    }

    @Get("/revenue")
    fun getRevenue(@QueryValue date: String, @QueryValue sector: String): HttpResponse<RevenueResponseDto> {
        try {
            logService.info("Consultando faturamento para setor $sector na data $date")
            val response = faturamentoService.consultarFaturamento(date, sector)
            return HttpResponse.ok(response)
        } catch (e: IllegalArgumentException) {
            logService.warn("Erro ao consultar faturamento: ${e.message}")
            throw e
        } catch (e: Exception) {
            logService.error("Erro ao consultar faturamento para setor $sector na data $date", e)
            throw e
        }
    }

    @Error(exception = IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): HttpResponse<JsonError> {
        return HttpResponse.badRequest<JsonError>()
            .body(JsonError(e.message ?: "Parâmetros inválidos"))
    }

    @Error(exception = Exception::class)
    fun handleGeneral(e: Exception): HttpResponse<JsonError> {
        return HttpResponse.serverError<JsonError>()
            .body(JsonError("Erro no servidor: ${e.message}"))
    }
}