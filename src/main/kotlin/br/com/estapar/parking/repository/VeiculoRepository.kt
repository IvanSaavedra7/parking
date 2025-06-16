package br.com.estapar.parking.repository

import br.com.estapar.parking.entity.Veiculo
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.Optional

@JdbcRepository(dialect = Dialect.POSTGRES)
interface VeiculoRepository : CrudRepository<Veiculo, Long> {
    fun findByPlaca(placa: String): Optional<Veiculo>
}