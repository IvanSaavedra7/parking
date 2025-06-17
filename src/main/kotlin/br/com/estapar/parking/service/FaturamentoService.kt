package br.com.estapar.parking.service

import br.com.estapar.commons.util.LogService
import br.com.estapar.parking.dto.RevenueResponseDto
import br.com.estapar.parking.entity.FaturamentoDiario
import br.com.estapar.parking.entity.TransacaoEstacionamento
import br.com.estapar.parking.repository.FaturamentoDiarioRepository
import br.com.estapar.parking.repository.SetorRepository
import br.com.estapar.parking.repository.TransacaoEstacionamentoRepository
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import jakarta.transaction.Transactional

@Singleton
open class FaturamentoService(
    private val faturamentoDiarioRepository: FaturamentoDiarioRepository,
    private val transacaoEstacionamentoRepository: TransacaoEstacionamentoRepository,
    private val setorRepository: SetorRepository,
    private val logService: LogService
) {

    @Transactional
    open fun atualizarFaturamentoDiario(setorId: Long, transacao: TransacaoEstacionamento) {
        logService.info("Atualizando faturamento diario para setor id=${setorId}...")

        if (transacao.horaSaida == null || transacao.precoFinal == null) {
            logService.warn("Tentativa de atualizar faturamento com transação incompleta: ${transacao.id}")
            return
        }

        val dataFaturamento = transacao.horaSaida!!.toLocalDate()
        logService.info("Atualizando faturamento diario para setor $setorId na data $dataFaturamento")

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
                logService.info("Faturamento atualizado: ID ${it.id}, valor total: ${it.valor}")
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
                logService.info("Novo registro de faturamento criado: ID ${it.id}, valor: ${it.valor}")
            }
        }
    }

    fun consultarFaturamento(data: String, codigoSetor: String): RevenueResponseDto {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dataLocalDate = LocalDate.parse(data, formatter)

        logService.info("Consultando faturamento para setor $codigoSetor na data $dataLocalDate")

        val setor = setorRepository.findByCodigoSetor(codigoSetor)
            .orElseThrow { IllegalArgumentException("Setor $codigoSetor não encontrado") }

        val faturamento = calcularFaturamentoDiario(setor.id!!, dataLocalDate)

        return RevenueResponseDto(
            amount = faturamento,
            currency = "BRL",
            timestamp = ZonedDateTime.now()
        )
    }

    fun calcularFaturamentoDiario(setorId: Long, data: LocalDate): BigDecimal {
        logService.info("Calculando faturamento para setor $setorId na data $data")

        val faturamentoOptional = faturamentoDiarioRepository.findBySetorIdAndData(setorId, data)

        if (faturamentoOptional.isPresent) {
            return faturamentoOptional.get().valor
        }

        // Se não existir registro no faturamento diário, calcula a partir das transações
        logService.info("Nenhum registro de faturamento encontrado. Calculando a partir das transações.")
        val transacoes = transacaoEstacionamentoRepository.findAllCompletedBySetorIdAndDate(setorId, data)

        val total = transacoes.fold(BigDecimal.ZERO) { acc, transacao ->
            transacao.precoFinal?.let { acc.add(it) } ?: acc
        }

        logService.info("Faturamento calculado: $total")
        return total
    }
}