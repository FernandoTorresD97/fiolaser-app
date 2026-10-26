-- Ajusta os nomes que passaram a não usar mais a marca "Método Fiolaser"
UPDATE esthetic_services SET nome = 'Tratamento para Celulite',
    descricao = 'Protocolo voltado a atenuar progressivamente a aparência da celulite, combinando tecnologia e técnicas manuais específicas.'
    WHERE slug = 'fiolaser-celulite';

UPDATE esthetic_services SET nome = 'Tratamento para Flacidez',
    descricao = 'Protocolo voltado ao estímulo da firmeza da pele, reduzindo a flacidez corporal e/ou facial.'
    WHERE slug = 'fiolaser-flacidez';

-- Novos procedimentos
INSERT INTO esthetic_services (id, slug, nome, descricao, duracao_minutos, preco, ativo) VALUES
(gen_random_uuid()::text, 'striout', 'Striout',
 'Tratamento voltado à redução da aparência de estrias, estimulando a renovação da pele na região tratada.',
 45, 190.00, true),

(gen_random_uuid()::text, 'preenchimento-facial', 'Preenchimento Facial',
 'Aplicação de substâncias injetáveis (como ácido hialurônico) para repor volume, suavizar sulcos e harmonizar contornos do rosto.',
 50, 700.00, true),

(gen_random_uuid()::text, 'bioestimulador', 'Bioestimulador de Colágeno',
 'Aplicação injetável que estimula a produção natural de colágeno pela pele, melhorando firmeza e qualidade cutânea ao longo do tempo.',
 50, 750.00, true),

(gen_random_uuid()::text, 'toxina-botulinica', 'Toxina Botulínica',
 'Aplicação injetável que relaxa a musculatura facial, suavizando linhas de expressão em áreas como testa, glabela e "pés de galinha".',
 30, 600.00, true),

(gen_random_uuid()::text, 'peim', 'PEIM',
 'Procedimento estético indicado conforme avaliação profissional prévia. Fale com a equipe para detalhes específicos do protocolo antes de agendar.',
 40, 200.00, true),

(gen_random_uuid()::text, 'lipoenzimatica', 'Lipoenzimática',
 'Aplicação injetável de enzimas que auxiliam na quebra de gordura localizada, indicada para pequenas áreas de gordura resistente.',
 45, 280.00, true),

(gen_random_uuid()::text, 'harmonizacao-corporal-intima', 'Harmonização Corporal e Íntima',
 'Conjunto de técnicas voltadas ao equilíbrio estético de contornos corporais e da região íntima, conforme avaliação individual.',
 50, 350.00, true),

(gen_random_uuid()::text, 'radiofrequencia', 'Radiofrequência',
 'Tecnologia que aquece as camadas da pele para estimular colágeno, auxiliando na firmeza e no combate à flacidez facial e corporal.',
 45, 200.00, true),

(gen_random_uuid()::text, 'ultracavitacao', 'Ultracavitação',
 'Uso de ondas ultrassônicas para romper células de gordura localizada, associada a hábitos saudáveis para potencializar resultados.',
 45, 190.00, true),

(gen_random_uuid()::text, 'corrente-russa', 'Corrente Russa',
 'Eletroestimulação muscular que auxilia na tonificação e no fortalecimento da musculatura em áreas específicas do corpo.',
 40, 150.00, true),

(gen_random_uuid()::text, 'hidrolipoclasia', 'Hidrolipoclasia',
 'Técnica injetável que combina soro e outras substâncias para auxiliar na redução de gordura localizada de forma não cirúrgica.',
 45, 260.00, true),

(gen_random_uuid()::text, 'remocao-tatuagem', 'Remoção de Tatuagem',
 'Uso de laser específico para fragmentar o pigmento da tatuagem, permitindo sua eliminação gradual pelo organismo ao longo de sessões.',
 40, 220.00, true),

(gen_random_uuid()::text, 'despigmentacao-sobrancelhas', 'Despigmentação de Sobrancelhas',
 'Procedimento a laser voltado à remoção de pigmentos de micropigmentação/tatuagem antiga nas sobrancelhas, preparando a área para um novo desenho.',
 30, 180.00, true),

(gen_random_uuid()::text, 'peeling-hollywood', 'Peeling de Hollywood',
 'Peeling a laser que utiliza uma camada de carbono para limpar poros, uniformizar a textura e dar viço imediato à pele do rosto.',
 40, 200.00, true),

(gen_random_uuid()::text, 'clareamento-intimo', 'Clareamento Íntimo',
 'Tratamento voltado à uniformização do tom de pele em regiões íntimas, reduzindo a aparência de manchas e escurecimento.',
 40, 180.00, true),

(gen_random_uuid()::text, 'camuflagem-cicatriz', 'Camuflagem de Cicatriz',
 'Técnica de micropigmentação que disfarça a aparência de cicatrizes, uniformizando a cor da pele na região tratada.',
 60, 350.00, true),

(gen_random_uuid()::text, 'lobuloplastia', 'Lobuloplastia',
 'Procedimento indicado para correção de lóbulos da orelha rasgados ou alongados, mediante avaliação profissional prévia.',
 45, 400.00, true),

(gen_random_uuid()::text, 'remocao-sinais', 'Remoção de Sinais',
 'Procedimento indicado para remoção de pintas e sinais na pele, mediante avaliação profissional prévia.',
 30, 250.00, true),

(gen_random_uuid()::text, 'microagulhamento', 'Microagulhamento',
 'Técnica que utiliza microagulhas para estimular a renovação da pele, auxiliando na textura, manchas e cicatrizes de acne.',
 45, 220.00, true),

(gen_random_uuid()::text, 'camuflagem-estrias', 'Camuflagem de Estrias',
 'Técnica de micropigmentação que disfarça a aparência de estrias, uniformizando a cor da pele na região tratada.',
 60, 350.00, true);
