-- Tabela para armazenar os setores da garagem
CREATE TABLE setores (
    id SERIAL PRIMARY KEY,
    codigo_setor VARCHAR(10) NOT NULL UNIQUE,
    preco_base NUMERIC(10, 2) NOT NULL,
    capacidade_maxima INTEGER NOT NULL,
    hora_abertura TIME NOT NULL,
    hora_fechamento TIME NOT NULL,
    limite_duracao_minutos INTEGER,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Tabela para armazenar as vagas individuais de estacionamento
CREATE TABLE vagas (
    id SERIAL PRIMARY KEY,
    id_externo INTEGER NOT NULL,
    setor_id INTEGER NOT NULL REFERENCES setores(id) ON DELETE CASCADE,
    latitude NUMERIC(10, 8) NOT NULL,
    longitude NUMERIC(11, 8) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DISPONIVEL',
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (latitude, longitude)
);

-- Tabela para armazenar os dados dos veículos
CREATE TABLE veiculos (
    id SERIAL PRIMARY KEY,
    placa VARCHAR(20) NOT NULL UNIQUE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Tabela para registrar as transações de estacionamento (entrada, permanência e saída)
CREATE TABLE transacoes_estacionamento (
    id SERIAL PRIMARY KEY,
    veiculo_id INTEGER NOT NULL REFERENCES veiculos(id) ON DELETE CASCADE,
    setor_id INTEGER NOT NULL REFERENCES setores(id) ON DELETE CASCADE,
    vaga_id INTEGER REFERENCES vagas(id) ON DELETE CASCADE,
    hora_entrada TIMESTAMP WITH TIME ZONE NOT NULL,
    hora_estacionamento TIMESTAMP WITH TIME ZONE,
    hora_saida TIMESTAMP WITH TIME ZONE,
    duracao_minutos INTEGER,
    preco_base NUMERIC(10, 2) NOT NULL,
    fator_preco NUMERIC(5, 2) NOT NULL DEFAULT 1.0,
    preco_final NUMERIC(10, 2),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ENTROU', 'ESTACIONADO', 'SAIU', 'CANCELADO')),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Tabela para registrar o histórico de ocupação dos setores ao longo do tempo
CREATE TABLE historico_ocupacao_setor (
    id SERIAL PRIMARY KEY,
    setor_id INTEGER NOT NULL REFERENCES setores(id) ON DELETE CASCADE,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    vagas_ocupadas INTEGER NOT NULL,
    total_vagas INTEGER NOT NULL,
    percentual_ocupacao NUMERIC(5, 2) NOT NULL,
    fator_preco_atual NUMERIC(5, 2) NOT NULL
);

-- Tabela para registrar o faturamento diário por setor
CREATE TABLE faturamento_diario (
    id SERIAL PRIMARY KEY,
    setor_id INTEGER NOT NULL REFERENCES setores(id) ON DELETE CASCADE,
    data DATE NOT NULL,
    valor NUMERIC(10, 2) NOT NULL DEFAULT 0,
    quantidade_transacoes INTEGER NOT NULL DEFAULT 0,
    tempo_medio_permanencia_minutos NUMERIC(10, 2) DEFAULT 0,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (setor_id, data)
);

-- Tabela para registrar eventos do sistema (trilha de auditoria)
CREATE TABLE eventos_sistema (
    id SERIAL PRIMARY KEY,
    tipo_evento VARCHAR(50) NOT NULL,
    tipo_entidade VARCHAR(50) NOT NULL,
    entidade_id INTEGER NOT NULL,
    descricao TEXT,
    metadados JSONB,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    usuario_id VARCHAR(50)
);

-- Índices para otimização de consultas frequentes
CREATE INDEX idx_vagas_setor ON vagas(setor_id);
CREATE INDEX idx_transacoes_veiculo ON transacoes_estacionamento(veiculo_id);
CREATE INDEX idx_transacoes_status ON transacoes_estacionamento(status);
CREATE INDEX idx_transacoes_hora_entrada ON transacoes_estacionamento(hora_entrada);
CREATE INDEX idx_historico_ocupacao_timestamp ON historico_ocupacao_setor(timestamp);
CREATE INDEX idx_eventos_sistema_tipo ON eventos_sistema(tipo_evento, tipo_entidade);