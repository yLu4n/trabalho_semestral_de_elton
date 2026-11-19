"""Gera evidência sanitizada dos testes; não copia contas, tokens ou logs completos."""
from pathlib import Path
import json,xml.etree.ElementTree as ET
from datetime import datetime
from zoneinfo import ZoneInfo
from dev import ROOT,RUNTIME
report=ET.parse(ROOT/'backend/target/site/jacoco/jacoco.xml').getroot()
package=next(p for p in report.findall('package') if p.attrib['name']=='br/com/finai/service')
counter=next(c for c in package.findall('counter') if c.attrib['type']=='LINE')
covered=int(counter.attrib['covered']);missed=int(counter.attrib['missed']);coverage=covered/(covered+missed)*100
tests=failures=errors=0
for p in (ROOT/'backend/target/surefire-reports').glob('TEST-*.xml'):
    r=ET.parse(p).getroot();tests+=int(r.attrib['tests']);failures+=int(r.attrib['failures']);errors+=int(r.attrib['errors'])
smoke=json.loads((RUNTIME/'smoke-result.json').read_text(encoding='utf-8'))
perf=json.loads((RUNTIME/'performance-result.json').read_text(encoding='utf-8')) if (RUNTIME/'performance-result.json').exists() else None
data=datetime.now(ZoneInfo('America/Bahia')).isoformat(timespec='seconds')
texto=f'''# Validação da entrega

Registro local: {data} (America/Bahia).

## Evidências executadas

| Verificação | Resultado |
|---|---|
| Java/Spring Boot | Pacote compilado em Java 21.0.7; Spring Boot 3.5.6. |
| Testes de regras | {tests} testes, {failures} falhas e {errors} erros. |
| Cobertura de linhas dos serviços | {coverage:.2f}% ({covered} linhas cobertas de {covered+missed}); mínimo exigido de 70%. |
| Angular | Angular 21.2.26; compilação de produção concluída. |
| Integração HTTP | {smoke['quantidade']} cenários completos no backend real, PostgreSQL 18 e dependência de IA simulada por Postman 1.76.0. |
| Interface | Login, redirecionamento após login, logout e formulário manual conferidos no navegador. Painel com largura 320, 1440 e 1920 pixels; sem rolagem horizontal de página nos extremos medidos. |
| Backup | Full, incremental e cadeia combinada verificados por pg_verifybackup, sem substituir o banco em uso. |

Os testes de serviço usam JUnit/Mockito. Os cenários de integração usam HTTP, autenticação, banco PostgreSQL e adaptador HTTP reais, com resposta da dependência controlada no Postman. A busca de mock publicado não foi realizada: o CLI instalado não oferece `postman status`, e nenhuma conexão autenticada foi configurada. O mock local deriva do contrato Chat Completions documentado, usa dados sintéticos e não atesta o comportamento ou a acurácia do fornecedor real.

## Cenários HTTP

'''+''.join('- '+item+'\n' for item in smoke['verificacoes'])
if perf:
    texto+=f'''
## Ensaio de carga local

{perf['solicitacoes']} requisições; {perf['erros']} erros; alvo de {perf['rps_alvo']} req/s por {perf['segundos_envio']} segundos; até {perf['workers_maximos']} workers.

- p95 observado: **{perf['p95_ms']:.2f} ms**.
- p95 de referência: {perf['baseline_p95_ms']:.2f} ms.
- Variação de p95: {perf['degradacao_p95_percentual']:.2f}%.
- Meta local de consultas <= 2s: **{'atingida neste ensaio' if perf['p95_ms']<=2000 else 'não atingida neste ensaio'}**.

{perf['observacao']} O teste não equivale ao aceite completo de 200 usuários distintos simultâneos. A meta de degradação <=20% precisa de cenário repetível em infraestrutura representativa.
'''
texto+='''
## Pendências de aceite externo

- Provedor de IA real: configurar a chave e validar autenticação, parâmetros, retenção contratada e p95. Nenhuma chamada paga foi realizada.
- Acurácia: executar as 50 frases sintéticas com o provedor real; meta de 85% sem correção de valor/tipo não foi medida. O mock não é um modelo de linguagem.
- Usabilidade: ensaio com cinco participantes, até três interações e menos de dez segundos não foi realizado.
- WCAG 2.2 AA: foram implementadas bases de acessibilidade e feitas verificações visuais; não há certificação integral de contraste, leitor de tela, teclado e todos os estados.
- Produção: Compose, gateway HTTPS e SMTP preparados; Docker não estava instalado. Não houve implantação pública, pentest completo ou medição mensal de disponibilidade de 99%.
- Backups: arquivos verificados, mas não foi realizada troca da instância de produção nem aferição de RTO de quatro horas. Agendamento diário/6h, cópia externa criptografada e retenção precisam ser ativados no ambiente operacional.

## Reproduzir

`python scripts/dev.py test`, `python scripts/dev.py build-frontend`, `python scripts/smoke.py` e `python scripts/performance.py`. Veja `operacao.md` para iniciar o simulador e o backend correto. As contas são fictícias e os relatórios locais com credenciais ficam em `.runtime`, ignorado pelo Git.
'''
(ROOT/'docs/validacao.md').write_text(texto,encoding='utf-8')
print(f'Evidência: {tests} testes, {coverage:.2f}% de cobertura, {smoke["quantidade"]} cenários HTTP.')
