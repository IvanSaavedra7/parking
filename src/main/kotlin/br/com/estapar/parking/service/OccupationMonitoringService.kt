package br.com.estapar.parking.service

import br.com.estapar.commons.util.LogService
import br.com.estapar.parking.entity.HistoricoOcupacaoSetor
import br.com.estapar.parking.repository.HistoricoOcupacaoSetorRepository
import br.com.estapar.parking.repository.TransacaoEstacionamentoRepository
import br.com.estapar.parking.repository.VagaRepository
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.math.RoundingMode
import jakarta.transaction.Transactional

@Singleton
open class OccupationMonitoringService(
    private val vagaRepository: VagaRepository,
    private val transacaoEstacionamentoRepository: TransacaoEstacionamentoRepository,
    private val historicoOcupacaoSetorRepository: HistoricoOcupacaoSetorRepository,
    private val dynamicPricingService: DynamicPricingService,
    private val logService: LogService
) {

    @Transactional
    open fun atualizarOcupacaoSetor(setorId: Long): HistoricoOcupacaoSetor {
        logService.debug("Atualizando ocupação do setor $setorId")

        val vagasOcupadas = transacaoEstacionamentoRepository.countActiveBySetorId(setorId)
        val totalVagas = vagaRepository.countBySetorId(setorId)

        if (totalVagas == 0L) {
            throw IllegalStateException("Setor $setorId não possui vagas configuradas")
        }

        val percentualOcupacao = BigDecimal(vagasOcupadas)
            .divide(BigDecimal(totalVagas), 4, RoundingMode.HALF_EVEN)

        val fatorPreco = dynamicPricingService.calcularFatorPrecoAtual(setorId)

        logService.debug("Ocupação atual: $vagasOcupadas/$totalVagas vagas (${percentualOcupacao.movePointRight(2)}%)")

        val historico = HistoricoOcupacaoSetor(
            setorId = setorId,
            vagasOcupadas = vagasOcupadas.toInt(),
            totalVagas = totalVagas.toInt(),
            percentualOcupacao = percentualOcupacao,
            fatorPrecoAtual = fatorPreco
        )

        return historicoOcupacaoSetorRepository.save(historico)
    }

    @Transactional
    open fun verificarDisponibilidadeSetor(setorId: Long): Boolean {
        val historico = historicoOcupacaoSetorRepository.buscarUltimaOcupacaoPorSetor(setorId)

        if (historico.isEmpty) {
            logService.warn("Histórico de ocupação não encontrado para o setor $setorId")
            return true
        }

        val dadosOcupacao = historico.get()
        val percentualOcupacao = dadosOcupacao.percentualOcupacao

        logService.debug("Verificando disponibilidade do setor $setorId - Ocupação atual: ${percentualOcupacao.movePointRight(2)}%")

        // Se o percentual de ocupação for 100% ou mais, o setor está cheio
        val setorCheio = percentualOcupacao >= BigDecimal.ONE

        if (setorCheio) {
            logService.info("Setor $setorId está CHEIO - negando entrada")
        }

        return !setorCheio
    }

    fun getOcupacaoAtual(setorId: Long): HistoricoOcupacaoSetor {
        val historico = historicoOcupacaoSetorRepository.buscarUltimaOcupacaoPorSetor(setorId)

        if (historico.isEmpty) {
            throw IllegalStateException("Histórico de ocupação não encontrado para o setor $setorId")
        }

        return historico.get()
    }
}