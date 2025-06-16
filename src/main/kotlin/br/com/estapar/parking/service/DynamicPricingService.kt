package br.com.estapar.parking.service

import br.com.estapar.commons.util.LogService
import br.com.estapar.parking.repository.HistoricoOcupacaoSetorRepository
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.math.RoundingMode
import jakarta.transaction.Transactional

@Singleton
open class DynamicPricingService(
    private val historicoOcupacaoSetorRepository: HistoricoOcupacaoSetorRepository,
    private val logService: LogService
) {

    @Transactional
    open fun calcularFatorPrecoAtual(setorId: Long): BigDecimal {
        val historicoOcupacao = historicoOcupacaoSetorRepository.buscarUltimaOcupacaoPorSetor(setorId)

        if (historicoOcupacao.isEmpty) {
            logService.warn("Histórico de ocupação não encontrado para o setor $setorId. Usando fator de preço padrão (1.0)")
            return BigDecimal.ONE
        }

        val percentualOcupacao = historicoOcupacao.get().percentualOcupacao
        logService.debug("Percentual de ocupação atual do setor $setorId: $percentualOcupacao")

        return calcularFatorPrecoComBaseEmOcupacao(percentualOcupacao).also {
            logService.debug("Fator de preço calculado: $it")
        }
    }

    private fun calcularFatorPrecoComBaseEmOcupacao(percentualOcupacao: BigDecimal): BigDecimal {
        return when {
            percentualOcupacao < BigDecimal("0.25") -> BigDecimal("0.9")
            percentualOcupacao < BigDecimal("0.5") -> BigDecimal.ONE
            percentualOcupacao < BigDecimal("0.75") -> BigDecimal("1.1")
            else -> BigDecimal("1.25")
        }.setScale(2, RoundingMode.HALF_EVEN)
    }
}