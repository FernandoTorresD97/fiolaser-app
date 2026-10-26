(async()=>{
 try {
  const user=await apiFetch("/api/auth/me");
  document.getElementById("account-description").textContent=user.nomeCompleto;
  if(user.provider==="GOOGLE") {document.getElementById("password-form").hidden=true;document.getElementById("account-status").textContent="Sua senha é gerenciada na conta Google.";}
 } catch(e) {document.getElementById("account-status").textContent=e.message;}
})();
document.getElementById("password-form").addEventListener("submit",async e=>{
 e.preventDefault();const message=document.getElementById("account-status"),button=e.target.querySelector("button");
 const password=document.getElementById("new-password").value;
 if(password!==document.getElementById("confirm-password").value) {message.textContent="As senhas não coincidem.";return;}
 button.disabled=true;
 try {
  salvarAcesso(await apiFetch("/api/auth/senha",{method:"PUT",body:JSON.stringify({senhaAtual:document.getElementById("current-password").value,novaSenha:password})}));
  e.target.reset();message.textContent="Senha alterada. Os acessos anteriores foram invalidados.";
 } catch(err) {message.textContent=err.message;} finally {button.disabled=false;}
});