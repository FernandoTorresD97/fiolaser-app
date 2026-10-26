(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.FiolaserDemo=factory();})(typeof window!=='undefined'?window:this,function(){
  'use strict';
  const KEY='fiolaser-portfolio-demo-v1';
  const localDate=d=>`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
  function create({catalog,storage,now=()=>new Date()}) {
    let memory;
    const clone=value=>JSON.parse(JSON.stringify(value));
    function seed(){
      const day=new Date(now());day.setDate(day.getDate()+1);
      return {version:1,users:[
        {id:'demo-admin',nomeCompleto:'Admin de demonstração',email:'admin@example.invalid',telefone:'5500000000001',role:'ADMIN',ativo:true,perfilCompleto:true,provider:'DEMO'},
        {id:'demo-ana',nomeCompleto:'Ana · cliente fictícia',email:'ana@example.invalid',telefone:'5500000000002',role:'CLIENTE',ativo:true,perfilCompleto:true,provider:'DEMO'},
        {id:'demo-lucas',nomeCompleto:'Lucas · cliente fictício',email:'lucas@example.invalid',telefone:'5500000000003',role:'CLIENTE',ativo:true,perfilCompleto:true,provider:'DEMO'}
      ],bookings:[{id:'demo-booking-inicial',userId:'demo-ana',serviceId:catalog[0].id,servicoNome:catalog[0].nome,
        horario:localDate(day)+'T10:00:00',duracaoMinutos:catalog[0].duracaoMinutos,valor:catalog[0].preco,formaPagamento:'PIX',status:'RESERVADO'}]};
    }
    function read(){
      let value;try{value=JSON.parse(storage?.getItem(KEY)||'null');}catch{}
      if(!value||value.version!==1||!Array.isArray(value.users)||!Array.isArray(value.bookings))value=memory||seed();
      memory=value;return value;
    }
    function write(value){memory=value;try{storage?.setItem(KEY,JSON.stringify(value));}catch{}return value;}
    function actor(data,token){const id=typeof token==='string'&&token.startsWith('demo:')?token.slice(5):null;
      const user=data.users.find(u=>u.id===id&&u.ativo);if(!user)throw Error('Escolha um perfil fictício em Entrar para testar.');return user;}
    function admin(data,token){const user=actor(data,token);if(user.role!=='ADMIN')throw Error('Use o perfil administrativo de demonstração.');return user;}
    function service(id){const s=catalog.find(s=>s.id===id);if(!s)throw Error('Procedimento não encontrado.');return s;}
    function dateTime(value){if(typeof value!=='string'||!/^\d{4}-\d{2}-\d{2}T\d{2}:[03]0(:00)?$/.test(value))throw Error('Escolha um horário na grade de 30 minutos.');
      const d=new Date(value);if(!Number.isFinite(d.getTime())||localDate(d)!==value.slice(0,10))throw Error('Data inválida.');return d;}
    function available(data,id,when,skip){const s=service(id),start=dateTime(when),finish=new Date(start.getTime()+s.duracaoMinutos*60000);
      if(start<now()||start.getHours()<9||finish.getHours()>19||(finish.getHours()===19&&finish.getMinutes()>0)||localDate(start)!==localDate(finish))return false;
      return !data.bookings.some(b=>b.id!==skip&&b.serviceId===id&&['RESERVADO','CONFIRMADO'].includes(b.status)&&
        new Date(b.horario)<finish&&new Date(new Date(b.horario).getTime()+b.duracaoMinutos*60000)>start);
    }
    function booking(data,input,owner,id){const s=service(input.serviceId);
      if(!['PIX','CARTAO','DINHEIRO'].includes(input.formaPagamento))throw Error('Forma de pagamento inválida.');
      if(!available(data,s.id,input.horario,id))throw Error('Horário indisponível ou fora do expediente (09h–19h).');
      if(data.bookings.length>=150&&!id)throw Error('Limite da demonstração atingido. Reinicie os exemplos.');
      return {id:id||('demo-'+Math.random().toString(36).slice(2)+Date.now().toString(36)),userId:owner.id,serviceId:s.id,servicoNome:s.nome,
        horario:input.horario.length===16?input.horario+':00':input.horario,duracaoMinutos:s.duracaoMinutos,valor:s.preco,formaPagamento:input.formaPagamento,status:'RESERVADO'};
    }
    function response(data,b){const u=data.users.find(u=>u.id===b.userId);return {...b,clienteNome:u.nomeCompleto,clienteEmail:u.email,clienteTelefone:u.telefone};}
    function auth(user){return {token:'demo:'+user.id,nomeCompleto:user.nomeCompleto,role:user.role,perfilCompleto:true};}
    function handle(path,options={},token){const data=read(),url=new URL(path,'https://demo.invalid'),method=options.method||'GET';
      const input=typeof options.body==='string'?JSON.parse(options.body):options.body||{};
      if(url.pathname==='/api/services'&&method==='GET')return clone(catalog);
      if(url.pathname==='/api/demo/login'&&method==='POST'){const u=data.users.find(u=>u.id===input.userId&&u.ativo);if(!u)throw Error('Perfil fictício indisponível. Reinicie os exemplos.');return auth(u);}
      if(url.pathname==='/api/auth/me')return clone(actor(data,token));
      if(url.pathname==='/api/auth/logout')return null;
      if(url.pathname==='/api/payments/capabilities')return {online:false,demo:true};
      if(url.pathname.startsWith('/api/bookings/availability/')&&method==='GET'){
        const id=url.pathname.split('/').pop(),day=url.searchParams.get('dia');service(id);
        if(!/^\d{4}-\d{2}-\d{2}$/.test(day||''))throw Error('Data inválida.');
        return Array.from({length:20},(_,i)=>{const min=9*60+i*30,horario=day+'T'+String(Math.floor(min/60)).padStart(2,'0')+':'+String(min%60).padStart(2,'0')+':00';
          return {horario,disponivel:available(data,id,horario)};});
      }
      if(url.pathname==='/api/bookings'&&method==='POST'){const user=actor(data,token),b=booking(data,input,user);data.bookings.push(b);write(data);return clone(b);}
      if(url.pathname==='/api/bookings/me'&&method==='GET'){const user=actor(data,token);return clone(data.bookings.filter(b=>b.userId===user.id).map(b=>response(data,b)));}
      if(url.pathname.startsWith('/api/bookings/')&&method==='DELETE'){
        const user=actor(data,token),b=data.bookings.find(b=>b.id===url.pathname.split('/').pop()&&b.userId===user.id);
        if(!b)throw Error('Reserva não encontrada.');b.status='CANCELADO';write(data);return null;
      }
      if(url.pathname.startsWith('/api/admin/')){
        const current=admin(data,token);
        if(url.pathname==='/api/admin/users'&&method==='GET'){const q=(url.searchParams.get('q')||'').toLowerCase();return clone(data.users.filter(u=>(u.nomeCompleto+' '+u.email).toLowerCase().includes(q)));}
        if(url.pathname.startsWith('/api/admin/users/')&&method==='PUT'){
          const u=data.users.find(u=>u.id===url.pathname.split('/').pop());if(!u)throw Error('Perfil não encontrado.');
          if(!['ADMIN','CLIENTE'].includes(input.role)||typeof input.ativo!=='boolean')throw Error('Perfil inválido.');
          if(u.id===current.id&&(input.role!=='ADMIN'||!input.ativo))throw Error('Você não pode retirar seu próprio acesso administrativo.');
          if(u.role==='ADMIN'&&u.ativo&&(input.role!=='ADMIN'||!input.ativo)&&data.users.filter(u=>u.role==='ADMIN'&&u.ativo).length<=1)throw Error('Mantenha um administrador ativo.');
          u.role=input.role;u.ativo=input.ativo;write(data);return clone(u);
        }
        if((url.pathname==='/api/admin/bookings'||url.pathname==='/api/admin/bookings/export')&&method==='GET'){
          const begin=url.searchParams.get('inicio'),end=url.searchParams.get('fim');
          if(!begin||!end||end<begin)throw Error('Informe um período válido.');
          const rows=data.bookings.filter(b=>b.horario.slice(0,10)>=begin&&b.horario.slice(0,10)<=end).map(b=>response(data,b));
          if(url.pathname.endsWith('/export')){const cell=v=>'"'+String(v??'').replace(/^[=+@-]/,"'$&").replace(/"/g,'""')+'"';
            return '\uFEFF'+[['Cliente fictício','Procedimento','Horário','Status','Valor ilustrativo'],...rows.map(b=>[b.clienteNome,b.servicoNome,b.horario,b.status,b.valor])].map(r=>r.map(cell).join(';')).join('\r\n');}
          return clone(rows);
        }
        if(url.pathname==='/api/admin/bookings'&&method==='POST'){const owner=data.users.find(u=>u.id===input.userId&&u.ativo);if(!owner)throw Error('Cliente fictício indisponível.');
          const b=booking(data,input,owner);data.bookings.push(b);write(data);return clone(b);}
        if(url.pathname.startsWith('/api/admin/bookings/')){
          const b=data.bookings.find(b=>b.id===url.pathname.split('/').pop());if(!b)throw Error('Reserva não encontrada.');
          if(method==='DELETE'){b.status='CANCELADO';write(data);return null;}
          if(method==='PUT'){if(b.status==='CANCELADO')throw Error('Uma reserva cancelada não pode ser editada.');
            const owner=data.users.find(u=>u.id===input.userId&&u.ativo);if(!owner)throw Error('Cliente fictício indisponível.');
            Object.assign(b,booking(data,input,owner,b.id));write(data);return clone(b);}
        }
      }
      throw Error('Esta função não faz parte da demonstração. Nenhuma API externa foi chamada.');
    }
    return {handle,reset:()=>write(seed())};
  }
  return {create};
});
