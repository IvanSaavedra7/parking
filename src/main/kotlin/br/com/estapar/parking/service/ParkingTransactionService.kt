// src/main/kotlin/br/com/estapar/parking/service/ParkingTransactionService.kt
package br.com.estapar.parking.service

import br.com.estapar.commons.util.LogService
import br.com.estapar.parking.dto.EntryEventDto
import br.com.estapar.parking.dto.ExitEventDto
import br.com.estapar.parking.dto.ParkedEventDto
import br.com.estapar.parking.entity.TransacaoEstacionamento
import br.com.estapar.parking.entity.Vaga
import br.com.estapar.parking.repository.SetorRepository
import br.com.estapar.parking.repository.TransacaoEstacionamentoRepository
import br.com.estapar.parking.repository.VagaRepository
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.ZonedDateTime
import jakarta.transaction.Transactional

@Singleton
open class ParkingTransactionService(
    private val veiculoService: VeiculoService,
    private val vagaRepository: VagaRepository,
    private val setorRepository: SetorRepository,
    private val transacaoRepository: TransacaoEstacionamentoRepository,
    private val occupationMonitoringService: OccupationMonitoringService,
    private val dynamicPricingService: DynamicPricingService,
    private val eventoSistemaService: EventoSistemaService,
    private val faturamentoService: FaturamentoService,
    private val logService: LogService
) {

    @Transactional
    open fun processarEntradaVeiculo(evento: EntryEventDto): TransacaoEstacionamento {
        val placa = evento.licensePlate
        logService.info("Processando entrada do veículo com placa $placa")

        // Verificar se já existe uma transação ativa para este veículo
        val transacaoExistente = transacaoRepository.findActiveByPlaca(placa)
        if (transacaoExistente.isPresent) {
            logService.warn("Veículo com placa $placa já possui uma transação ativa (ID: ${transacaoExistente.get().id})")
            throw IllegalStateException("Veículo com placa $placa já se encontra no estacionamento")
        }

        // Obter o veículo (ou criar se não existir)
        val veiculo = veiculoService.obterOuCriarVeiculo(placa)

        // Determinar o setor, pega o primeiro setor disponível
        val setores = setorRepository.findAll()
        val setor = setores.firstOrNull() ?: throw IllegalStateException("Nenhum setor configurado no sistema")

        // Verificar ocupação do setor
        val setorDisponivel = occupationMonitoringService.verificarDisponibilidadeSetor(setor.id!!)
        if (!setorDisponivel) {
            val mensagem = "Entrada negada: setor ${setor.codigoSetor} está com ocupação máxima"
            logService.warn(mensagem)

            // Registrar evento de entrada negada
            eventoSistemaService.registrarEvento(
                tipoEvento = "ENTRADA_NEGADA",
                tipoEntidade = "VEICULO",
                entidadeId = veiculo.id!!,
                descricao = mensagem,
                metadados = mapOf("placa" to placa, "setor" to setor.codigoSetor)
            )

            throw IllegalStateException(mensagem)
        }

        // Obter o fator de preço atual com base na ocupação
        val ocupacaoAtual = occupationMonitoringService.getOcupacaoAtual(setor.id)
        val fatorPreco = dynamicPricingService.calcularFatorPrecoAtual(setor.id)

        // Criar nova transação
        val transacao = TransacaoEstacionamento(
            veiculoId = veiculo.id!!,
            setorId = setor.id,
            horaEntrada = evento.entryTime,
            precoBase = setor.precoBase,
            fatorPreco = fatorPreco,
            status = "ENTROU"
        )

        val transacaoSalva = transacaoRepository.save(transacao)
        logService.info("Transação de entrada criada: ID ${transacaoSalva.id}, veículo $placa, setor ${setor.codigoSetor}")

        // Atualizar ocupação do setor
        occupationMonitoringService.atualizarOcupacaoSetor(setor.id)

        // Registrar evento
        eventoSistemaService.registrarEvento(
            tipoEvento = "ENTRADA",
            tipoEntidade = "VEICULO",
            entidadeId = veiculo.id,
            descricao = "Veículo entrou no estacionamento",
            metadados = mapOf(
                "placa" to placa,
                "setor" to setor.codigoSetor,
                "fator_preco" to fatorPreco.toString(),
                "ocupacao" to "${ocupacaoAtual.percentualOcupacao.movePointRight(2)}%"
            )
        )

        return transacaoSalva
    }

    @Transactional
    open fun processarEstacionamentoVeiculo(evento: ParkedEventDto): TransacaoEstacionamento {
        val placa = evento.licensePlate
        logService.info("Processando estacionamento do veículo com placa $placa")

        // Buscar transação ativa do veículo
        val transacaoOpt = transacaoRepository.findActiveByPlaca(placa)
        if (transacaoOpt.isEmpty) {
            val mensagem = "Veículo com placa $placa não possui entrada registrada"
            logService.warn(mensagem)
            throw IllegalStateException(mensagem)
        }

        val transacao = transacaoOpt.get()
        if (transacao.status == "ESTACIONADO") {
            val mensagem = "Veículo com placa $placa já está estacionado"
            logService.warn(mensagem)
            throw IllegalStateException(mensagem)
        }

        // Identificar a vaga pelas coordenadas
        val latitude = BigDecimal(evento.lat)
        val longitude = BigDecimal(evento.lng)

        val vagaOpt = vagaRepository.findByLatitudeAndLongitude(latitude, longitude)
        if (vagaOpt.isEmpty) {
            val mensagem = "Vaga com coordenadas (${evento.lat}, ${evento.lng}) não encontrada"
            logService.warn(mensagem)
            throw IllegalStateException(mensagem)
        }

        val vaga = vagaOpt.get()

        // Verificar se a vaga está disponível
        if (vaga.status != "DISPONIVEL") {
            val mensagem = "Vaga ${vaga.id} não está disponível (status: ${vaga.status})"
            logService.warn(mensagem)
            throw IllegalStateException(mensagem)
        }

        // Atualizar status da vaga
        val vagaAtualizada = vaga.copy(status = "OCUPADA")
        vagaRepository.update(vagaAtualizada)

        // Atualizar transação
        val transacaoAtualizada = transacao.copy(
            vagaId = vaga.id,
            horaEstacionamento = ZonedDateTime.now(),
            status = "ESTACIONADO"
        )

        val transacaoSalva = transacaoRepository.update(transacaoAtualizada)
        logService.info("Veículo $placa estacionado na vaga ${vaga.id} - Transação atualizada: ${transacaoSalva.id}")

        // Registrar evento
        eventoSistemaService.registrarEvento(
            tipoEvento = "ESTACIONAMENTO",
            tipoEntidade = "VEICULO",
            entidadeId = transacao.veiculoId,
            descricao = "Veículo estacionado",
            metadados = mapOf(
                "vaga_id" to vaga.id.toString(),
                "lat" to evento.lat.toString(),
                "lng" to evento.lng.toString()
            )
        )

        return transacaoSalva
    }

    @Transactional
    open fun processarSaidaVeiculo(evento: ExitEventDto): TransacaoEstacionamento {
        val placa = evento.licensePlate
        logService.info("Processando saída do veículo com placa $placa")

        // Buscar transação ativa do veículo
        val transacaoOpt = transacaoRepository.findActiveByPlaca(placa)
        if (transacaoOpt.isEmpty) {
            val mensagem = "Veículo com placa $placa não possui entrada registrada"
            logService.warn(mensagem)
            throw IllegalStateException(mensagem)
        }

        val transacao = transacaoOpt.get()

        // Calcular duração e preço final
        val duracao = calcularDuracaoEstacionamento(transacao.horaEntrada, evento.exitTime)
        val precoFinal = calcularPrecoFinal(transacao.precoBase, transacao.fatorPreco, duracao)

        // Liberar a vaga, se estiver ocupando uma
        if (transacao.vagaId != null) {
            val vagaOpt = vagaRepository.findById(transacao.vagaId)
            if (vagaOpt.isPresent) {
                val vaga = vagaOpt.get()
                val vagaAtualizada = vaga.copy(status = "DISPONIVEL")
                vagaRepository.update(vagaAtualizada)
                logService.debug("Vaga ${vaga.id} liberada")
            }
        }

        // Atualizar transação
        val transacaoAtualizada = transacao.copy(
            horaSaida = evento.exitTime,
            duracaoMinutos = duracao,
            precoFinal = precoFinal,
            status = "SAIU"
        )

        val transacaoSalva = transacaoRepository.update(transacaoAtualizada)
        logService.info("Veículo $placa saiu do estacionamento - Transação finalizada: ${transacaoSalva.id}, valor: $precoFinal")

        // Atualizar ocupação do setor
        occupationMonitoringService.atualizarOcupacaoSetor(transacao.setorId)

        // Atualizar faturamento
        faturamentoService.atualizarFaturamentoDiario(transacao.setorId, transacaoSalva)

        // Registrar evento
        eventoSistemaService.registrarEvento(
            tipoEvento = "SAIDA",
            tipoEntidade = "VEICULO",
            entidadeId = transacao.veiculoId,
            descricao = "Veículo saiu do estacionamento",
            metadados = mapOf(
                "duracao_minutos" to duracao.toString(),
                "preco_final" to precoFinal.toString()
            )
        )

        return transacaoSalva
    }

    private fun calcularDuracaoEstacionamento(entrada: ZonedDateTime, saida: ZonedDateTime): Int {
        val duracao = Duration.between(entrada, saida)
        return duracao.toMinutes().toInt()
    }

    private fun calcularPrecoFinal(precoBase: BigDecimal, fatorPreco: BigDecimal, duracaoMinutos: Int): BigDecimal {
        // Preço final = preço base por hora * fator de preço * (duração em minutos / 60)
        val duracaoEmHoras = BigDecimal(duracaoMinutos).divide(BigDecimal(60), 2, RoundingMode.HALF_EVEN)

        return precoBase
            .multiply(fatorPreco)
            .multiply(duracaoEmHoras)
            .setScale(2, RoundingMode.HALF_EVEN)
    }
}