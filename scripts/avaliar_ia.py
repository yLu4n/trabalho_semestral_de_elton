"""Avaliação real de 50 frases. Consome cota e créditos do provedor configurado.
Execute somente com backend normal e AI_API_KEY; nunca reporte o mock como acurácia.
"""
import json,sys,time
from decimal import Decimal
from dev import ROOT,RUNTIME
from smoke import Cliente
if '--confirmar-uso-real' not in sys.argv:raise SystemExit('Use --confirmar-uso-real para autorizar explicitamente as 50 chamadas ao provedor configurado.')
acesso=json.loads((RUNTIME/'acesso-teste.json').read_text());c=Cliente();c.call('POST','/auth/login',{'email':acesso['email'],'senha':acesso['senha']});c.call('GET','/auth/csrf');c.call('GET','/auth/csrf')
resultados=[]
for caso in json.loads((ROOT/'tests/frases-ia.json').read_text(encoding='utf-8')):
    inicio=time.monotonic();s=c.call('POST','/ia/interpretacoes',{'texto':caso['texto']})
    try:correto=not s['manual'] and s['campos']['tipo']==caso['tipo'] and Decimal(s['campos']['valor'])==Decimal(caso['valor'])
    except (KeyError,TypeError):correto=False
    resultados.append({'texto':caso['texto'],'correto':correto,'duracao_ms':round((time.monotonic()-inicio)*1000)})
    if s.get('id'):c.call('DELETE','/ia/sugestoes/'+s['id'])
taxa=sum(r['correto'] for r in resultados)/len(resultados)
relatorio={'taxa_sem_edicao_valor_tipo':taxa,'aprovado':taxa>=.85,'casos':resultados}
(RUNTIME/'avaliacao-ia-real.json').write_text(json.dumps(relatorio,ensure_ascii=False,indent=2),encoding='utf-8');print(f'Acurácia: {taxa:.1%}. Critério: pelo menos 85%.')
raise SystemExit(0 if taxa>=.85 else 1)
