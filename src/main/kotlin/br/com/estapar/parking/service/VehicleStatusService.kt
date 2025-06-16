package br.com.estapar.parking.service

import br.com.estapar.commons.util.LogService
import br.com.estapar.parking.dto.PlateStatusResponseDto
import br.com.estapar.parking.entity.TransacaoEstacionamento
import br.com.estapar.parking.entity.Vaga
import br.com.estapar.parking.repository.TransacaoEstacionamentoRepository
import br.com.estapar.parking.repository.VagaRepository
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.ZonedDateTime

@Singleton
class VehicleStatusService(
    private val transacaoRepository: TransacaoEstacionamentoRepository,
    private val vagaRepository: VagaRepository,
    private val logService: LogService
) {

    fun consultarStatusPlaca(placa: String): PlateStatusResponseDto {
        logService.debug("Consultando status da placa $placa")

        val transacao = transacaoRepository.findActiveByPlaca(placa)
            .orElseThrow { IllegalArgumentException("Veículo com placa $placa não está no estacionamento") }

        var latitude: Double? = null
        var longitude: Double? = null

        // Se tiver vaga associada, busca as coordenadas
        if (transacao.vagaId != null) {
            val vaga = vagaRepository.findById(transacao.vagaId)
                .orElseThrow { IllegalStateException("Vaga ${transacao.vagaId} não encontrada") }

            latitude = vaga.latitude.toDouble()
            longitude = vaga.longitude.toDouble()
        }

        // Calcula o preço até agora
        val precoAteAgora = calcularPrecoAteAgora(transacao)

        return PlateStatusResponseDto(
            licensePlate = placa,
            priceUntilNow = precoAteAgora,
            entryTime = transacao.horaEntrada,
            timeParked = transacao.horaEstacionamento,
            lat = latitude,
            lng = longitude
        )
    }

    private fun calcularPrecoAteAgora(transacao: TransacaoEstacionamento): BigDecimal {
        val agora = ZonedDateTime.now()

        val duracaoAtualMinutos = Duration.between(transacao.horaEntrada, agora).toMinutes()
        val duracaoEmHoras = BigDecimal(duracaoAtualMinutos).divide(BigDecimal(60), 2, RoundingMode.HALF_EVEN)

        return transacao.precoBase
            .multiply(transacao.fatorPreco)
            .multiply(duracaoEmHoras)
            .setScale(2, RoundingMode.HALF_EVEN)
    }
}