// Ajuste para a URL do backend em produção (ex.: https://api.fiolasercruzdasalmas.com.br)
const API_BASE = window.API_BASE ?? "";

function getToken() {
  return sessionStorage.getItem("fiolaser_token");
}

function isLogado() {
  return !!getToken();
}

function limparAcesso() {
  sessionStorage.removeItem("fiolaser_token");
  sessionStorage.removeItem("fiolaser_nome");
  sessionStorage.removeItem("fiolaser_role");
}

async function logout() {
  try { if(getToken()) await apiFetch('/api/auth/logout',{method:'POST'}); }
  catch { /* Offline: only this tab can be cleared; the server token keeps its expiry. */ }
  finally { limparAcesso();window.location.href = "index.html"; }
}

async function apiFetch(path, options = {}) {
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  const token = getToken();
  if (token) headers["Authorization"] = `Bearer ${token}`;

  const res = await fetch(`${API_BASE}${path}`, { ...options, headers });
  const data = await res.json().catch(() => ({}));

  if (!res.ok) {
    if(res.status === 401) { limparAcesso(); throw new Error("Entre novamente para continuar."); }
    const msg = data.erro || Object.values(data)[0] || "Ocorreu um erro. Tente novamente.";
    throw new Error(msg);
  }
  return data;
}

const PAGINAS_NAV = [
  { chave: "inicio", href: "index.html", label: "Início" },
  { chave: "servicos", href: "servicos.html", label: "Tratamentos" },
  { chave: "quem-somos", href: "quem-somos.html", label: "A Fiolaser" },
  { chave: "agendamento", href: "agendamento.html", label: "Agendar" },
];

function montarHeader() {
  const el = document.getElementById("header-fiolaser");
  if (!el) return;

  const paginaAtual = document.body.dataset.page || "";

  const nav = PAGINAS_NAV.map(p => `
    <li><a href="${p.href}" ${p.chave === paginaAtual ? 'aria-current="page"' : ""}>${p.label}</a></li>
  `).join("");

  el.innerHTML = `
    <header class="topbar">
      <div class="topbar-inner">
        <a href="index.html" class="marca-fiolaser">
          <span class="marca-fiolaser-texto">fi&oslash;laser</span>
          <span class="marca-fiolaser-selo">SPA</span>
        </a>
        <ul class="nav">${nav}</ul>
        <div class="acoes-topo" id="acoes-topo"></div>
      </div>
    </header>
  `;

  atualizarAcoesTopo();
}

function atualizarAcoesTopo() {
  const nome = escapeHtml(sessionStorage.getItem("fiolaser_nome") || "");
  const role = sessionStorage.getItem("fiolaser_role");
  const acoes = document.getElementById("acoes-topo");
  if (!acoes) return;

  if (!isLogado()) {
    acoes.innerHTML = `<a class="btn btn-secundario" href="login.html">Entrar</a>
       <a class="btn btn-primario" href="cadastro.html">Criar conta</a>`;
    return;
  }

  acoes.innerHTML = role === "ADMIN"
    ? `<span style="font-size:0.9rem;color:var(--grafite)">Olá, ${nome ? nome.split(" ")[0] : "admin"}</span>
       <a class="btn btn-secundario" href="admin.html">Painel</a>
       <a class="btn btn-secundario" href="conta.html">Minha conta</a><button class="btn btn-primario" onclick="logout()">Sair</button>`
    : `<span style="font-size:0.9rem;color:var(--grafite)">Olá, ${nome ? nome.split(" ")[0] : "cliente"}</span>
       <a class="btn btn-secundario" href="agendamento.html">Agendar</a>
       <a class="btn btn-secundario" href="conta.html">Minha conta</a><button class="btn btn-primario" onclick="logout()">Sair</button>`;
}

function montarFooter() {
  const el = document.getElementById("footer-fiolaser");
  if (!el) return;
  el.innerHTML = `
    <footer class="footer-vermelho">
      <div class="container footer-vermelho-grid">
        <div class="footer-vermelho-marca">
          <a href="index.html" class="marca-fiolaser marca-clara">
            <span class="marca-fiolaser-texto">fi&oslash;laser</span>
            <span class="marca-fiolaser-selo">SPA</span>
          </a>
          <div class="footer-abf">
            <span class="footer-abf-icone">&#9673;</span>
            <span>Associação Brasileira<br>de Franchising</span>
          </div>
        </div>

        <div>
          <h4>A Fiolaser</h4>
          <ul>
            <li><a href="quem-somos.html">Institucional</a></li>
            <li><a href="quem-somos.html#manifesto">Manifesto Fiolaser</a></li>
            <li><a href="quem-somos.html#universo">Universo Fiolaser</a></li>
          </ul>
        </div>

        <div>
          <h4>Serviços</h4>
          <ul>
            <li><a href="servicos.html">Depilação a Laser</a></li>
            <li><a href="servicos.html">Tratamentos Estéticos</a></li>
          </ul>
        </div>

        <div>
          <h4>Atendimento</h4>
          <ul>
            <li><a href="https://instagram.com/fiolaser.cruzdasalmas" target="_blank" rel="noopener">Fale Conosco</a></li>
            <li><a href="perguntas-frequentes.html">Perguntas Frequentes</a></li>
            <li><a href="politica-privacidade.html">Política de Privacidade</a></li>
            <li><a href="agendamento.html">Agende uma avaliação</a></li>
          </ul>
        </div>

        <div class="footer-vermelho-cta">
          <a href="agendamento.html" class="btn" style="background:var(--branco); color:var(--vermelho-escuro);">fiolaser+</a>
        </div>
      </div>
      <div class="container footer-baixo footer-baixo-claro">
        <span>© ${new Date().getFullYear()} Fio Laser Cruz das Almas. Todos os direitos reservados.</span>
        <span>Projeto de desenvolvimento · plataforma de agendamento</span>
      </div>
    </footer>
  `;
}

// ---------- Carrossel do banner (home) ----------
function montarCarrossel() {
  const el = document.getElementById("hero-carrossel");
  if (!el) return;

  const slides = [
    {
      classe: "slide-a",
     foto: "img/banner-mais-autoestima.jpg",
      somenteImagem: true,
      cta: "agendamento.html",

    },
    {
      classe: "slide-b",
      foto: "img/banner-versao.jpg",
      somenteImagem: true,
      cta: "servicos.html"
    },
    {
      classe: "slide-c",
      foto: "img/banner-tecnologia-seguranca.jpg",
      somenteImagem: true,
      cta: "agendamento.html",

    },
    {
      classe: "slide-a",
      foto: "img/banner-transforme-rotina.jpg",
      somenteImagem: true,
      cta: "agendamento.html",

    },
  ];

  let atual = 0;

  function render() {
    const s = slides[atual];

    const conteudo = s.somenteImagem
      ? ""
      : `
        <div class="hero-slide-conteudo">
          <div class="hero-eyebrow" style="color:var(--branco);">${s.eyebrow}</div>
          <h1 style="color:var(--branco); font-size:clamp(1.8rem,3.4vw,2.6rem);">${s.titulo}</h1>
          <p style="color:#FFDADA; max-width:46ch;">${s.texto}</p>
          <a href="${s.cta}" class="btn" style="background:var(--branco); color:var(--vermelho-escuro); margin-top:8px;">${s.ctaTexto}</a>
        </div>
      `;

    const estiloFundo = s.foto
      ? `style="background-image:url('${s.foto}'); background-size:cover; background-position:center;"`
      : "";

    const tag = s.somenteImagem ? "a" : "div";
    const atributosExtra = s.somenteImagem ? `href="${s.cta}" aria-label="Ver procedimentos"` : "";
    const classeFoto = s.foto ? "hero-slide-foto" : "";

    el.innerHTML = `
      <${tag} class="hero-slide ${s.classe} ${classeFoto}" ${estiloFundo} ${atributosExtra}>
        ${conteudo}
      </${tag}>
      <button class="hero-seta hero-seta-esq" aria-label="Anterior">‹</button>
      <button class="hero-seta hero-seta-dir" aria-label="Próximo">›</button>
      <div class="hero-dots">
        ${slides.map((_, i) => `<span class="hero-dot ${i === atual ? "ativo" : ""}"></span>`).join("")}
      </div>
    `;
    el.querySelector(".hero-seta-esq").addEventListener("click", () => mudar(-1));
    el.querySelector(".hero-seta-dir").addEventListener("click", () => mudar(1));
  }

  function mudar(delta) {
    atual = (atual + delta + slides.length) % slides.length;
    render();
  }

  render();
  setInterval(() => mudar(1), 6000);
}

document.addEventListener("DOMContentLoaded", () => {
  montarHeader();
  montarFooter();
  montarCarrossel();
});

function escapeHtml(value) { return String(value).replace(/[&<>"']/g, c => ({ "&":"&amp;", "<":"&lt;", ">":"&gt;", "\"":"&quot;", "'":"&#39;" }[c])); }
function salvarAcesso(data) { sessionStorage.setItem("fiolaser_token",data.token);sessionStorage.setItem("fiolaser_nome",data.nomeCompleto);sessionStorage.setItem("fiolaser_role",data.role); }
