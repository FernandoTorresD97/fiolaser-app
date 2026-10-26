# Publicação AWS

Esta configuração cria VPC em duas zonas, duas NAT Gateways, ALB HTTPS, ECS/Fargate, RDS PostgreSQL Multi-AZ, MSK Serverless com IAM, ECR, CloudWatch Logs e frontend em S3 privado com CloudFront/OAC.

**Ainda não foi aplicada numa conta AWS.** `terraform validate` valida a configuração, não comprova disponibilidade de quotas, domínio, permissões ou execução em produção.

## Pré-requisitos

- Terraform >= 1.10, AWS CLI, Docker e credenciais de uma conta AWS autorizada.
- Zona pública Route 53 e um subdomínio de origem da API, por exemplo `origin-api.seudominio.com.br`.
- Um segredo JSON no Secrets Manager, na mesma região, com:
  `JWT_SECRET`, `CPF_PEPPER`, `CPF_AES_KEY`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `SMTP_PASSWORD`, `ADMIN_BOOTSTRAP_PASSWORD`.
- Segredos próprios. Chave CPF AES de 32 bytes em Base64; JWT com pelo menos 32 bytes. Não colocar valores reais no tfvars ou no repositório.
- E-mail novo em `bootstrap_email` para criar o primeiro administrador.
- Para SMTP, preencher host/usuário/remetente e configurar o provedor; o worker permanece desligado enquanto `notifications_enabled=false`.
- Para SMS opcional, `sms_enabled=true` adiciona a permissão SNS e habilita o adaptador; homologar destinos e limites no SNS. O canal SMS é usado para cadastros sem e-mail.
- Usar a chave gerenciada padrão do Secrets Manager ou adicionar `kms:Decrypt` para a chave customizada adotada.

## Provisionamento e publicação

1. Configure um backend remoto Terraform privado, cifrado e com bloqueio de estado antes de trabalho em equipe.
2. Copie `terraform.tfvars.example` para `terraform.tfvars` e preencha somente parâmetros/ARNs.
3. Execute `terraform init`, `terraform validate` e `terraform plan -out=deploy.plan`. Revise região, domínio, quotas e custos recorrentes (RDS/MSK/NAT/ALB permanecem provisionados mesmo com zero tasks).
4. Aplique o plano. O padrão `desired_count=0` permite criar o ECR e a infraestrutura antes da imagem existir.
5. Obtenha `ecr_repository` nos outputs. Faça login no ECR pelo AWS CLI, construa `backend/Dockerfile`, marque e envie com a mesma `image_tag`. As tags são imutáveis: use uma tag nova a cada versão.
6. Envie o conteúdo de `frontend/` ao bucket `frontend_bucket` com `aws s3 sync`. Não copie .env, backend ou arquivos de teste.
7. Configure no Google o callback `<frontend_url>/login/oauth2/code/google`. O endereço público do frontend também é usado como origem da API; não use o domínio interno do ALB no navegador.
8. Altere `desired_count` para 1 inicialmente e aplique um novo plano. Habilite notificações somente depois de configurar SMTP/SNS.
9. Confirme health, login, Google, cadastro, reserva, consumo Kafka, DLT, e-mail/SMS sandbox e exportação antes de abrir ao público.
10. Retire o bootstrap de futuras tasks depois de criar o administrador. Para uma nova versão use tag nova e force a atualização via Terraform. Invalide o CloudFront ao alterar os arquivos estáticos.

A configuração permite duas ou mais tasks e usa afinidade temporária do ALB para OAuth. Uma troca de task durante um login pode exigir repetir o login; não há sessão OAuth distribuída.

## Proteções e limitações

- Banco e Kafka ficam em sub-redes privadas. Tasks não recebem IP público.
- O ALB aceita HTTPS apenas da lista gerenciada de IPs de origem do CloudFront; o domínio da origem não deve ser divulgado como URL da aplicação.
- S3 bloqueia acesso público. CloudFront usa OAC.
- As rotas `/api/*`, `/oauth2/*` e `/login/*` não usam cache e encaminham cookies/cabeçalhos ao ALB.
- O domínio padrão CloudFront é usado nesta primeira versão; configurar domínio próprio do frontend/ACM us-east-1 é uma extensão separada.
- RDS tem cifragem, backup de 7 dias e proteção contra exclusão. O driver usa TLS (`sslmode=require`); configurar CA e `verify-full` na homologação.
- A senha RDS é gerenciada pelo serviço e injetada no início das tasks. Rotação exige renovação das tasks/conexões; separar credenciais de aplicação e migrations antes de produção.
- Os processos compartilham permissões Kafka nesta base. Refinar papéis distintos se exigido pelo ambiente.
- SMS por telefone exige `sns:Publish` com Resource "*"; a política limita a região, e o recurso só é criado quando habilitado.
- Alarmes, autoscaling, WAF/rate limiting, restore drill e replay de DLT não foram automatizados.
- O estado local e o arquivo de plano são sensíveis. Não os versionar.
