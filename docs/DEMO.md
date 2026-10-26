# Demonstração pública

URL: https://fernandotorresd97.github.io/fernando-torres-portfolio/demos/fiolaser/

Esta versão estática não conecta ao backend. Perfis públicos fictícios permitem experimentar o fluxo do cliente e do administrador. Os registros são salvos em localStorage neste navegador; a seleção de perfil dura na aba. Reiniciar demo restaura os exemplos.

Disponível: catálogo de 33 itens, agenda por duração, prevenção de sobreposição por procedimento, reservas, cancelamento, gestão de perfis fictícios e exportação CSV. Preços, disponibilidade e imagens são ilustrativos.

Não disponível: autenticação real, reservas compartilhadas entre visitantes, pagamentos, Google, Kafka e envio de mensagens. As permissões no navegador são apenas uma simulação; a autorização do sistema real é aplicada no backend Spring Security. A demo não serve como sistema de atendimento.

Reconstruir: `node scripts/build-demo.cjs CAMINHO_DESTINO`. O destino atual é `D:/Projetos/fernando-torres-portfolio/portfolio/public/demos/fiolaser`.

Validar: `node --test demo/demo-core.test.cjs` (5 testes) e `node scripts/check-frontend.cjs`. Backend: `mvn verify` (31 testes aprovados com PostgreSQL real, sem testes ignorados). Validado também no navegador: entrada como cliente, reserva e persistência, painel administrativo, criação administrativa, download CSV e menu móvel de 390 px.

Imagens: veja [IMAGENS.md](IMAGENS.md) para os prompts e arquivos gerados com a ferramenta integrada. Os três WebP finais estão em `frontend/img`.
