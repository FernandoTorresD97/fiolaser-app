-- Usuário administrador padrão. Troque a senha assim que possível
-- (não existe tela de "trocar senha" ainda — pode ser feito direto no banco por enquanto).
--
-- Login: admin@fiolasercruzdasalmas.com.br
-- Senha: Fiolaser@2026
INSERT INTO users (
    id, nome_completo, email, telefone, cpf_hash, cpf_encrypted,
    data_nascimento, senha_hash, provider, provider_id, role, ativo, criado_em
) VALUES (
    gen_random_uuid()::text,
    'Administração Fiolaser',
    'admin@fiolasercruzdasalmas.com.br',
    NULL,
    'ADMIN-SEED-0001',
    '',
    '1990-01-01',
    '$2b$12$0YJ.z2X/GcbDXevYQ4acwuIXiBqOaBKuVyOmcy2AudNAQSfTmy5Ia',
    'LOCAL',
    NULL,
    'ADMIN',
    true,
    now()
);
