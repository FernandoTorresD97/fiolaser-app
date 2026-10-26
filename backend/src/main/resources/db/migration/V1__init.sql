CREATE TABLE users (
    id              VARCHAR(36) PRIMARY KEY,
    nome_completo   VARCHAR(255) NOT NULL,
    email           VARCHAR(255) UNIQUE,
    telefone        VARCHAR(30) UNIQUE,
    cpf_hash        VARCHAR(64) NOT NULL UNIQUE,
    cpf_encrypted   TEXT NOT NULL,
    data_nascimento DATE NOT NULL,
    senha_hash      VARCHAR(255),
    provider        VARCHAR(20) NOT NULL,
    provider_id     VARCHAR(255),
    role            VARCHAR(20) NOT NULL,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE esthetic_services (
    id                VARCHAR(36) PRIMARY KEY,
    slug              VARCHAR(100) NOT NULL UNIQUE,
    nome              VARCHAR(150) NOT NULL,
    descricao         TEXT NOT NULL,
    duracao_minutos   INT NOT NULL,
    preco             NUMERIC(10,2) NOT NULL,
    ativo             BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE bookings (
    id                VARCHAR(36) PRIMARY KEY,
    user_id           VARCHAR(36) NOT NULL REFERENCES users(id),
    service_id        VARCHAR(36) NOT NULL REFERENCES esthetic_services(id),
    horario           TIMESTAMP NOT NULL,
    forma_pagamento   VARCHAR(20) NOT NULL,
    status            VARCHAR(20) NOT NULL,
    criado_em         TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (service_id, horario)
);

CREATE INDEX idx_bookings_user ON bookings(user_id);
CREATE INDEX idx_bookings_service_horario ON bookings(service_id, horario);
