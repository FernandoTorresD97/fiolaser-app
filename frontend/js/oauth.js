(async () => {
 const code=new URLSearchParams(location.hash.slice(1)).get("code");
 history.replaceState(null,"",location.pathname);
 try {
  if(!code) throw new Error("Código de acesso ausente. Tente entrar novamente.");
  const data=await apiFetch("/api/auth/google/exchange",{method:"POST",body:JSON.stringify({code})});
  salvarAcesso(data);
  location.replace(data.perfilCompleto ? (data.role==="ADMIN" ? "admin.html" : "agendamento.html") : "completar-cadastro.html");
 } catch(e) { document.getElementById("oauth-status").textContent=e.message; }
})();