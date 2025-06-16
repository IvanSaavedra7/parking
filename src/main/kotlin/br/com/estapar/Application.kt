package br.com.estapar

import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.runtime.Micronaut
import io.micronaut.runtime.event.ApplicationStartupEvent
import jakarta.inject.Singleton
import br.com.estapar.parking.service.GarageInitializationService
import br.com.estapar.commons.util.LogService

fun main(args: Array<String>) {
    Micronaut.build()
        .args(*args)
        .packages("br.com.estapar")
        .start()
}

@Singleton
class AppStartupListener(
    private val garageInitializationService: GarageInitializationService,
    private val logService: LogService
) : ApplicationEventListener<ApplicationStartupEvent> {

    override fun onApplicationEvent(event: ApplicationStartupEvent) {
        logService.info("Aplicação iniciada. Iniciando carga de dados da garagem...")
        try {
            garageInitializationService.initializeGarage()
            logService.info("Carga de dados da garagem concluída com sucesso")
            logService.info("Sistema pronto para receber eventos de webhook na porta 3003")
        } catch (e: Exception) {
            logService.error("Erro ao inicializar dados da garagem", e)
            // Não interrompe a inicialização da aplicação, mas deixa o log do erro
        }
    }
}