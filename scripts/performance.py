"""Mede consultas autenticadas contra o backend real. Não faz chamadas à IA.
Crie antes .runtime/acesso-teste.json com scripts/smoke.py. Dados fictícios.
"""
import argparse,json,time,statistics,concurrent.futures,urllib.request,urllib.error,http.cookiejar
from dev import RUNTIME
parser=argparse.ArgumentParser();parser.add_argument('--users',type=int,default=200);parser.add_argument('--rps',type=int,default=50);parser.add_argument('--seconds',type=int,default=20);args=parser.parse_args()
acesso=json.loads((RUNTIME/'acesso-teste.json').read_text());jar=http.cookiejar.CookieJar();opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar));base='http://127.0.0.1:8080/api'
for tentativa in range(180):
    try:
        with urllib.request.urlopen(base+'/auth/csrf',timeout=2) as r:
            if r.status==200:break
    except (urllib.error.URLError,TimeoutError):pass
    time.sleep(1)
else:raise SystemExit('O backend não ficou pronto para o ensaio de carga.')
with opener.open(base+'/auth/csrf') as r:csrf=json.load(r)['token']
req=urllib.request.Request(base+'/auth/login',data=json.dumps({'email':acesso['email'],'senha':acesso['senha']}).encode(),headers={'Content-Type':'application/json','X-XSRF-TOKEN':csrf},method='POST')
opener.open(req).close();cookie='; '.join(c.name+'='+c.value for c in jar if c.name=='FINAI_SESSION')
def consulta(i):
    inicio=time.perf_counter()
    try:
        req=urllib.request.Request(base+('/painel' if i%2 else '/lancamentos'),headers={'Cookie':cookie})
        with urllib.request.urlopen(req,timeout=15) as r:r.read();assert r.status==200
        return (time.perf_counter()-inicio)*1000,True
    except Exception:return (time.perf_counter()-inicio)*1000,False
baseline=[consulta(i)[0] for i in range(30)]
inicio=time.monotonic();tarefas=[]
with concurrent.futures.ThreadPoolExecutor(max_workers=args.users) as pool:
    for i in range(args.rps*args.seconds):
        espera=inicio+i/args.rps-time.monotonic()
        if espera>0:time.sleep(espera)
        tarefas.append(pool.submit(consulta,i))
resultados=[f.result() for f in tarefas];latencias=sorted(ms for ms,ok in resultados if ok)
def p95(v):return sorted(v)[int((len(v)-1)*.95)]
relatorio={'solicitacoes':len(resultados),'erros':sum(not ok for _,ok in resultados),'workers_maximos':args.users,'rps_alvo':args.rps,'segundos_envio':args.seconds,'p95_ms':round(p95(latencias),2),'baseline_p95_ms':round(p95(baseline),2),'observacao':'Uma conta fictícia com histórico curto, até 200 workers. Não equivale a 200 titulares distintos simultâneos. Sem avaliação de latência do provedor real.'}
relatorio['degradacao_p95_percentual']=round((relatorio['p95_ms']/relatorio['baseline_p95_ms']-1)*100,2)
(RUNTIME/'performance-result.json').write_text(json.dumps(relatorio,indent=2,ensure_ascii=False),encoding='utf-8');print(json.dumps(relatorio,indent=2,ensure_ascii=False))
