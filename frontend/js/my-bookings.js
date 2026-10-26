async function loadBookings(){
 try {
  const bookings=await apiFetch("/api/bookings/me"),container=document.getElementById("my-bookings");container.replaceChildren();
  document.getElementById("bookings-status").textContent=bookings.length?"":"Você ainda não tem agendamentos.";
  bookings.forEach(b=>{
   const card=document.createElement("article");card.className="admin-editor";
   const text=document.createElement("p");text.textContent=b.servicoNome+" — "+b.horario.replace("T"," ").slice(0,16)+" (Bahia) — "+b.status;card.append(text);
   if(["RESERVADO","CONFIRMADO"].includes(b.status)){
    const button=document.createElement("button");button.textContent="Cancelar reserva";button.className="btn btn-secundario";
    button.onclick=async()=>{if(!confirm("Cancelar esta reserva?"))return;button.disabled=true;try{await apiFetch("/api/bookings/"+encodeURIComponent(b.id),{method:"DELETE"});await loadBookings();}catch(e){document.getElementById("bookings-status").textContent=e.message;button.disabled=false;}};card.append(button);
   }
   container.append(card);
  });
 } catch(e){document.getElementById("bookings-status").textContent=e.message;}
}loadBookings();