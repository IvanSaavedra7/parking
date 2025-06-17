package br.com.estapar.parking.service

import br.com.estapar.commons.util.LogService
import br.com.estapar.parking.entity.Veiculo
import br.com.estapar.parking.repository.VeiculoRepository
import jakarta.inject.Singleton
import jakarta.transaction.Transactional

@Singleton
open class VeiculoService(
    private val veiculoRepository: VeiculoRepository,
    private val logService: LogService
) {

    @Transactional
    open fun obterOuCriarVeiculo(placa: String): Veiculo {
        logService.info("Buscando ou criando veículo com placa $placa")

        val veiculoOptional = veiculoRepository.findByPlaca(placa)

        if (veiculoOptional.isPresent) {
            logService.info("Veículo encontrado: ID ${veiculoOptional.get().id}")
            return veiculoOptional.get()
        }

        logService.info("Veículo não encontrado, criando novo registro")
        val novoVeiculo = Veiculo(placa = placa)
        return veiculoRepository.save(novoVeiculo).also {
            logService.info("Novo veículo criado: ID ${it.id}")
        }
    }
}