// src/main/kotlin/br/com/estapar/parking/service/FaturamentoService.kt
package br.com.estapar.parking.service

import br.com.estapar.commons.util.LogService
import br.com.estapar.parking.entity.FaturamentoDiario
import br.com.estapar.parking.entity.TransacaoEstacionamento
import br.com.estapar.parking.repository.FaturamentoDiarioRepository
import br.com.estapar.parking.repository.TransacaoEstacionamentoRepository
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import jakarta.transaction.Transactional

@Singleton
open class FaturamentoService(
    private val faturamentoDiarioRepository: FaturamentoDiarioRepository,
    private val transacaoEstacionamentoRepository: TransacaoEstacionamentoRepository,
    private val logService: LogService
) {

    @Transactional
    open fun atualizarFaturamentoDiario(setorId: Long, transacao: TransacaoEstacionamento) {
        if (transacao.horaSaida == null || transacao.precoFinal == null) {
            logService.warn("Tentativa de atualizar faturamento com transação incompleta: ${transacao.id}")
            return
        }

        val dataFaturamento = transacao.horaSaida!!.toLocalDate()
        logService.debug("Atualizando faturamento diário para setor $setorId na data $dataFaturamento")

        val faturamentoExistente = faturamentoDiarioRepository.findBySetorIdAndData(setorId, dataFaturamento)

        if (faturamentoExistente.isPresent) {
            val faturamento = faturamentoExistente.get()

            val novoValor = faturamento.valor.add(transacao.precoFinal)
            val novaQuantidade = faturamento.quantidadeTransacoes + 1

            // Calcular novo tempo médio de permanência
            val novoTempoTotal = faturamento.tempoMedioPermanenciaMinutos
                .multiply(BigDecimal(faturamento.quantidadeTransacoes))
                .add(BigDecimal(transacao.duracaoMinutos ?: 0))

            val novoTempoMedio = if (novaQuantidade > 0) {
                novoTempoTotal.divide(BigDecimal(novaQuantidade), 2, RoundingMode.HALF_EVEN)
            } else {
                BigDecimal.ZERO
            }

            val faturamentoAtualizado = faturamento.copy(
                valor = novoValor,
                quantidadeTransacoes = novaQuantidade,
                tempoMedioPermanenciaMinutos = novoTempoMedio
            )

            faturamentoDiarioRepository.update(faturamentoAtualizado).also {
                logService.debug("Faturamento atualizado: ID ${it.id}, valor total: ${it.valor}")
            }
        } else {
            val novoFaturamento = FaturamentoDiario(
                setorId = setorId,
                data = dataFaturamento,
                valor = transacao.precoFinal,
                quantidadeTransacoes = 1,
                tempoMedioPermanenciaMinutos = BigDecimal(transacao.duracaoMinutos ?: 0)
            )

            faturamentoDiarioRepository.save(novoFaturamento).also {
                logService.debug("Novo registro de faturamento criado: ID ${it.id}, valor: ${it.valor}")
            }
        }
    }

    fun calcularFaturamentoDiario(setorId: Long, data: LocalDate): BigDecimal {
        logService.debug("Calculando faturamento para setor $setorId na data $data")

        val faturamentoOptional = faturamentoDiarioRepository.findBySetorIdAndData(setorId, data)

        if (faturamentoOptional.isPresent) {
            return faturamentoOptional.get().valor
        }

        // Se não existir registro no faturamento diário, calcula a partir das transações
        logService.debug("Nenhum registro de faturamento encontrado. Calculando a partir das transações.")
        val transacoes = transacaoEstacionamentoRepository.findAllCompletedBySetorIdAndDate(setorId, data)

        val total = transacoes.fold(BigDecimal.ZERO) { acc, transacao ->
            transacao.precoFinal?.let { acc.add(it) } ?: acc
        }

        logService.debug("Faturamento calculado: $total")
        return total
    }
}