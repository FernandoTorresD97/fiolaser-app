const fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'..'),dest=path.resolve(process.argv[2]||path.join(root,'demo-dist'));
fs.mkdirSync(dest,{recursive:true});
for(const dir of ['css','img','js'])fs.cpSync(path.join(root,'frontend',dir),path.join(dest,dir),{recursive:true});
const sql=['V2__seed_services.sql','V3__update_services.sql'].map(file=>fs.readFileSync(path.join(root,'backend/src/main/resources/db/migration',file),'utf8')).join('\n');
const re=/\(gen_random_uuid\(\)::text,\s*'((?:[^']|'')*)',\s*'((?:[^']|'')*)',\s*'((?:[^']|'')*)',\s*(\d+),\s*(\d+(?:\.\d+)?),\s*true\)/g;
const catalog=[...sql.matchAll(re)].map(m=>({id:m[1],nome:m[2].replaceAll("''","'"),descricao:'Item de demonstração. Valores, duração e horários ilustrativos.',duracaoMinutos:Number(m[4]),preco:Number(m[5]),ativo:true}));
if(catalog.length!==33)throw Error('Catálogo incompleto: '+catalog.length);
for(const item of catalog){if(item.id==='fiolaser-celulite')item.nome='Tratamento para Celulite';if(item.id==='fiolaser-flacidez')item.nome='Tratamento para Flacidez';}
fs.writeFileSync(path.join(dest,'js/catalog.js'),'window.FIOLASER_CATALOG='+JSON.stringify(catalog)+';');
for(const name of ['demo-core.cjs','demo.js'])fs.copyFileSync(path.join(root,'demo',name),path.join(dest,'js',name==='demo-core.cjs'?'demo-core.js':name));
let main=fs.readFileSync(path.join(dest,'js/main.js'),'utf8');
main=main.slice(0,main.indexOf('async function apiFetch('))+'async function apiFetch(path,options={}) { return window.FIOLASER_DEMO_API(path,options); }\n\n'+main.slice(main.indexOf('const PAGINAS_NAV'));
for(const key of ['token','nome','role'])main=main.replaceAll('fiolaser_'+key,'fiolaser_demo_'+key);
main=main.replaceAll('Criar conta','Testar perfis').replace(/https:\/\/(?:www\.)?instagram\.com[^"\s]*/g,'sobre-projeto.html').replaceAll('Associação Brasileira<br>de Franchising','Projeto de portfólio<br>Demonstração interativa').replaceAll('href="conta.html">Minha conta','href="meus-agendamentos.html">Minhas reservas');
fs.writeFileSync(path.join(dest,'js/main.js'),main);
let admin=fs.readFileSync(path.join(dest,'js/admin.js'),'utf8');
admin=admin.replace(/const res=await fetch\(API_BASE[\s\S]*?const url=URL\.createObjectURL\(await res\.blob\(\)\)/,'const csv=await apiFetch("/api/admin/bookings/export?"+queryPeriod());\n  const url=URL.createObjectURL(new Blob([csv],{type:"text/csv;charset=utf-8"}))');
if(admin.includes('fetch('))throw Error('Exportação ainda usa rede');
fs.writeFileSync(path.join(dest,'js/admin.js'),admin);
const inject=html=>html.replace('</head>','<link rel="stylesheet" href="css/demo.css"></head>').replace('<script src="js/main.js">','<script src="js/catalog.js"></script><script src="js/demo-core.js"></script><script src="js/demo.js"></script><script src="js/main.js">');
for(const name of ['index','servicos','agendamento','meus-agendamentos','admin','quem-somos','perguntas-frequentes','politica-privacidade']){
 const file=path.join(root,'frontend',name+'.html');if(!fs.existsSync(file))continue;
 fs.writeFileSync(path.join(dest,name+'.html'),inject(fs.readFileSync(file,'utf8').replaceAll('Reserva recebida! Nenhuma cobrança online foi realizada.','Simulação salva neste navegador. Nenhuma reserva real ou cobrança foi realizada.')));
}
const page=(title,body)=>inject(`<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${title} · Fiolaser Demo</title><link rel="stylesheet" href="css/style.css"></head><body><div id="header-fiolaser"></div><main class="demo-intro container"><p class="demo-eyebrow">PROJETO DE PORTFÓLIO</p><h1>${title}</h1>${body}</main><div id="footer-fiolaser"></div><script src="js/main.js"></script></body></html>`);
const login=page('Experimente a plataforma','<p>Escolha um perfil fictício. Não informe dados pessoais nem use credenciais reais.</p><div class="demo-profiles"><button class="btn btn-primario" onclick="entrarDemo(\'demo-ana\')">Entrar como Ana · cliente</button><button class="btn btn-secundario" onclick="entrarDemo(\'demo-lucas\')">Entrar como Lucas · cliente</button><button class="btn btn-secundario" onclick="entrarDemo(\'demo-admin\')">Explorar painel administrativo</button></div><p id="demo-login-status" role="status"></p><a href="meus-agendamentos.html">Meus agendamentos</a> · <a href="sobre-projeto.html">Como funciona esta demo</a>');
for(const name of ['login','cadastro','conta','completar-cadastro'])fs.writeFileSync(path.join(dest,name+'.html'),login);
fs.writeFileSync(path.join(dest,'sobre-projeto.html'),page('Fiolaser · sobre o projeto','<p>Explore 33 procedimentos, disponibilidade por duração, reservas, cancelamentos, gestão de clientes e exportação CSV.</p><p>Esta demonstração é uma simulação em JavaScript: os dados ficam apenas neste navegador. Os perfis são públicos e não representam autenticação de produção. Preços, imagens, pessoas e reservas são ilustrativos.</p><p>O projeto completo usa Java 21, Spring Boot, PostgreSQL, Flyway, JWT revogável e outbox com Kafka. O backend teve 31 testes aprovados com PostgreSQL real. Esse backend não está conectado a esta página. AWS, Google, pagamentos e mensagens não são acionados pela demo.</p><p>As imagens editoriais foram geradas por IA; não representam clientes ou instalações reais.</p><a class="btn btn-primario" href="login.html">Começar demonstração</a>'));
fs.writeFileSync(path.join(dest,'css/demo.css'),`.demo-banner{background:#28172a;color:#fff;padding:14px 5%;display:flex;gap:18px;justify-content:space-between;align-items:center;position:relative;z-index:20;font:14px/1.5 system-ui}.demo-banner strong,.demo-banner span{display:block}.demo-banner span{opacity:.8;max-width:650px}.demo-banner nav{display:flex;gap:14px;flex-wrap:wrap}.demo-banner a,.demo-banner button{color:#fff;background:none;border:0;text-decoration:underline;cursor:pointer;font:inherit}.demo-intro{padding:70px 24px;min-height:60vh;max-width:1000px}.demo-intro h1{font-size:clamp(30px,5vw,52px)}.demo-intro p{max-width:760px;margin:22px 0;line-height:1.8}.demo-eyebrow{letter-spacing:.15em;color:#8d243b}.demo-profiles{display:flex;gap:18px;flex-wrap:wrap;margin:35px 0}@media(max-width:750px){.demo-banner{align-items:flex-start;flex-direction:column}.topbar-inner{flex-wrap:wrap}.nav{flex-wrap:wrap}.acoes-topo{flex-wrap:wrap}}`);
console.log(`Demo criada: ${dest} (${catalog.length} procedimentos)`);
// Keep deployed CSS/JS in sync even when a browser cached an earlier demo.
for(const name of fs.readdirSync(dest).filter(n=>n.endsWith('.html'))){
 const file=path.join(dest,name);const html=fs.readFileSync(file,'utf8').replace(/(src|href)="((?:js|css)\/[^"?]+\.(?:js|css))"/g,(_,attr,url)=>{
 const hash=require('node:crypto').createHash('sha256').update(fs.readFileSync(path.join(dest,url))).digest('hex').slice(0,10);return `${attr}="${url}?v=${hash}"`;});fs.writeFileSync(file,html);
}
