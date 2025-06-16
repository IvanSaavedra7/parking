package br.com.estapar.parking.repository

import br.com.estapar.parking.entity.TransacaoEstacionamento
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.time.LocalDate
import java.util.Optional

@JdbcRepository(dialect = Dialect.POSTGRES)
interface TransacaoEstacionamentoRepository : CrudRepository<TransacaoEstacionamento, Long> {

    @Query("""
        SELECT t.* FROM transacoes_estacionamento t 
        JOIN veiculos v ON t.veiculo_id = v.id 
        WHERE v.placa = :placa AND t.status IN ('ENTROU', 'ESTACIONADO')
    """)
    fun findActiveByPlaca(placa: String): Optional<TransacaoEstacionamento>

    @Query("""
        SELECT t.* FROM transacoes_estacionamento t 
        JOIN vagas v ON t.vaga_id = v.id 
        WHERE v.id = :vagaId AND t.status = 'ESTACIONADO'
    """)
    fun findActiveByVagaId(vagaId: Long): Optional<TransacaoEstacionamento>

    @Query("""
        SELECT t.* FROM transacoes_estacionamento t 
        WHERE t.setor_id = :setorId AND t.status = 'SAIU' 
        AND DATE(t.hora_saida) = :data
    """)
    fun findAllCompletedBySetorIdAndDate(setorId: Long, data: LocalDate): List<TransacaoEstacionamento>

    @Query("""
        SELECT COUNT(*) FROM transacoes_estacionamento t
        WHERE t.setor_id = :setorId AND t.status IN ('ENTROU', 'ESTACIONADO')
    """)
    fun countActiveBySetorId(setorId: Long): Long
}