"""Teste HTTP completo contra o backend real, PostgreSQL e simulador Postman local.
Não utiliza credenciais reais do provedor. Execute com backend-simulation ativo.
"""
import urllib.request, urllib.error, http.cookiejar, json, uuid, re, time
from pathlib import Path
from decimal import Decimal
from dev import ROOT, RUNTIME
BASE='http://127.0.0.1:8080/api'
class Cliente:
    def __init__(self):
        self.jar=http.cookiejar.CookieJar();self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar));self.csrf=''
        self.call('GET','/auth/csrf');self.csrf=next(c.value for c in self.jar if c.name=='XSRF-TOKEN')
    def call(self,method,path,body=None,status=200):
        self.csrf=next((c.value for c in self.jar if c.name=='XSRF-TOKEN'),self.csrf)
        headers={'Content-Type':'application/json','X-XSRF-TOKEN':self.csrf}
        request=urllib.request.Request(BASE+path,data=json.dumps(body).encode() if body is not None else None,headers=headers,method=method)
        try:
            with self.opener.open(request,timeout=90) as response: code=response.status;data=response.read()
        except urllib.error.HTTPError as e: code=e.code;data=e.read()
        assert code==status,(method,path,code,data.decode())
        try:return json.loads(data) if data else None
        except ValueError:return data.decode('utf-8')
    def cadastrar(self,nome):
        self.email=f'teste-{uuid.uuid4().hex[:12]}@finai.example';self.senha='TesteSeguro123!'
        self.call('POST','/auth/cadastro',{'nome':nome,'email':self.email,'senha':self.senha,'nascimento':'1995-01-01','aceitaPrivacidade':True})
        arquivos=list((ROOT/'backend/var/mail').glob('*.eml'))
        texto=next(p.read_text(encoding='utf-8') for p in arquivos if self.email in p.read_text(encoding='utf-8'))
        token=re.search(r'token=([a-zA-Z0-9-]+)',texto)[1]
        self.call('POST','/auth/verificar',{'token':token})
        self.call('POST','/auth/login',{'email':self.email,'senha':self.senha})
        self.call('GET','/auth/csrf')
        self.perfil=self.call('GET','/auth/eu')
def modo(nome):
    with urllib.request.urlopen('http://127.0.0.1:4010/llm/controle/'+nome,timeout=10) as r: assert r.status==200
if __name__=='__main__':
    for tentativa in range(90):
        try:
            with urllib.request.urlopen(BASE+'/auth/csrf',timeout=2) as r:
                if r.status==200:break
        except (urllib.error.URLError,TimeoutError):pass
        time.sleep(1)
    else:raise SystemExit('O backend não ficou disponível. Inicie backend-simulation e confira o log antes de repetir.')
    checks=[]
    def ok(texto):checks.append(texto);print('OK',texto,flush=True)
    a=Cliente();a.cadastrar('Titular de teste A');b=Cliente();b.cadastrar('Titular de teste B');ok('Cadastro, e-mail verificado e login de duas contas')
    categorias=a.call('GET','/categorias');categoria=next(c for c in categorias if c['tipo']=='DESPESA')
    import datetime
    hoje=datetime.date.today().isoformat();mes=hoje[:7]
    entrada={'tipo':'DESPESA','valor':'0.10','data':hoje,'categoriaId':categoria['id'],'descricao':'Café de teste','estabelecimento':'','formaPagamento':'Pix'}
    l=a.call('POST','/lancamentos',entrada);a.call('POST','/lancamentos',{**entrada,'valor':'0.20'})
    p=a.call('GET','/painel');assert Decimal(p['totais']['despesas'])==Decimal('0.30');ok('Persistência real e soma decimal exata: 0,10 + 0,20 = 0,30')
    b.call('PUT','/lancamentos/'+l['id'],entrada,status=404);b.call('DELETE','/lancamentos/'+l['id'],status=404);assert not b.call('GET','/lancamentos')['itens'];ok('Isolamento horizontal entre titulares')
    a.call('DELETE','/lancamentos/'+l['id']);assert Decimal(a.call('GET','/painel')['totais']['despesas'])==Decimal('0.20')
    a.call('POST','/lancamentos/'+l['id']+'/restaurar');ok('Exclusão lógica retira totais; restauração devolve o lançamento')
    meta=a.call('POST','/metas',{'categoriaId':categoria['id'],'mes':mes,'limite':'0.30'});assert a.call('GET','/metas?mes='+mes)[0]['estado']=='ATENCAO'
    a.call('POST','/metas',{'categoriaId':categoria['id'],'mes':mes,'limite':'0.20'});assert a.call('GET','/metas?mes='+mes)[0]['estado']=='CRITICO';ok('Metas em atenção aos 100% e críticas acima de 100%')
    a.call('POST','/lancamentos',{**entrada,'valor':'0.001'},status=400);a.call('POST','/lancamentos',{**entrada,'valor':'1000000.01'},status=400);ok('Validação rejeita precisão e valor fora dos limites')
    antes=a.call('GET','/lancamentos')['quantidade'];modo('valido')
    s=a.call('POST','/ia/interpretacoes',{'texto':'Almocei 38 reais hoje via Pix'});assert not s['manual'],s
    assert a.call('GET','/lancamentos')['quantidade']==antes
    confirmado={**entrada,**s['campos']};confirmado={k:(v if v is not None else '') for k,v in confirmado.items()}
    a.call('POST','/ia/sugestoes/'+s['id']+'/confirmar',{'confirmado':False,'lancamento':confirmado},status=400)
    a.call('POST','/ia/sugestoes/'+s['id']+'/confirmar',{'confirmado':True,'lancamento':confirmado})
    a.call('POST','/ia/sugestoes/'+s['id']+'/confirmar',{'confirmado':True,'lancamento':confirmado},status=404)
    ok('Adaptador HTTP real, confirmação obrigatória e bloqueio de confirmação duplicada')
    modo('baixa-confianca');s=a.call('POST','/ia/interpretacoes',{'texto':'Talvez um almoço de 38 hoje'});assert s['campos']['valor'] is None and s['campos']['data'] is None and s['conferirValor'];ok('Baixa confiança deixa valor e data em branco e emite alerta')
    a.call('DELETE','/ia/sugestoes/'+s['id'])
    for falha in ('invalido','erro','cota'):
        modo(falha);s=a.call('POST','/ia/interpretacoes',{'texto':'Almoço 38'});assert s['manual'] and s['campos']=={'descricao':'Almoço 38'}
    ok('Categoria inválida, HTTP 503 e HTTP 429 levam ao formulário manual sem perder texto')
    modo('timeout');inicio=time.monotonic();s=a.call('POST','/ia/interpretacoes',{'texto':'Almoço 38'});assert s['manual'];assert time.monotonic()-inicio<23;ok('Timeout do provedor tratado sem bloquear registro manual')
    modo('valido');d=a.call('POST','/ia/diagnosticos?mes='+mes);assert len(d['texto'].split())<=250 and 'não constitui' in d['aviso'];ok('Diagnóstico com limite de palavras e aviso permanente')
    csv=a.call('GET','/exportacao');assert 'Café de teste' in csv;ok('Exportação CSV UTF-8')
    a.call('GET','/admin/metricas',status=403);a.call('GET','/dpo/auditoria',status=403);ok('Titular não acessa rotas administrativas ou do DPO')
    c=Cliente();c.call('POST','/lancamentos',entrada,status=401);req=urllib.request.Request(BASE+'/auth/login',data=b'{}',headers={'Content-Type':'application/json'},method='POST')
    try:urllib.request.urlopen(req);raise AssertionError('CSRF deveria impedir')
    except urllib.error.HTTPError as e:assert e.code==403
    ok('Rotas protegidas exigem autenticação e mutações exigem CSRF')
    b.call('POST','/privacidade/exclusao',{'senha':'errada'},status=401);b.call('POST','/privacidade/exclusao',{'senha':b.senha});b.call('GET','/auth/eu',status=401);ok('Exclusão exige reautenticação e revoga sessões imediatamente')
    (RUNTIME/'smoke-result.json').write_text(json.dumps({'data':hoje,'verificacoes':checks,'quantidade':len(checks)},ensure_ascii=False,indent=2),encoding='utf-8')
    # Conta A fica disponível para inspeção da interface; credenciais não entram em logs ou no Git.
    (RUNTIME/'acesso-teste.json').write_text(json.dumps({'email':a.email,'senha':a.senha,'id':a.perfil['id']}),encoding='utf-8')
    print(f'{len(checks)} verificações HTTP completas passaram. Evidência em .runtime/smoke-result.json')
