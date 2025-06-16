package br.com.estapar.parking.repository

import br.com.estapar.parking.entity.FaturamentoDiario
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.time.LocalDate
import java.util.Optional

@JdbcRepository(dialect = Dialect.POSTGRES)
interface FaturamentoDiarioRepository : CrudRepository<FaturamentoDiario, Long> {
    fun findBySetorIdAndData(setorId: Long, data: LocalDate): Optional<FaturamentoDiario>
}