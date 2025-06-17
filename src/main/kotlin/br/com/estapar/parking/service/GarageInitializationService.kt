package br.com.estapar.parking.service

import br.com.estapar.commons.util.LogService
import br.com.estapar.parking.client.SimulatorClient
import br.com.estapar.parking.dto.GarageResponseDto
import br.com.estapar.parking.entity.HistoricoOcupacaoSetor
import br.com.estapar.parking.entity.Setor
import br.com.estapar.parking.entity.Vaga
import br.com.estapar.parking.repository.HistoricoOcupacaoSetorRepository
import br.com.estapar.parking.repository.SetorRepository
import br.com.estapar.parking.repository.VagaRepository
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Optional
import jakarta.transaction.Transactional

@Singleton
open class GarageInitializationService(
    private val simulatorClient: SimulatorClient,
    private val setorRepository: SetorRepository,
    private val vagaRepository: VagaRepository,
    private val historicoOcupacaoSetorRepository: HistoricoOcupacaoSetorRepository,
    private val logService: LogService
) {

    @Transactional
    open fun initializeGarage() {
        logService.info("Iniciando processo de inicialização da garagem")

        try {
            // Limpa dados existentes
            limparDadosAnteriores()

            // Obtém configuração atual da garagem do simulador
            val garageConfiguration = simulatorClient.getGarageConfiguration()
            logService.info("Configuração da garagem obtida: ${garageConfiguration.garage.size} setores e ${garageConfiguration.spots.size} vagas")

            // Processa e persiste os setores
            val setoresSalvos = processarSetores(garageConfiguration)

            // Processa e persiste as vagas
            processarVagas(garageConfiguration, setoresSalvos)

            // Inicializa dados de ocupação
            inicializarHistoricoOcupacao(setoresSalvos)

            logService.info("Inicialização da garagem concluída com sucesso")
        } catch (e: Exception) {
            logService.error("Erro ao inicializar a garagem", e)
            throw e
        }
    }

    private fun limparDadosAnteriores() {
        logService.info("Limpando dados anteriores da garagem")
        try {
            vagaRepository.truncateTable()
            setorRepository.truncateTable()
            logService.info("Dados anteriores da garagem removidos com sucesso")
        } catch (e: Exception) {
            logService.error("Erro ao limpar dados anteriores", e)
            throw e
        }
    }

    private fun processarSetores(garageConfiguration: GarageResponseDto): Map<String, Long> {
        logService.info("Processando setores da garagem")
        val setoresSalvos = mutableMapOf<String, Long>()

        garageConfiguration.garage.forEach { setorDto ->
            try {
                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                logService.info("Setor sendo incluido no BD {}:", setorDto)
                val setor = Setor(
                    codigoSetor = setorDto.sector,
                    precoBase = BigDecimal(setorDto.basePrice).setScale(2, RoundingMode.HALF_EVEN),
                    capacidadeMaxima = setorDto.maxCapacity,
                    horaAbertura = LocalTime.parse(setorDto.openHour, timeFormatter),
                    horaFechamento = LocalTime.parse(setorDto.closeHour, timeFormatter),
                    limiteDuracaoMinutos = setorDto.durationLimitMinutes
                )

                val setorSalvo = setorRepository.save(setor)
                setoresSalvos[setorDto.sector] = setorSalvo.id!!
                logService.info("Setor salvo: ${setorDto.sector} - ID: ${setorSalvo.id}")
            } catch (e: Exception) {
                logService.error("Erro ao processar setor ${setorDto.sector}", e)
                throw e
            }
        }

        return setoresSalvos
    }

    private fun processarVagas(garageConfiguration: GarageResponseDto, setoresSalvos: Map<String, Long>) {
        logService.info("Processando vagas da garagem")

        garageConfiguration.spots.forEach { spotDto ->
            try {
                val setorId = setoresSalvos[spotDto.sector]
                    ?: throw IllegalStateException("Setor ${spotDto.sector} não encontrado")

                val vaga = Vaga(
                    idExterno = spotDto.id,
                    setorId = setorId,
                    latitude = BigDecimal(spotDto.lat).setScale(8, RoundingMode.HALF_EVEN),
                    longitude = BigDecimal(spotDto.lng).setScale(8, RoundingMode.HALF_EVEN)
                )

                val vagaSalva = vagaRepository.save(vaga)
                logService.debug("Vaga salva: ID: ${vagaSalva.id}, Setor: ${spotDto.sector}, Coordenadas: ${spotDto.lat},${spotDto.lng}")
            } catch (e: Exception) {
                logService.error("Erro ao processar vaga ${spotDto.id}", e)
                throw e
            }
        }
    }

    private fun inicializarHistoricoOcupacao(setoresSalvos: Map<String, Long>) {
        logService.info("Inicializando histórico de ocupação")

        setoresSalvos.values.forEach { setorId ->
            try {
                val totalVagas = vagaRepository.countBySetorId(setorId).toInt()
                val vagasOcupadas = 0 // Inicialmente, não há vagas ocupadas

                val percentualOcupacao = BigDecimal.ZERO
                val fatorPreco = calcularFatorPreco(percentualOcupacao)

                val historico = HistoricoOcupacaoSetor(
                    setorId = setorId,
                    vagasOcupadas = vagasOcupadas,
                    totalVagas = totalVagas,
                    percentualOcupacao = percentualOcupacao,
                    fatorPrecoAtual = fatorPreco
                )

                historicoOcupacaoSetorRepository.save(historico)
                logService.debug("Histórico de ocupação inicializado para setor ID: $setorId - Total vagas: $totalVagas")
            } catch (e: Exception) {
                logService.error("Erro ao inicializar histórico de ocupação para setor $setorId", e)
                throw e
            }
        }
    }

    private fun calcularFatorPreco(percentualOcupacao: BigDecimal): BigDecimal {
        return when {
            percentualOcupacao < BigDecimal("0.25") -> BigDecimal("0.9")
            percentualOcupacao < BigDecimal("0.5") -> BigDecimal("1.0")
            percentualOcupacao < BigDecimal("0.75") -> BigDecimal("1.1")
            else -> BigDecimal("1.25")
        }
    }
}