package br.com.estapar.parking.repository

import br.com.estapar.parking.entity.Setor
import br.com.estapar.parking.entity.Vaga
import br.com.estapar.parking.entity.HistoricoOcupacaoSetor
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.math.BigDecimal
import java.util.Optional

@JdbcRepository(dialect = Dialect.POSTGRES)
interface SetorRepository : CrudRepository<Setor, Long> {
    fun findByCodigoSetor(codigoSetor: String): Optional<Setor>

    @Query("DELETE FROM setores")
    fun truncateTable()
}

@JdbcRepository(dialect = Dialect.POSTGRES)
interface VagaRepository : CrudRepository<Vaga, Long> {
    fun findBySetorId(setorId: Long): List<Vaga>
    fun findByLatitudeAndLongitude(latitude: BigDecimal, longitude: BigDecimal): Optional<Vaga>
    fun countBySetorId(setorId: Long): Long
    fun countBySetorIdAndStatus(setorId: Long, status: String): Long

    @Query("DELETE FROM vagas")
    fun truncateTable()
}

@JdbcRepository(dialect = Dialect.POSTGRES)
interface HistoricoOcupacaoSetorRepository : CrudRepository<HistoricoOcupacaoSetor, Long> {
    @Query(value = "SELECT * FROM historico_ocupacao_setor WHERE setor_id = :setorId ORDER BY timestamp DESC LIMIT 1", nativeQuery = true)
    fun buscarUltimaOcupacaoPorSetor(setorId: Long): Optional<HistoricoOcupacaoSetor>
}