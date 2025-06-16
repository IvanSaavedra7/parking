package br.com.estapar.parking.service

import br.com.estapar.commons.util.LogService
import br.com.estapar.parking.dto.SpotStatusResponseDto
import br.com.estapar.parking.repository.TransacaoEstacionamentoRepository
import br.com.estapar.parking.repository.VagaRepository
import br.com.estapar.parking.repository.VeiculoRepository
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.ZonedDateTime

@Singleton
class SpotStatusService(
    private val vagaRepository: VagaRepository,
    private val transacaoRepository: TransacaoEstacionamentoRepository,
    private val veiculoRepository: VeiculoRepository,
    private val logService: LogService
) {

    fun consultarStatusVaga(latitude: Double, longitude: Double): SpotStatusResponseDto {
        val latitudeBD = BigDecimal(latitude).setScale(8, RoundingMode.HALF_EVEN)
        val longitudeBD = BigDecimal(longitude).setScale(8, RoundingMode.HALF_EVEN)

        logService.debug("Consultando status da vaga nas coordenadas ($latitudeBD, $longitudeBD)")

        val vagaOpt = vagaRepository.findByLatitudeAndLongitude(latitudeBD, longitudeBD)

        if (vagaOpt.isEmpty) {
            throw IllegalArgumentException("Vaga com coordenadas ($latitude, $longitude) não encontrada")
        }

        val vaga = vagaOpt.get()

        if (vaga.status != "OCUPADA") {
            return SpotStatusResponseDto(ocupied = false)
        }

        // Se a vaga estiver ocupada, busca os detalhes da transação
        val transacaoOpt = transacaoRepository.findActiveByVagaId(vaga.id!!)

        if (transacaoOpt.isEmpty) {
            // Este é um estado inconsistente - vaga está marcada como ocupada mas não há transação ativa
            logService.warn("Vaga ${vaga.id} está marcada como ocupada mas não tem transação associada")
            return SpotStatusResponseDto(ocupied = true)
        }

        val transacao = transacaoOpt.get()
        val veiculo = veiculoRepository.findById(transacao.veiculoId)
            .orElseThrow { IllegalStateException("Veículo ${transacao.veiculoId} não encontrado") }

        // Calcula o preço atual
        val agora = ZonedDateTime.now()
        val duracaoAtualMinutos = Duration.between(transacao.horaEntrada, agora).toMinutes()
        val duracaoEmHoras = BigDecimal(duracaoAtualMinutos).divide(BigDecimal(60), 2, RoundingMode.HALF_EVEN)

        val precoAtual = transacao.precoBase
            .multiply(transacao.fatorPreco)
            .multiply(duracaoEmHoras)
            .setScale(2, RoundingMode.HALF_EVEN)

        return SpotStatusResponseDto(
            ocupied = true,
            licensePlate = veiculo.placa,
            priceUntilNow = precoAtual,
            entryTime = transacao.horaEntrada,
            timeParked = transacao.horaEstacionamento
        )
    }
}