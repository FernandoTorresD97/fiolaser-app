if(!isLogado()) location.replace("login.html");
document.getElementById("profile-form").addEventListener("submit",async e=>{
 e.preventDefault();const button=e.target.querySelector("button");button.disabled=true;
 try {
  const resp=await apiFetch("/api/auth/perfil",{method:"PUT",body:JSON.stringify({
   cpf:document.getElementById("cpf").value,dataNascimento:document.getElementById("birth").value,telefone:document.getElementById("phone").value||null})});
  salvarAcesso(resp);location.replace(resp.role==="ADMIN" ? "admin.html" : "agendamento.html");
 } catch(err) {document.getElementById("profile-status").textContent=err.message;} finally {button.disabled=false;}
});