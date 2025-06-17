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
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    @Transactional
    open fun processarEntradaVeiculo(evento: EntryEventDto): TransacaoEstacionamento {
        val placa = evento.licensePlate
        val startTime = System.currentTimeMillis()
        logService.info("ENTRADA: Iniciando processamento de entrada do veiculo placa={}", placa)

        val horaEntrada = evento.entryTime ?: ZonedDateTime.now()
        logService.info("ENTRADA: Horario de entrada registrado={}", horaEntrada.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))

        // Verificar se ja existe uma transacao ativa para este veiculo
        logService.info("ENTRADA: Verificando se veiculo placa={} ja esta no estacionamento...", placa)
        val transacaoExistente = transacaoRepository.findActiveByPlaca(placa)
        if (transacaoExistente.isPresent) {
            val transacao = transacaoExistente.get()
            logService.warn("ENTRADA: Veiculo placa={} ja possui transacao ativa id={}, status={}, entrada={}",
                placa, transacao.id!!, transacao.status, transacao.horaEntrada)
            throw IllegalStateException("Veiculo com placa $placa ja se encontra no estacionamento")
        }
        logService.info("ENTRADA: Veiculo placa={} nao possui transacao ativa entao pode entrar", placa)

        // Obter o veiculo (ou criar se nao existir)
        val veiculo = veiculoService.obterOuCriarVeiculo(placa)

        // Obter todos os setores disponiveis
        logService.info("ENTRADA: Buscando setores disponiveis no sistema...")
        val setores = setorRepository.findAll().toList()
        if (setores.isEmpty()) {
            logService.error("ENTRADA: Nenhum setor configurado no sistema!")
            throw IllegalStateException("Nenhum setor configurado no sistema")
        }

        logService.info("ENTRADA: Total de setores encontrados: {}", setores.size)
        setores.forEach { setor ->
            logService.info("ENTRADA: Setor encontrado: codigo={}, preco_base={}, capacidade={}, horario={}~{}",
                setor.codigoSetor, setor.precoBase, setor.capacidadeMaxima,
                setor.horaAbertura, setor.horaFechamento)
        }

        // Verificar setores com disponibilidade
        val horaAtual = ZonedDateTime.now()
        logService.info("ENTRADA: Hora atual: {}", horaAtual.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))

        logService.info("ENTRADA: Verificando setores abertos e com disponibilidade...")
        val setoresDisponiveis = mutableListOf<br.com.estapar.parking.entity.Setor>()

        setores.forEach { setor ->
            // Verificar se o estacionamento esta aberto neste horario
            val horaAbertura = LocalTime.parse(setor.horaAbertura.toString())
            val horaFechamento = LocalTime.parse(setor.horaFechamento.toString())
            val horaAtualLocal = horaAtual.toLocalTime()

            val estaAberto = if (horaAbertura < horaFechamento) {
                horaAtualLocal >= horaAbertura && horaAtualLocal <= horaFechamento
            } else { // Caso cruze a meia-noite
                horaAtualLocal >= horaAbertura || horaAtualLocal <= horaFechamento
            }

            val temDisponibilidade = occupationMonitoringService.verificarDisponibilidadeSetor(setor.id!!)

            logService.info("ENTRADA: Analise do setor={}: aberto={}, disponibilidade={}",
                setor.codigoSetor, estaAberto, temDisponibilidade)

            if (estaAberto && temDisponibilidade) {
                setoresDisponiveis.add(setor)
            }
        }

        // Ordenar setores disponiveis
        val setoresOrdenados = setoresDisponiveis.sortedBy { it.codigoSetor }
        logService.info("ENTRADA: Setores disponiveis apos analise: {}", setoresOrdenados.size)

        if (setoresOrdenados.isEmpty()) {
            val mensagem = "Entrada negada: Todos os setores estao com ocupacao maxima ou fechados neste horario"
            logService.warn("ENTRADA: {}", mensagem)

            // Registrar evento de entrada negada
            logService.info("ENTRADA: Registrando evento de entrada negada para veiculo id={}", veiculo.id!!)
            eventoSistemaService.registrarEvento(
                tipoEvento = "ENTRADA_NEGADA",
                tipoEntidade = "VEICULO",
                entidadeId = veiculo.id!!,
                descricao = mensagem,
                metadados = mapOf(
                    "placa" to placa,
                    "hora_tentativa" to horaAtual.toString()
                )
            )

            throw IllegalStateException(mensagem)
        }

        // Pegar o primeiro setor disponivel da lista ordenada
        val setor = setoresOrdenados.firstOrNull()
            ?: throw IllegalStateException("Nenhum setor encontrado disponivel")

        logService.info("ENTRADA: Selecionado setor={} para entrada do veiculo placa={}",
            setor.codigoSetor, placa)

        val ocupacaoAtual = occupationMonitoringService.getOcupacaoAtual(setor.id!!)

        logService.info("ENTRADA: Ocupacao atual do setor={}: {}% ({}/{} vagas)",
            setor.codigoSetor,
            ocupacaoAtual.percentualOcupacao.movePointRight(2),
            ocupacaoAtual.vagasOcupadas,
            ocupacaoAtual.totalVagas)

        logService.info("ENTRADA: Calculando fator de preco para setor id={} com ocupacao atual {}%",
            setor.id, ocupacaoAtual.percentualOcupacao.movePointRight(2))
        val fatorPreco = dynamicPricingService.calcularFatorPrecoAtual(setor.id)
        logService.info("ENTRADA: Fator de preco calculado: {}", fatorPreco)

        // Criar nova transacao
        logService.info("ENTRADA: Criando transacao de estacionamento para veiculo placa={}, setor={}",
            placa, setor.codigoSetor)
        val transacao = TransacaoEstacionamento(
            veiculoId = veiculo.id!!,
            setorId = setor.id,
            horaEntrada = horaEntrada,
            precoBase = setor.precoBase,
            fatorPreco = fatorPreco,
            status = "ENTROU"
        )

        logService.info("ENTRADA: Salvando transacao no banco de dados...")
        val transacaoSalva = transacaoRepository.save(transacao)
        logService.info("ENTRADA: Transacao salva com sucesso: id={}, veiculo={}, setor={}",
            transacaoSalva.id!!, placa, setor.codigoSetor)

        // Atualizar ocupacao do setor
        logService.info("ENTRADA: Atualizando contadores de ocupacao para setor id={}...", setor.id)
        occupationMonitoringService.atualizarOcupacaoSetor(setor.id)
        logService.info("ENTRADA: Contadores de ocupacao atualizados para setor={}", setor.codigoSetor)

        // Registrar evento
        logService.info("ENTRADA: Registrando evento de sistema para entrada do veiculo id={}", veiculo.id)
        eventoSistemaService.registrarEvento(
            tipoEvento = "ENTRADA",
            tipoEntidade = "VEICULO",
            entidadeId = veiculo.id,
            descricao = "Veiculo entrou no estacionamento",
            metadados = mapOf(
                "placa" to placa,
                "setor" to setor.codigoSetor,
                "fator_preco" to fatorPreco.toString(),
                "ocupacao" to "${ocupacaoAtual.percentualOcupacao.movePointRight(2)}%",
                "preco_base" to setor.precoBase.toString(),
                "preco_estimado_hora" to (setor.precoBase * fatorPreco).toString()
            )
        )

        val endTime = System.currentTimeMillis()
        logService.info("ENTRADA: Processamento concluido em {}ms para veiculo placa={}",
            (endTime - startTime), placa)

        return transacaoSalva
    }

    @Transactional
    open fun processarEstacionamentoVeiculo(evento: ParkedEventDto): TransacaoEstacionamento {
        val placa = evento.licensePlate
        val startTime = System.currentTimeMillis()
        logService.info("ESTACIONAMENTO: Iniciando processamento para veiculo placa={}", placa)

        // Verificar se os campos obrigatorios estao presentes
        if (evento.lat == null || evento.lng == null) {
            val mensagem = "Coordenadas de latitude e longitude sao obrigatorias para estacionamento"
            logService.error("ESTACIONAMENTO: ${mensagem}")
            throw IllegalArgumentException(mensagem)
        }

        // Buscar transacao ativa do veiculo
        logService.info("ESTACIONAMENTO: Buscando transacao ativa para veiculo placa={}", placa)
        val transacaoOpt = transacaoRepository.findActiveByPlaca(placa)
        if (transacaoOpt.isEmpty) {
            val mensagem = "Veiculo com placa $placa nao possui entrada registrada"
            logService.warn("ESTACIONAMENTO: {}", mensagem)
            throw IllegalStateException(mensagem)
        }

        val transacao = transacaoOpt.get()
        logService.info("ESTACIONAMENTO: Transacao ativa encontrada id={}, status={}, setor id={}",
            transacao.id!!, transacao.status, transacao.setorId)

        if (transacao.status == "ESTACIONADO") {
            val mensagem = "Veiculo com placa $placa ja esta estacionado"
            logService.warn("ESTACIONAMENTO: {}", mensagem)
            throw IllegalStateException(mensagem)
        }

        // Identificar a vaga pelas coordenadas
        logService.info("ESTACIONAMENTO: Convertendo coordenadas: lat={}, lng={}", evento.lat, evento.lng)
        val latitude = BigDecimal(evento.lat).setScale(8, RoundingMode.HALF_EVEN)
        val longitude = BigDecimal(evento.lng).setScale(8, RoundingMode.HALF_EVEN)
        logService.info("ESTACIONAMENTO: Buscando vaga com coordenadas lat={}, lng={}", latitude, longitude)

        val vagaOpt = vagaRepository.findByLatitudeAndLongitude(latitude, longitude)
        if (vagaOpt.isEmpty) {
            val mensagem = "Vaga com coordenadas (${evento.lat}, ${evento.lng}) nao encontrada"
            logService.warn("ESTACIONAMENTO: {}", mensagem)
            throw IllegalStateException(mensagem)
        }

        val vaga = vagaOpt.get()
        logService.info("ESTACIONAMENTO: Vaga encontrada id={}, setor id={}, status={}",
            vaga.id!!, vaga.setorId, vaga.status)

        // Verificar se a vaga esta disponivel
        if (vaga.status != "DISPONIVEL") {
            val mensagem = "Vaga ${vaga.id} nao esta disponivel (status: ${vaga.status})"
            logService.warn("ESTACIONAMENTO: {}", mensagem)
            throw IllegalStateException(mensagem)
        }

        // Atualizar status da vaga
        logService.info("ESTACIONAMENTO: Atualizando status da vaga id={} para OCUPADA", vaga.id)
        val vagaAtualizada = vaga.copy(status = "OCUPADA")
        vagaRepository.update(vagaAtualizada)
        logService.info("ESTACIONAMENTO: Status da vaga id={} atualizado para OCUPADA", vaga.id)

        // Atualizar transacao
        logService.info("ESTACIONAMENTO: Atualizando transacao id={} para incluir vaga id={}",
            transacao.id!!, vaga.id)
        val horaEstacionamento = ZonedDateTime.now()
        val transacaoAtualizada = transacao.copy(
            vagaId = vaga.id,
            horaEstacionamento = horaEstacionamento,
            status = "ESTACIONADO"
        )

        logService.info("ESTACIONAMENTO: Salvando transacao atualizada no banco de dados...")
        val transacaoSalva = transacaoRepository.update(transacaoAtualizada)
        logService.info("ESTACIONAMENTO: Transacao atualizada: id={}, status={}, hora_estacionamento={}",
            transacaoSalva.id!!, transacaoSalva.status,
            transacaoSalva.horaEstacionamento?.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)!!)

        // Registrar evento
        logService.info("ESTACIONAMENTO: Registrando evento de sistema...")
        eventoSistemaService.registrarEvento(
            tipoEvento = "ESTACIONAMENTO",
            tipoEntidade = "VEICULO",
            entidadeId = transacao.veiculoId,
            descricao = "Veiculo estacionado",
            metadados = mapOf(
                "vaga_id" to vaga.id.toString(),
                "lat" to evento.lat.toString(),
                "lng" to evento.lng.toString()
            )
        )

        val endTime = System.currentTimeMillis()
        logService.info("ESTACIONAMENTO: Processamento concluido em {}ms para veiculo placa={}",
            (endTime - startTime), placa)

        return transacaoSalva
    }

    @Transactional
    open fun processarSaidaVeiculo(evento: ExitEventDto): TransacaoEstacionamento {
        val placa = evento.licensePlate
        val startTime = System.currentTimeMillis()
        logService.info("SAIDA: Iniciando processamento de saida para veiculo placa={}", placa)

        val horaSaida = evento.exitTime ?: ZonedDateTime.now()
        logService.info("SAIDA: Horario de saida registrado={}",
            horaSaida.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))

        // Buscar transacao ativa do veiculo
        logService.info("SAIDA: Buscando transacao ativa para veiculo placa={}", placa)
        val transacaoOpt = transacaoRepository.findActiveByPlaca(placa)
        if (transacaoOpt.isEmpty) {
            val mensagem = "Veiculo com placa $placa nao possui entrada registrada"
            logService.warn("SAIDA: {}", mensagem)
            throw IllegalStateException(mensagem)
        }

        val transacao = transacaoOpt.get()
        logService.info("SAIDA: Transacao ativa encontrada id={}, status={}, entrada={}",
            transacao.id!!, transacao.status,
            transacao.horaEntrada.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))

        // Calcular duracao e preco final
        logService.info("SAIDA: Calculando duracao do estacionamento: entrada={}, saida={}",
            transacao.horaEntrada.format(DateTimeFormatter.ISO_ZONED_DATE_TIME),
            horaSaida.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
        val duracao = calcularDuracaoEstacionamento(transacao.horaEntrada, horaSaida)
        logService.info("SAIDA: Duracao calculada: {} minutos", duracao)

        logService.info("SAIDA: Calculando preco final: base={}, fator={}, duracao={}min",
            transacao.precoBase, transacao.fatorPreco, duracao)
        val precoFinal = calcularPrecoFinal(transacao.precoBase, transacao.fatorPreco, duracao)
        logService.info("SAIDA: Preco final calculado: {}", precoFinal)

        // Liberar a vaga, se estiver ocupando uma
        if (transacao.vagaId != null) {
            logService.info("SAIDA: Verificando vaga id={} para liberacao", transacao.vagaId)
            val vagaOpt = vagaRepository.findById(transacao.vagaId)
            if (vagaOpt.isPresent) {
                val vaga = vagaOpt.get()
                logService.info("SAIDA: Atualizando status da vaga id={} para DISPONIVEL", vaga.id!!)
                val vagaAtualizada = vaga.copy(status = "DISPONIVEL")
                vagaRepository.update(vagaAtualizada)
                logService.info("SAIDA: Vaga id={} liberada com sucesso", vaga.id!!)
            } else {
                logService.warn("SAIDA: Vaga id={} nao encontrada para liberacao", transacao.vagaId)
            }
        } else {
            logService.info("SAIDA: Veiculo nao estava associado a uma vaga especifica")
        }

        // Atualizar transacao
        logService.info("SAIDA: Atualizando transacao id={} com dados de saida", transacao.id)
        val transacaoAtualizada = transacao.copy(
            horaSaida = horaSaida,
            duracaoMinutos = duracao,
            precoFinal = precoFinal,
            status = "SAIU"
        )

        logService.info("SAIDA: Salvando transacao finalizada no banco de dados...")
        val transacaoSalva = transacaoRepository.update(transacaoAtualizada)
        logService.info("SAIDA: Transacao finalizada: id={}, valor={}, duracao={}min",
            transacaoSalva.id!!, precoFinal, duracao)

        // Atualizar ocupacao do setor
        occupationMonitoringService.atualizarOcupacaoSetor(transacao.setorId)

        // Atualizar faturamento
        logService.info("SAIDA: Atualizando faturamento diario para setor id={}...", transacao.setorId)
        faturamentoService.atualizarFaturamentoDiario(transacao.setorId, transacaoSalva)
        logService.info("SAIDA: Faturamento diario atualizado com sucesso")

        // Registrar evento
        logService.info("SAIDA: Registrando evento de sistema...")
        eventoSistemaService.registrarEvento(
            tipoEvento = "SAIDA",
            tipoEntidade = "VEICULO",
            entidadeId = transacao.veiculoId,
            descricao = "Veiculo saiu do estacionamento",
            metadados = mapOf(
                "duracao_minutos" to duracao.toString(),
                "preco_final" to precoFinal.toString()
            )
        )

        val endTime = System.currentTimeMillis()
        logService.info("SAIDA: Processamento concluido em {}ms para veiculo placa={}",
            (endTime - startTime), placa)

        return transacaoSalva
    }

    private fun calcularDuracaoEstacionamento(entrada: ZonedDateTime, saida: ZonedDateTime): Int {
        logService.debug("Calculando duracao entre {} e {}",
            entrada.format(DateTimeFormatter.ISO_ZONED_DATE_TIME),
            saida.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))

        val duracao = Duration.between(entrada, saida)
        val minutos = duracao.toMinutes().toInt()

        logService.debug("Duracao calculada: {} minutos", minutos)
        return minutos
    }

    private fun calcularPrecoFinal(precoBase: BigDecimal, fatorPreco: BigDecimal, duracaoMinutos: Int): BigDecimal {
        // Preco final = preco base por hora * fator de preco * (duracao em minutos / 60)
        logService.debug("Calculando preco final: base={}, fator={}, duracao={}min",
            precoBase, fatorPreco, duracaoMinutos)

        val duracaoEmHoras = BigDecimal(duracaoMinutos).divide(BigDecimal(60), 2, RoundingMode.HALF_EVEN)
        logService.debug("Duracao em horas: {}", duracaoEmHoras)

        val precoIntermediario = precoBase.multiply(fatorPreco)
        logService.debug("Preco base ajustado pelo fator: {}", precoIntermediario)

        val precoFinal = precoIntermediario
            .multiply(duracaoEmHoras)
            .setScale(2, RoundingMode.HALF_EVEN)

        logService.debug("Preco final calculado: {}", precoFinal)
        return precoFinal
    }
}