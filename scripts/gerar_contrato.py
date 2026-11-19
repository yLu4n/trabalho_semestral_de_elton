"""Mantém um contrato OpenAPI e uma coleção executável sem dados pessoais."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
S={'type':'string'}
def obj(properties,required=None):return {'type':'object','properties':properties,'required':required or list(properties),'additionalProperties':False}
def ref(name):return {'$ref':'#/components/schemas/'+name}
schemas={
 'Mensagem':obj({'mensagem':S}),
 'Cadastro':obj({'nome':{'type':'string','maxLength':100},'email':{'type':'string','format':'email'},'senha':{'type':'string','minLength':8,'maxLength':72},'nascimento':{'type':'string','format':'date'},'aceitaPrivacidade':{'type':'boolean','enum':[True],'description':'Aceite explícito dos termos de uso e da política de privacidade.'}}),
 'Login':obj({'email':{'type':'string','format':'email'},'senha':S}),
 'CategoriaEntrada':obj({'nome':{'type':'string','maxLength':80},'tipo':{'type':'string','enum':['RECEITA','DESPESA']},'cor':{'type':'string','pattern':'^#[0-9a-fA-F]{6}$'},'ativa':{'type':'boolean'}}),
 'LancamentoEntrada':obj({'tipo':{'type':'string','enum':['RECEITA','DESPESA']},'valor':{'type':'string','pattern':r'^\d{1,7}(\.\d{1,2})?$','description':'Decimal exato entre 0.01 e 1000000.00. Nunca calculado em ponto flutuante.'},'data':{'type':'string','format':'date'},'categoriaId':{'type':'string','format':'uuid'},'descricao':{'type':'string','minLength':1,'maxLength':280},'estabelecimento':{'type':'string','maxLength':120},'formaPagamento':{'type':'string','maxLength':60}},['tipo','valor','data','categoriaId','descricao']),
 'MetaEntrada':obj({'categoriaId':{'type':'string','format':'uuid'},'mes':{'type':'string','pattern':r'^\d{4}-\d{2}$'},'limite':S}),
 'Confirmacao':obj({'confirmado':{'type':'boolean','enum':[True]},'lancamento':ref('LancamentoEntrada')}),
 'TextoEntrada':obj({'texto':{'type':'string','minLength':1,'maxLength':280}}),
 'Senha':obj({'senha':S}),
 'Sugestao':obj({'id':{'type':['string','null'],'format':'uuid'},'campos':{'type':'object','description':'Campos com confiança inferior a 0,60 retornam null.'},'confianca':{'type':'object','additionalProperties':{'type':'string'}},'pendentes':{'type':'array','items':S},'conferirValor':{'type':'boolean'},'manual':{'type':'boolean'},'aviso':S}),
 'Totais':obj({'receitas':S,'despesas':S,'saldo':S}),
 'Lancamento':{'allOf':[ref('LancamentoEntrada'),{'type':'object','properties':{'id':{'type':'string','format':'uuid'},'origem':{'type':'string','enum':['IA','MANUAL']},'excluidoEm':{'type':['string','null'],'format':'date-time'}}}]},
 'Pagina':obj({'itens':{'type':'array','items':ref('Lancamento')},'pagina':{'type':'integer'},'paginas':{'type':'integer'},'quantidade':{'type':'integer'},'totais':ref('Totais')})
}
schemas['Lancamento']=obj({
    **schemas['LancamentoEntrada']['properties'],
    'id':{'type':'string','format':'uuid'},'usuarioId':{'type':'string','format':'uuid'},
    'origem':{'type':'string','enum':['IA','MANUAL']},
    'excluidoEm':{'type':['string','null'],'format':'date-time'},
    'criadoEm':{'type':'string','format':'date-time'},'versao':{'type':'integer'},
    'estabelecimento':{'type':['string','null'],'maxLength':120},
    'formaPagamento':{'type':['string','null'],'maxLength':60}
})
paths={}
def add(method,path,summary,body=None,response=None,query=(),public=False):
    operation={'summary':summary,'operationId':method+'_'+path.replace('/','_').replace('{','').replace('}',''),'responses':{'200':{'description':'Operação concluída'},'400':{'description':'Entrada inválida'},'401':{'description':'Sessão inválida ou expirada'},'403':{'description':'Papel não autorizado ou CSRF inválido'},'404':{'description':'Recurso inexistente ou de outro titular'},'422':{'description':'Regra de negócio violada'},'429':{'description':'Cota da IA excedida'},'503':{'description':'Serviço externo indisponível'}}}
    if body:operation['requestBody']={'required':True,'content':{'application/json':{'schema':ref(body) if isinstance(body,str) else body}}}
    if response:operation['responses']['200']['content']={'application/json':{'schema':ref(response)}}
    params=[]
    if '{id}' in path:params.append({'in':'path','name':'id','required':True,'schema':{'type':'string','format':'uuid'}})
    for name in query:params.append({'in':'query','name':name,'required':name=='mes','schema':{'type':'integer'} if name in ('pagina','tamanho') else S})
    if method not in ('get','head'):params.append({'in':'header','name':'X-XSRF-TOKEN','required':True,'schema':S,'description':'Valor atual do cookie XSRF-TOKEN obtido em GET /auth/csrf.'})
    if params:operation['parameters']=params
    if public:operation['security']=[]
    paths.setdefault('/api'+path,{})[method]=operation
add('get','/auth/csrf','Obter token CSRF',public=True)
add('post','/auth/cadastro','Criar conta e enviar verificação','Cadastro','Mensagem',public=True)
paths['/api/auth/cadastro']['post']['responses']['409']={'description':'E-mail já cadastrado'}
add('post','/auth/verificar','Verificar e-mail',obj({'token':S}),'Mensagem',public=True)
add('post','/auth/recuperar','Solicitar vínculo de recuperação',obj({'email':{'type':'string','format':'email'}}),'Mensagem',public=True)
add('post','/auth/redefinir','Redefinir senha com vínculo de uso único',obj({'token':S,'senha':{'type':'string','minLength':8,'maxLength':72}}),'Mensagem',public=True)
add('post','/auth/login','Autenticar com e-mail e senha','Login','Mensagem',public=True)
add('get','/auth/eu','Consultar perfil autenticado')
add('post','/auth/logout','Revogar sessão')
add('get','/categorias','Listar categorias disponíveis')
add('post','/categorias','Criar categoria pessoal','CategoriaEntrada')
add('put','/categorias/{id}','Editar ou desativar categoria pessoal','CategoriaEntrada')
add('get','/lancamentos','Filtrar e paginar lançamentos ativos',response='Pagina',query=('inicio','fim','categoriaId','tipo','texto','pagina','tamanho'))
add('post','/lancamentos','Cadastrar lançamento manual','LancamentoEntrada','Lancamento')
add('put','/lancamentos/{id}','Editar lançamento','LancamentoEntrada','Lancamento')
add('delete','/lancamentos/{id}','Excluir logicamente por 90 dias')
add('post','/lancamentos/{id}/restaurar','Restaurar lançamento excluído')
add('get','/lixeira','Listar lançamentos excluídos do titular')
add('get','/painel','Consolidar totais, distribuição, série e projeção',query=('inicio','fim'))
add('get','/metas','Consultar metas e consumo mensal',query=('mes',))
add('post','/metas','Criar ou atualizar meta','MetaEntrada')
add('delete','/metas/{id}','Remover meta')
add('post','/ia/interpretacoes','Interpretar frase sem persistir lançamento','TextoEntrada','Sugestao')
add('post','/ia/sugestoes/{id}/confirmar','Confirmar sugestão uma única vez','Confirmacao','Lancamento')
add('delete','/ia/sugestoes/{id}','Descartar sugestão')
add('post','/ia/diagnosticos','Gerar diagnóstico com agregados e aliases',query=('mes',))
add('get','/exportacao','Baixar extrato completo em CSV UTF-8')
paths['/api/exportacao']['get']['responses']['200']['content']={'text/csv':{'schema':S}}
add('post','/privacidade/exclusao','Reautenticar, revogar sessões e solicitar eliminação','Senha')
add('get','/privacidade/solicitacoes','Acompanhar solicitações do titular')
add('post','/privacidade/solicitacoes','Registrar solicitação ao DPO',obj({'tipo':{'type':'string','enum':['ACESSO','CORRECAO','INFORMACOES']}}))
for path in ['/admin/metricas','/admin/contas','/admin/categorias','/dpo/auditoria','/dpo/solicitacoes']:add('get',path,'Operação restrita ao papel '+('ADMIN' if '/admin' in path else 'DPO'))
add('post','/admin/categorias','Criar categoria padrão (ADMIN)','CategoriaEntrada')
add('put','/admin/categorias/{id}','Editar categoria padrão (ADMIN)','CategoriaEntrada')
add('put','/admin/contas/{id}/bloqueio','Bloquear ou liberar titular (ADMIN)',obj({'bloqueado':{'type':'boolean'}}))
add('put','/dpo/solicitacoes/{id}','Atualizar atendimento (DPO)',obj({'estado':{'type':'string','enum':['ABERTA','EM_ATENDIMENTO','CONCLUIDA']}}))
spec={'openapi':'3.1.0','info':{'title':'FinAI API','version':'1.0.0','description':'MVC. Moedas são strings decimais. Sessão em cookie HttpOnly assinado; 30 min de inatividade/12 h totais. Requer CSRF nas mutações.'},'servers':[{'url':'http://localhost:8080'}],'security':[{'sessao':[]}],'paths':paths,'components':{'securitySchemes':{'sessao':{'type':'apiKey','in':'cookie','name':'FINAI_SESSION'}},'schemas':schemas}}
(ROOT/'docs/openapi.json').write_text(json.dumps(spec,ensure_ascii=False,indent=2),encoding='utf-8')
items=[]
for name,url in [('Obter CSRF','/api/auth/csrf'),('Perfil','/api/auth/eu'),('Categorias','/api/categorias'),('Painel','/api/painel'),('Lançamentos','/api/lancamentos'),('Metas','/api/metas?mes={{mes}}')]:
    item={'name':name,'request':{'method':'GET','url':'{{baseUrl}}'+url},'event':[{'listen':'test','script':{'type':'text/javascript','exec':['pm.test("Resposta válida", () => pm.response.to.have.status(200));']}}]}
    if name=='Obter CSRF':item['event'][0]['script']['exec'].append('pm.collectionVariables.set("csrf", pm.response.json().token);')
    items.append(item)
items.insert(1,{'name':'Login','request':{'method':'POST','header':[{'key':'Content-Type','value':'application/json'},{'key':'X-XSRF-TOKEN','value':'{{csrf}}'}],'url':'{{baseUrl}}/api/auth/login','body':{'mode':'raw','raw':json.dumps({'email':'{{email}}','senha':'{{senha}}'})}},'event':[{'listen':'test','script':{'type':'text/javascript','exec':['pm.test("Login confirmado", () => pm.response.to.have.status(200));']}}]})
items.insert(2,items[0].copy())
collection={'info':{'name':'FinAI · verificação local','schema':'https://schema.getpostman.com/json/collection/v2.1.0/collection.json'},'variable':[{'key':'baseUrl','value':'http://127.0.0.1:8080'},{'key':'mes','value':'2026-11'},{'key':'email','value':''},{'key':'senha','value':''}],'item':items}
(ROOT/'postman/FinAI.postman_collection.json').write_text(json.dumps(collection,ensure_ascii=False,indent=2),encoding='utf-8')
print('Contrato OpenAPI e coleção Postman atualizados.')
