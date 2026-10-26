ALTER TABLE users ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE users ALTER COLUMN cpf_hash DROP NOT NULL;
ALTER TABLE users ALTER COLUMN cpf_encrypted DROP NOT NULL;
ALTER TABLE users ALTER COLUMN data_nascimento DROP NOT NULL;
UPDATE users SET cpf_hash = NULL, cpf_encrypted = NULL, data_nascimento = NULL
 WHERE provider = 'GOOGLE' AND cpf_hash LIKE 'PENDENTE-%';
-- The public seed account must never retain its known password.
UPDATE users SET ativo = false, token_version = token_version + 1
 WHERE cpf_hash = 'ADMIN-SEED-0001' AND senha_hash = '$2b$12$0YJ.z2X/GcbDXevYQ4acwuIXiBqOaBKuVyOmcy2AudNAQSfTmy5Ia';
CREATE UNIQUE INDEX users_provider_identity ON users(provider, provider_id) WHERE provider_id IS NOT NULL;
CREATE TABLE oauth_login_codes (
 code_hash VARCHAR(64) PRIMARY KEY, user_id VARCHAR(36) NOT NULL REFERENCES users(id),
 expires_at TIMESTAMPTZ NOT NULL
);
ALTER TABLE bookings DROP CONSTRAINT bookings_service_id_horario_key;
ALTER TABLE bookings ADD COLUMN fim_horario TIMESTAMP;
ALTER TABLE bookings ADD COLUMN valor NUMERIC(10,2);
UPDATE bookings b SET fim_horario = b.horario + s.duracao_minutos * interval '1 minute',
 valor = s.preco FROM esthetic_services s WHERE s.id = b.service_id;
ALTER TABLE bookings ALTER COLUMN fim_horario SET NOT NULL;
ALTER TABLE bookings ALTER COLUMN valor SET NOT NULL;
CREATE UNIQUE INDEX bookings_active_slot ON bookings(service_id, horario)
 WHERE status IN ('RESERVADO', 'CONFIRMADO');
CREATE TABLE booking_outbox (
 id VARCHAR(36) PRIMARY KEY, payload TEXT NOT NULL, criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
 publicado_em TIMESTAMPTZ
);
CREATE INDEX booking_outbox_pending ON booking_outbox(criado_em) WHERE publicado_em IS NULL;
CREATE TABLE notification_deliveries (
 event_id VARCHAR(36) NOT NULL, channel VARCHAR(10) NOT NULL,
 sent_at TIMESTAMPTZ NOT NULL DEFAULT now(), PRIMARY KEY(event_id, channel)
);
