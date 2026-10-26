const byId=id=>document.getElementById(id);
let bookings=[], services=[];
const localDate=d=>[d.getFullYear(),String(d.getMonth()+1).padStart(2,"0"),String(d.getDate()).padStart(2,"0")].join("-");
const today=new Date();
byId("period-start").value=localDate(new Date(today.getFullYear(),today.getMonth(),1));
byId("period-end").value=localDate(new Date(today.getFullYear(),today.getMonth()+1,0));
function queryPeriod() {
 const inicio=byId("period-start").value,fim=byId("period-end").value;
 if(!inicio||!fim||fim<inicio) throw new Error("Informe um período válido.");
 return new URLSearchParams({inicio,fim}).toString();
}
function notice(e) {byId("admin-status").textContent=e.message||e;}
async function refresh() {
 try {
  bookings=await apiFetch("/api/admin/bookings?"+queryPeriod());
  byId("bookings-body").replaceChildren();
  bookings.forEach(b=>{
   const row=document.createElement("tr");
   [b.clienteNome,b.clienteEmail||b.clienteTelefone||"—",b.servicoNome,b.horario.replace("T"," ").slice(0,16),
    Number(b.valor).toLocaleString("pt-BR",{style:"currency",currency:"BRL"}),b.formaPagamento,b.status].forEach(value=>{
    const cell=document.createElement("td");cell.textContent=value;row.append(cell);
   });
   const actions=document.createElement("td");
   if(["RESERVADO","CONFIRMADO"].includes(b.status)) {
    const edit=document.createElement("button");edit.className="btn btn-secundario";edit.textContent="Editar";edit.onclick=()=>openEditor(b);
    const cancel=document.createElement("button");cancel.className="btn-cancelar-admin";cancel.textContent="Cancelar";cancel.onclick=async()=>{
     if(!confirm("Cancelar esta reserva?")) return;cancel.disabled=true;
     try {await apiFetch("/api/admin/bookings/"+encodeURIComponent(b.id),{method:"DELETE"});await refresh();} catch(e) {notice(e);cancel.disabled=false;}
    };
    actions.append(edit,cancel);
   }
   row.append(actions);byId("bookings-body").append(row);
  });
  byId("admin-summary").textContent=bookings.length+" reserva(s). Valores contratados não representam pagamentos recebidos.";
  notice("");
 } catch(e) {notice(e);}
}
async function clients(selected) {
 const data=await apiFetch("/api/admin/users?q="+encodeURIComponent(byId("client-search").value));
 const select=byId("client");select.replaceChildren(new Option("Selecione um cliente",""));
 data.filter(u=>u.ativo&&u.perfilCompleto).forEach(u=>select.add(new Option(u.nomeCompleto+" — "+(u.email||u.telefone||""),u.id)));
 if(selected && ![...select.options].some(o=>o.value===selected.userId)) select.add(new Option(selected.clienteNome,selected.userId));
 if(selected) select.value=selected.userId;
}
async function openEditor(b) {
 try {
  byId("booking-form").reset();byId("booking-id").value=b?.id||"";byId("editor-title").textContent=b?"Editar agendamento":"Novo agendamento";
  byId("editor-status").textContent="";await clients(b);
  byId("service").replaceChildren(...services.map(s=>new Option(s.nome,s.id)));
  if(b) {byId("service").value=b.serviceId;byId("booking-time").value=b.horario.slice(0,16);byId("payment").value=b.formaPagamento;}
  byId("booking-form").hidden=false;byId("booking-form").scrollIntoView({behavior:"smooth",block:"center"});
 } catch(e) {notice(e);}
}
byId("booking-form").onsubmit=async e=>{
 e.preventDefault();e.submitter.disabled=true;
 try {
  const id=byId("booking-id").value;
  await apiFetch("/api/admin/bookings"+(id?"/"+encodeURIComponent(id):""),{method:id?"PUT":"POST",body:JSON.stringify({
   userId:byId("client").value,serviceId:byId("service").value,horario:byId("booking-time").value+":00",formaPagamento:byId("payment").value})});
  byId("booking-form").hidden=true;await refresh();
 } catch(err) {byId("editor-status").textContent=err.message;} finally {e.submitter.disabled=false;}
};
async function loadUsers() {
 try {
  const users=await apiFetch("/api/admin/users?q="+encodeURIComponent(byId("user-search").value));
  byId("users-body").replaceChildren();
  for(const u of users) {
   const row=document.createElement("tr");
   [u.nomeCompleto,u.email||"—"].forEach(value=>{const td=document.createElement("td");td.textContent=value;row.append(td);});
   const role=document.createElement("select");role.setAttribute("aria-label","Perfil de "+u.nomeCompleto);
   role.add(new Option("Cliente","CLIENTE"));role.add(new Option("Administrador","ADMIN"));role.value=u.role;
   const active=document.createElement("input");active.type="checkbox";active.checked=u.ativo;active.setAttribute("aria-label","Conta ativa de "+u.nomeCompleto);
   const save=document.createElement("button");save.textContent="Salvar";save.className="btn btn-secundario";
   save.onclick=async()=>{
    if(role.value==="ADMIN"&&u.role!=="ADMIN"&&!confirm("Conceder acesso administrativo a "+u.nomeCompleto+"?"))return;
    save.disabled=true;
    try {await apiFetch("/api/admin/users/"+encodeURIComponent(u.id),{method:"PUT",body:JSON.stringify({role:role.value,ativo:active.checked})});await loadUsers();notice("Acesso atualizado.");}
    catch(e){notice(e);save.disabled=false;}
   };
   [role,active,save].forEach(el=>{const td=document.createElement("td");td.append(el);row.append(td);});byId("users-body").append(row);
  }
 } catch(e) {notice(e);}
}
byId("export").onclick=async()=>{
 try {
  const res=await fetch(API_BASE+"/api/admin/bookings/export?"+queryPeriod(),{headers:{Authorization:"Bearer "+getToken()}});
  if(!res.ok) throw new Error("Não foi possível exportar o relatório. Verifique o período e seu acesso.");
  const url=URL.createObjectURL(await res.blob()),link=document.createElement("a");link.href=url;link.download="agendamentos.csv";link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
 } catch(e) {notice(e);}
};
byId("refresh").onclick=refresh;byId("new-booking").onclick=()=>openEditor(null);byId("close-editor").onclick=()=>byId("booking-form").hidden=true;
byId("search-users").onclick=loadUsers;byId("search-clients").onclick=()=>clients().catch(notice);
(async()=>{
 try {
  const me=await apiFetch("/api/auth/me");if(me.role!=="ADMIN")throw new Error("Acesso exclusivo para administradores.");
  services=await apiFetch("/api/services");byId("admin-content").hidden=false;await refresh();await loadUsers();
 } catch(e) {notice(e);}
})();