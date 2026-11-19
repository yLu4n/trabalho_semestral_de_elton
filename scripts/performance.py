"""Ensaio HTTP local em base isolada, com contas sintéticas de titulares."""
import argparse
import concurrent.futures
import json
import math
import time
from datetime import date
from pathlib import Path

from dev import RUNTIME
from smoke import Cliente


def autenticar(acesso):
    cliente = Cliente()
    cliente.call('POST', '/auth/login', acesso)
    cliente.call('GET', '/auth/csrf')
    return cliente


def preparar(acesso, persistencia):
    cliente = autenticar(acesso)
    categoria = None
    if persistencia:
        categoria = next(c['id'] for c in cliente.call('GET', '/categorias')
                         if c['ativa'] and c['tipo'] == 'DESPESA')
    return cliente, categoria


def transacao(ator, indice):
    inicio = time.perf_counter()
    cliente, categoria = ator
    try:
        if categoria and indice % 5 == 0:
            cliente.call('POST', '/metas', {
                'categoriaId': categoria, 'mes': date.today().strftime('%Y-%m'),
                'limite': str(100 + (indice // 5) % 397)
            })
        else:
            cliente.call('GET', '/painel' if indice % 2 else '/lancamentos')
        return (time.perf_counter() - inicio) * 1000, True
    except (OSError, ValueError, AssertionError):
        return (time.perf_counter() - inicio) * 1000, False


def ensaio(atores, rps, segundos):
    tarefas = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=len(atores)) as pool:
        inicio = time.monotonic()
        for i in range(rps * segundos):
            espera = inicio + i / rps - time.monotonic()
            if espera > 0:
                time.sleep(espera)
            # Keep a browser and its cookie jar single-threaded when sessions are reused.
            if i >= len(atores):
                tarefas[i - len(atores)].result()
            tarefas.append(pool.submit(transacao, atores[i % len(atores)], i))
        duracao_envio = max(segundos, time.monotonic() - inicio)
        resultados = [future.result() for future in tarefas]
    latencias = sorted(ms for ms, ok in resultados if ok)
    p95 = latencias[math.ceil(len(latencias) * .95) - 1] if latencias else None
    return round(p95, 2) if p95 is not None else None, sum(not ok for _, ok in resultados), len(resultados), duracao_envio


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--users', type=int, default=200)
    parser.add_argument('--rps', type=int, default=50)
    parser.add_argument('--seconds', type=int, default=600)
    parser.add_argument('--contas-teste', type=Path, help='JSON com email e senha por titular fictício')
    parser.add_argument('--base-isolada', action='store_true', help='A base local de teste aceita gravações de metas sintéticas')
    args = parser.parse_args()
    if min(args.users, args.rps, args.seconds) <= 0:
        parser.error('Usuários, duração e taxa devem ser positivos.')
    if args.contas_teste:
        if not args.base_isolada:
            parser.error('O ensaio com persistência exige --base-isolada.')
        contas = json.loads(args.contas_teste.read_text(encoding='utf-8-sig'))
        contas = [{campo: c[campo] for campo in ('email', 'senha')} for c in contas]
        if len({c['email'].strip().lower() for c in contas[:args.users]}) != args.users:
            parser.error('Forneça uma conta fictícia distinta por usuário virtual.')
        contas = contas[:args.users]
    else:
        acesso = json.loads((RUNTIME / 'acesso-teste.json').read_text(encoding='utf-8-sig'))
        contas = [{campo: acesso[campo] for campo in ('email', 'senha')}]
    atores = [preparar(conta, bool(args.contas_teste)) for conta in contas]
    referencia, erros_referencia, _, _ = ensaio(atores[:1], args.rps, 15)
    p95, erros, total, duracao = ensaio(atores, args.rps, args.seconds)
    variacao = round((p95 / referencia - 1) * 100, 2) if p95 is not None and referencia else None
    elegivel = bool(args.contas_teste and args.users >= 200 and args.rps >= 50 and args.seconds >= 600)
    aprovado = bool(elegivel and not erros and not erros_referencia and variacao is not None
                    and p95 <= 2000 and variacao <= 20 and total / duracao >= 50)
    relatorio = {
        'solicitacoes': total, 'erros': erros, 'erros_baseline': erros_referencia,
        'workers_maximos': len(atores), 'usuarios_distintos': len(contas), 'rps_alvo': args.rps,
        'rps_observado': round(total / duracao, 3), 'segundos_envio': args.seconds,
        'p95_ms': p95, 'baseline_p95_ms': referencia, 'degradacao_p95_percentual': variacao,
        'aprovado': aprovado,
        'observacao': ('Contas distintas, painel, extrato e gravação de metas.' if args.contas_teste
                       else 'Diagnóstico com uma conta, somente leitura. Não comprova o aceite de carga.')
    }
    texto = json.dumps(relatorio, indent=2, ensure_ascii=False)
    (RUNTIME / 'performance-result.json').write_text(texto, encoding='utf-8')
    print(texto)
    return 0 if aprovado or (not elegivel and not erros and not erros_referencia) else 1


if __name__ == '__main__':
    raise SystemExit(main())
