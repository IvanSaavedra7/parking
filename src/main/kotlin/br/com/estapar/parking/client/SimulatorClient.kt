package br.com.estapar.parking.client

import br.com.estapar.parking.dto.GarageResponseDto
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.annotation.Client

@Client(id = "simulator", path = "/")
interface SimulatorClient {

    @Get("/garage")
    fun getGarageConfiguration(): GarageResponseDto
}