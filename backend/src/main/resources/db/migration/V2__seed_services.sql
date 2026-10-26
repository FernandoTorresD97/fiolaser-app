INSERT INTO esthetic_services (id, slug, nome, descricao, duracao_minutos, preco, ativo) VALUES
(gen_random_uuid()::text, 'depilacao-a-laser', 'Depilação a Laser',
 'Método que utiliza luz laser para atingir o folículo piloso e reduzir progressivamente o crescimento dos pelos, com resultados duradouros e menos desconforto que a depilação tradicional.',
 45, 150.00, true),

(gen_random_uuid()::text, 'criolipolise', 'Criolipólise',
 'Tratamento não invasivo que utiliza o frio controlado para reduzir localizadamente as células de gordura, indicado para gordura localizada resistente a dieta e exercício.',
 60, 280.00, true),

(gen_random_uuid()::text, 'limpeza-de-pele', 'Limpeza de Pele',
 'Procedimento de higienização profunda que remove cravos, impurezas e células mortas, equilibrando a pele e preparando-a para outros tratamentos faciais.',
 50, 120.00, true),

(gen_random_uuid()::text, 'revitalizacao-facial', 'Revitalização Facial',
 'Conjunto de técnicas que estimulam a renovação celular da pele do rosto, melhorando viço, textura e uniformidade do tom da pele.',
 45, 180.00, true),

(gen_random_uuid()::text, 'peeling-de-diamante', 'Peeling de Diamante',
 'Esfoliação mecânica que utiliza uma ponteira com micropartículas de diamante para remover camadas superficiais da pele, suavizando manchas, cicatrizes leves e linhas finas.',
 40, 140.00, true),

(gen_random_uuid()::text, 'massagem-modeladora', 'Massagem Modeladora',
 'Massagem com movimentos intensos que auxiliam na quebra de nódulos de gordura e na definição do contorno corporal, associada a hábitos saudáveis.',
 50, 130.00, true),

(gen_random_uuid()::text, 'massagem-relaxante', 'Massagem Relaxante',
 'Massagem de toque suave e contínuo, indicada para aliviar tensões musculares, reduzir o estresse e proporcionar bem-estar geral.',
 50, 120.00, true),

(gen_random_uuid()::text, 'drenagem-linfatica', 'Drenagem Linfática',
 'Massagem que estimula o sistema linfático a eliminar líquidos e toxinas retidas no corpo, reduzindo inchaço e sensação de peso nas pernas.',
 50, 130.00, true),

(gen_random_uuid()::text, 'endermologia', 'Endermologia',
 'Massagem mecânica realizada com aparelho de sucção e rolos, que estimula a circulação e a produção de colágeno, auxiliando na melhora da celulite e flacidez.',
 45, 160.00, true),

(gen_random_uuid()::text, 'power-bumbum', 'Power Bumbum',
 'Protocolo focado no glúteo que combina técnicas manuais e/ou aparelhos para tonificar, firmar e melhorar o contorno da região.',
 50, 170.00, true),

(gen_random_uuid()::text, 'fiolaser-celulite', 'Método Fiolaser para Celulite',
 'Protocolo exclusivo Fiolaser que combina tecnologia e técnicas manuais específicas para atenuar a aparência da celulite de forma progressiva.',
 60, 220.00, true),

(gen_random_uuid()::text, 'fiolaser-flacidez', 'Método Fiolaser para Flacidez',
 'Protocolo exclusivo Fiolaser voltado ao estímulo da firmeza da pele, reduzindo a flacidez corporal e/ou facial com tecnologia própria da marca.',
 60, 220.00, true),

(gen_random_uuid()::text, 'terapia-capilar', 'Terapia Capilar',
 'Tratamento voltado à saúde do couro cabeludo e dos fios, auxiliando no combate à queda de cabelo e estimulando o crescimento capilar saudável.',
 45, 150.00, true);
