# Revisão técnica — 16/09/2026

## Resultado e escopo

A revisão incluiu leitura de backend, migrations, frontend e Compose; implementação dos fluxos de autenticação/administração; correções na agenda; worker de notificações; testes e preparação de infraestrutura. Não houve publicação na AWS nem cobranças ou mensagens para clientes reais.

## Achados corrigidos

| Prioridade | Problema original | Alteração |
|---|---|---|
| Alta | Nome/contato do cliente interpolado em HTML no painel; um cadastro malicioso poderia executar JavaScript no navegador do administrador | Renderização por textContent/elementos DOM; escape do nome no cabeçalho |
| Alta | Administrador criado com senha pública e segredos de desenvolvimento aceitos sem distinção | Desativação condicional do seed na V5; bootstrap por ambiente; perfil prod exige segredos e rejeita placeholders conhecidos |
| Alta | Cancelar não liberava o horário devido ao UNIQUE incondicional | Índice parcial para reservas ativas e teste real de cancelamento/reutilização |
| Alta | Agenda comparava somente o início, ignorando duração, expediente e horários enviados diretamente à API | Validação no serviço, duração/fim preservados e trava por procedimento em transação |
| Alta | Kafka era publicado antes do commit sem tratar falha assíncrona | Outbox gravada na transação da reserva e publicação com confirmação/repetição |
| Alta | Conta bloqueada ou administrador rebaixado continuaria autorizado pelo token | Consulta ao estado atual e versão de token a cada requisição |
| Alta | Login Google não completava o acesso à aplicação | Handler OIDC, código temporário, troca por JWT e tela de completar perfil |
| Média | Cadastro aceitava nascimento nulo até falhar no banco e não exigia força mínima no backend | Validação obrigatória, senha mínima, limite em bytes BCrypt |
| Média | CORS ignorava FRONTEND_URL; frontend apontava para localhost em produção | Origem configurada e proxy de mesma origem no Nginx/CloudFront |
| Média | Painel só listava/cancelava e filtrava um dia | Criação/edição manual, período inclusivo de até 366 dias, relatório CSV e gestão de usuários |
| Média | Mensagem dizia “confirmado” embora só registrasse a reserva e a forma de pagamento | Interface comunica reserva recebida e pagamento na unidade |

## Decisões mantidas explícitas

1. **Pagamento:** ainda não há gateway, cobrança, webhook de pagamento, estorno ou expiração de reserva por falta de pagamento. O endpoint de checkout falha explicitamente. Escolher gateway antes de implementar, com valores obtidos do servidor, idempotência, assinatura de webhook, conciliação e testes sandbox. Retorno do navegador nunca deve comprovar pagamento.
2. **Capacidade:** uma agenda por procedimento, como no projeto original. Definir profissionais/equipamentos/salas e capacidade antes de usar para gerir uma operação com recursos compartilhados. Bloqueio por procedimento não impede dois procedimentos distintos de disputarem a mesma sala.
3. **Google:** autenticação por subject e e-mail verificado; sem associação automática com conta local por e-mail. A aplicação não entrega JWT em parâmetros de URL.
4. **Notificações:** SMTP genérico; adaptador SNS opcional. Contêiner separado, mas mesmo JAR e banco para esta fase. A separação não é ainda um microsserviço com banco próprio.
5. **AWS:** código de provisionamento, não deploy concluído. Não foi executado terraform plan/apply com credenciais reais.
6. **Dados existentes:** horários que já estavam sobrepostos não são automaticamente desmarcados. O valor/fim das reservas antigas é preenchido com o catálogo atual, pois a versão anterior não guardava o histórico.
7. **Conta inicial:** configurar um novo administrador se a conta seed ainda estava com a senha pública. Não existe senha universal nova.

## Pendências para produção

- Escolha e homologação do gateway Pix/cartão; política de cancelamento/estorno e duração da reserva.
- Credenciais Google, SMTP, AWS/SNS e domínio; verificar limites e disponibilidade dos provedores.
- Atualizar a linha Spring Boot 3.3.4 herdada e revisar dependências/imagens com scanner. A revisão funcional não é uma certificação de segurança nem uma auditoria de CVEs.
- Separar usuário de migrations do usuário de execução no RDS; a base Terraform inicial usa o usuário gerenciado do RDS. Definir rotação/reinício das tasks para atualizar o segredo injetado no ECS.
- Backend remoto Terraform com bloqueio de estado, alarmes operacionais, política de retenção de dados pessoais, teste de restauração e backup.
- Recuperação de senha, verificação de contato e trilha de auditoria administrativa.
- Eventos próprios para alterações/cancelamentos, replay operacional da DLT e limpeza da outbox.
- Política de atendimento: dias úteis, feriados, antecedência mínima/máxima e cancelamento de reservas passadas.
- Paginação/limites maiores conforme o crescimento: busca de usuários retorna até 100; relatório limita o período, mas não o número de linhas.
- CSP mais restritiva após remover os scripts e handlers inline remanescentes do frontend.

## Referências técnicas consultadas

- [Google OpenID Connect](https://developers.google.com/identity/openid-connect/openid-connect) — identidade e claims de login.
- [Spring Kafka: tratamento de falhas](https://docs.spring.io/spring-kafka/reference/kafka/annotation-error-handling.html) — retries e DLT.
- [AWS: rede do ECS](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/networking-connecting-vpc.html) — conectividade de tasks privadas.
- [AWS: acesso privado ao S3 via CloudFront](https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-management.html).
- [AWS SNS: sandbox SMS](https://docs.aws.amazon.com/sns/latest/dg/sns-sms-sandbox.html) — liberação do envio para destinos reais.

## Revisão adicional — 15/10/2026
- 31 testes de backend aprovados em PostgreSQL real; 5 testes da demo aprovados.
- Limites persistidos de login, cadastro e troca OAuth; identificadores protegidos por HMAC.
- JWT exige emissor e audiência; logout revoga tokens; senhas inválidas usam comparação BCrypt uniforme.
- Três imagens ilustrativas WebP, texto alternativo e menu móvel acessível.
- Demo pública isolada, sem dados pessoais ou chamadas ao backend; documentação em docs/DEMO.md.

