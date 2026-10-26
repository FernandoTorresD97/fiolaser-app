const fs=require("fs"),path=require("path"),vm=require("vm"),assert=require("assert");
const root=path.resolve(__dirname,"../frontend");
let scripts=0,pages=0;
for(const name of fs.readdirSync(root).filter(f=>f.endsWith(".html"))){
 const html=fs.readFileSync(path.join(root,name),"utf8"); pages++;
 for(const image of html.matchAll(/<img\b[^>]*\bsrc="([^"]+)"[^>]*>/g)) {
  assert(fs.existsSync(path.join(root,image[1])),"Imagem ausente: "+image[1]);
  assert(/\balt="[^"]+"/.test(image[0]),"Imagem sem descrição: "+image[1]);
 }
 const ids=[...html.matchAll(/\bid="([^"]+)"/g)].map(m=>m[1]);assert.equal(new Set(ids).size,ids.length,"IDs repetidos: "+name);
 for(const match of html.matchAll(/<script([^>]*)>([\s\S]*?)<\/script>/g)){
  const src=match[1].match(/\bsrc="([^"]+)"/);
  if(src){assert(fs.existsSync(path.join(root,src[1])),"Script ausente: "+src[1]);}
  else {new vm.Script(match[2],{filename:name});scripts++;}
 }
}
for(const name of fs.readdirSync(path.join(root,"js")).filter(f=>f.endsWith(".js"))){
 new vm.Script(fs.readFileSync(path.join(root,"js",name),"utf8"),{filename:name});scripts++;
}
const context=vm.createContext({window:{API_BASE:""},document:{addEventListener(){}},sessionStorage:{getItem(){return null;}}});
vm.runInContext(fs.readFileSync(path.join(root,"js/main.js"),"utf8"),context);
assert.equal(context.escapeHtml('<img src=x onerror="x">'),"&lt;img src=x onerror=&quot;x&quot;&gt;");
console.log(pages+" páginas e "+scripts+" scripts validados; escape de HTML verificado.");
