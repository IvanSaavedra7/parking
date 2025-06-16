package br.com.estapar.parking.repository

import br.com.estapar.parking.entity.EventoSistema
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

@JdbcRepository(dialect = Dialect.POSTGRES)
interface EventoSistemaRepository : CrudRepository<EventoSistema, Long>