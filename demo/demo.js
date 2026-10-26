const fiolaserDemoApp=FiolaserDemo.create({catalog:window.FIOLASER_CATALOG,storage:localStorage});
window.FIOLASER_DEMO_API=async(path,options)=>fiolaserDemoApp.handle(path,options,sessionStorage.getItem('fiolaser_demo_token'));
window.entrarDemo=async userId=>{
  try {const access=await window.FIOLASER_DEMO_API('/api/demo/login',{method:'POST',body:{userId}});salvarAcesso(access);location.href=userId==='demo-admin'?'admin.html':'agendamento.html';}
  catch(e){document.getElementById('demo-login-status').textContent=e.message;}
};
document.addEventListener('DOMContentLoaded',()=>{
  const banner=document.createElement('aside');banner.className='demo-banner';banner.setAttribute('aria-label','Ambiente de demonstração');
  banner.innerHTML='<div><strong>Demonstração · dados fictícios</strong><span>Reservas só neste navegador. Sem cadastro real, cobranças, Google, Kafka ou envio de mensagens.</span></div><nav><a href="../../#projetos">Portfólio ↗</a><a href="login.html">Trocar perfil</a><button type="button" id="reset-demo">Reiniciar demo</button></nav>';
  document.body.prepend(banner);
  document.getElementById('reset-demo').onclick=()=>{if(!confirm('Restaurar somente os exemplos fictícios desta demonstração?'))return;fiolaserDemoApp.reset();limparAcesso();location.href='index.html';};
});
