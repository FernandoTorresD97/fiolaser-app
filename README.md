# Fiolaser Cruz das Almas

Plataforma de agendamento em Java 21 / Spring Boot e HTML/CSS/JavaScript. O catálogo contém 33 procedimentos.

## Executar localmente

Requisitos: Docker Desktop com o engine Linux ativo e Docker Compose recente (suporte a `env_file.required`).

1. Copie `.env.example` para `.env` e substitua os valores `REPLACE_WITH_*` por valores locais próprios. `CPF_AES_KEY` precisa ser uma chave de 32 bytes codificada em Base64; não reutilize chaves entre ambientes.
2. Defina `ADMIN_BOOTSTRAP_EMAIL` com um e-mail novo e `ADMIN_BOOTSTRAP_PASSWORD` com uma senha de pelo menos 10 caracteres. O administrador inicial só é criado quando não há administrador ativo. Não reutilize o e-mail da antiga conta de demonstração.
3. Execute `docker compose up --build -d`.

| Serviço | Endereço |
|---|---|
| Site e API pelo proxy | http://localhost:5500 |
| API diretamente (diagnóstico) | http://localhost:18080 |
| E-mails de desenvolvimento, capturados pelo Mailpit | http://localhost:8025 |
| PostgreSQL / Kafka | localhost:5432 / localhost:9092 |

A porta direta da API pode ser alterada com `BACKEND_PORT`; o site continua usando o proxy em 5500. Essas portas são vinculadas apenas ao computador local. SMTP local não envia mensagens para a internet.

A pasta tem também o projeto Compose `fiolaser-review`, usado para a verificação desta revisão. Para controlá-lo, use `docker compose -p fiolaser-review ...`. Pare esse projeto antes de iniciar outro nas mesmas portas. Seus dados de teste ficam em volume separado.

## Funcionalidades implementadas

- Imagens ilustrativas locais nos cartões de tratamentos e manifesto, com descrição acessível, carregamento sob demanda e arquivos WebP. Não representam clientes ou instalações reais.
- Limites de tentativas de autenticação persistidos no PostgreSQL, compartilhados entre instâncias, com resposta 429 e Retry-After. Login limita também o identificador normalizado, independentemente do IP.
- Logout online revoga os tokens anteriores da conta. JWT exige emissor fiolaser-api e audiência fiolaser-web; tokens antigos sem esses campos exigem novo login.

- Cadastro local com validação de CPF, data de nascimento obrigatória, senha mínima de 10 caracteres e CPF cifrado.
- Login Google OIDC: validação de e-mail verificado, identidade por `sub`, código temporário de 60 segundos e uso único, seguido de troca por JWT. JWT não é colocado na URL.
- Tela de completar cadastro e bloqueio de reservas até informar CPF e nascimento.
- Tela Minha conta para trocar senha local, invalidando tokens anteriores. Contas Google gerenciam a senha no Google.
- Consulta/cancelamento das próprias reservas.
- Administração: criar/editar/cancelar reservas, filtrar período, exportar CSV, buscar usuários, promover/rebaixar administradores e ativar/desativar contas.
- Proteção contra retirar o próprio acesso administrativo ou remover o último administrador ativo.
- Agenda considera duração, expediente das 09h às 19h e grade de 30 minutos, no fuso America/Bahia; bloqueio transacional por procedimento para evitar reservas simultâneas conflitantes.
- Cancelamento libera o horário no banco; preço e duração contratados são preservados na reserva.
- Confirmações persistidas numa outbox na mesma transação da reserva e enviadas ao Kafka com repetição em falha.
- `notification-service` como processo/container separado, reutilizando o mesmo artefato Java: consome `booking-created`, envia e-mail via SMTP e pode usar AWS SNS para clientes sem e-mail.
- Infraestrutura Terraform em `infra/aws`: ECS/Fargate, RDS, MSK Serverless, S3 privado e CloudFront.

## O que ainda não está concluído

**Cobrança online não foi implementada:** depende da escolha do gateway e de suas credenciais. Pix/cartão/dinheiro continuam sendo formas de pagamento combinadas na unidade. As telas e mensagens dizem isso explicitamente. `/api/payments/capabilities` retorna `online: false`, e checkout retorna 503, sem simular cobrança aprovada.

**Deploy AWS não foi executado:** a infraestrutura foi preparada e validada estaticamente. A publicação exige conta AWS, domínio/Route 53, secrets, imagem no ECR e homologação. Veja [o guia AWS](infra/aws/README.md).

**Capacidade da clínica:** foi mantido o modelo original de uma agenda por procedimento. Ainda não há profissional, sala, equipamento, feriados ou quantidade configurável de atendimentos simultâneos. A exclusão de sobreposições vale dentro do mesmo procedimento.

**Integrações externas:** o fluxo com o Google real e o envio por SMTP/SNS em produção exigem credenciais. O e-mail local é verificável pelo Mailpit. SMS é opcional e desligado por padrão. Alterações e cancelamentos de reservas ainda não geram mensagens próprias.

## Configurar Google

Crie um cliente OAuth do tipo aplicação Web. Configure o redirecionamento autorizado:

- Local: `http://localhost:5500/login/oauth2/code/google`
- AWS: `https://SEU-CLOUDFRONT/login/oauth2/code/google`

Defina `GOOGLE_CLIENT_ID` e `GOOGLE_CLIENT_SECRET` no ambiente. `PUBLIC_BASE_URL` é a URL pública usada no callback; `FRONTEND_URL` é a origem do frontend usada em redirecionamentos e CORS. No Compose, ambas usam localhost:5500.

Uma conta local existente **não é vinculada automaticamente** ao Google apenas por ter o mesmo e-mail. Ela deve continuar entrando com senha; uma futura vinculação deve exigir reautenticação.

O estado temporário OAuth fica em sessão apenas durante o login. Na AWS, o ALB tem afinidade por cookie de 5 minutos e o CloudFront encaminha cookies. As APIs usam exclusivamente JWT Bearer; não aceitam sessão como autenticação.

## Notificações

No ambiente local, o worker envia apenas ao Mailpit. Para SMTP externo, configure `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_FROM`, `SMTP_AUTH=true` e `SMTP_TLS=true`.

Para SMS opcional via AWS SNS, configure `SMS_ENABLED=true`, `AWS_REGION` e uma identidade IAM com `sns:Publish`. Na AWS, use `sms_enabled=true` no Terraform. Clientes com e-mail recebem e-mail; clientes sem e-mail usam SMS. Sem canal disponível, o evento vai para o tópico de falhas depois das tentativas.

Há três novas tentativas com intervalo de 5 segundos antes de `booking-created.DLT`. A entrega confirmada fica registrada por evento/canal, impedindo reenvios normais por repetição do Kafka. SMTP/SNS não oferecem atomicidade com o banco: uma queda entre o envio e o registro pode duplicar a mensagem. Não há garantia de exactly-once.

Os eventos antigos do formato anterior, sem `eventId`, vão para DLT e precisam de tratamento na migração. Não os reenvie cegamente. Monitore DLT, atraso do consumidor e tamanho/idade da outbox antes de produção. Há retenção de 7 dias no tópico principal e 14 dias na DLT; a limpeza da outbox publicada e dos registros de entrega ainda deve ser definida.

## Migrations e segurança

A migration V5 preserva as V1–V4 existentes e:

- remove a restrição que impedia reservar novamente um horário cancelado;
- cria os campos de preço/fim de atendimento, outbox, controle de entrega, códigos OAuth e versão de token;
- permite dados de perfil ainda não informados para contas Google;
- desativa a conta seed **se ela ainda tiver o hash da senha pública original**. Uma conta seed cuja senha já foi trocada não é desativada.

Tokens antigos sem versão deixam de ser aceitos: faça login novamente. Bloqueio de conta e troca de perfil/senha são consultados na autenticação de cada requisição.

O frontend usa `sessionStorage` para credenciais. Os nomes e contatos no painel são inseridos como texto, e o CSV neutraliza fórmulas. Isso não substitui uma revisão completa de segurança.

Em produção use `SPRING_PROFILES_ACTIVE=prod`, segredos próprios e HTTPS. Nunca reutilize JWT, pepper ou chave AES de desenvolvimento. A chave AES precisa ter 32 bytes em Base64. Faça backup das chaves com controle de acesso; mudar a chave sem migração pode tornar CPFs antigos irrecuperáveis, e mudar o pepper altera a detecção de duplicidade.

## Validar

Com Java 21, Maven e Docker ativos:

```powershell
cd backend
mvn test
cd ..
node scripts/check-frontend.cjs
docker compose config --quiet
```

Os 31 testes usam PostgreSQL 16 real e descartável (embedded-postgres), sem depender de Docker. Testam concorrência, cancelamento/reutilização, duração, autorização, expiração/uso único de código OAuth, cadastro incompleto, revogação de tokens, outbox, notificações, exportação e limites de autenticação. SMTP é substituído por mock nessa suíte; não há alegação de homologação externa Google/SNS.

## Revisão visual e de autenticação

As novas imagens e os prompts estão registrados em [docs/IMAGENS.md](docs/IMAGENS.md). A migration V6 cria contadores de autenticação com chaves HMAC, sem persistir IP ou identificador em claro. Login permite até 10 tentativas por identificador em uma janela de cinco minutos e até 30 por IP/rota por minuto. Cadastro e troca do código Google também recebem o limite por IP. Os limites incluem tentativas válidas. Falhas do limitador não liberam o login.

O proxy local substitui X-Forwarded-For pela conexão observada, sem confiar no valor enviado pelo cliente. Na infraestrutura AWS, a cadeia de proxies/headers ainda precisa ser homologada; o limite por conta não depende do IP. A limpeza dos contadores ocorre a cada tentativa, removendo janelas com mais de uma hora.

O logout revoga todos os JWTs da conta, inclusive de outras abas/dispositivos. Offline, só é possível limpar a aba local; os tokens no servidor mantêm sua expiração. A troca de senha continua invalidando tokens. Nenhuma senha de usuário, CPF cifrado, pepper ou chave AES foi alterada nesta revisão. A segurança já existente não foi substituída por uma senha compartilhada.

O projeto permanece local e não tinha repositório Git associado nesta pasta. A apresentação do portfólio usa capturas da interface, não anuncia uma implantação AWS nem aceita agendamentos reais.

[Revisão técnica e próximos passos](REVISAO.md).

## Demonstração no portfólio
A demo estática usa perfis fictícios e armazenamento local do navegador. Não conecta ao backend nem envia mensagens ou pagamentos. Para reconstruir: node scripts/build-demo.cjs CAMINHO_DESTINO. Para validar regras da simulação: node --test demo/demo-core.test.cjs (5 testes).
Backend: 31 testes aprovados com PostgreSQL real. Revisão adicional: JWT com emissor/audiência, revogação no logout e limites persistidos de tentativas. Tokens anteriores exigem novo login.

