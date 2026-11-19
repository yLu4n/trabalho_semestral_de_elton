"""Coleta de saúde e apuração mensal UTC da disponibilidade externa."""
import argparse
import json
from datetime import datetime, timezone
from pathlib import Path
from urllib.error import URLError
from urllib.request import urlopen


def coletar(url, arquivo):
    agora = datetime.now(timezone.utc)
    try:
        with urlopen(url, timeout=5) as resposta:
            ok = resposta.status == 200 and json.load(resposta).get('status') == 'UP'
    except (URLError, OSError, ValueError, AttributeError):
        ok = False
    with arquivo.open('a', encoding='utf-8') as destino:
        destino.write(json.dumps({'utc': agora.isoformat(), 'ok': ok}) + '\n')
    return ok


def mensal(mes, arquivo):
    inicio = datetime.strptime(mes, '%Y-%m').replace(tzinfo=timezone.utc)
    fim = inicio.replace(year=inicio.year + (inicio.month == 12), month=inicio.month % 12 + 1)
    limite = min(fim, datetime.now(timezone.utc))
    total = int((limite - inicio).total_seconds() // 300)
    if total <= 0:
        raise ValueError('O mês ainda não contém intervalos completos de coleta.')
    slots = {}
    for linha in arquivo.read_text(encoding='utf-8').splitlines():
        prova = json.loads(linha)
        indice = int((datetime.fromisoformat(prova['utc']) - inicio).total_seconds() // 300)
        if 0 <= indice < total:
            slots[indice] = slots.get(indice, True) and prova['ok']
    exitos = sum(slots.values())
    taxa = exitos / total
    print(f'{mes} UTC: {taxa:.2%}, {exitos}/{total} intervalos com sucesso, {total-len(slots)} sem medição.')
    return limite == fim and taxa >= .99


def main():
    parser = argparse.ArgumentParser()
    modos = parser.add_subparsers(dest='modo', required=True)
    amostra = modos.add_parser('coletar')
    amostra.add_argument('url')
    amostra.add_argument('arquivo', type=Path)
    relatorio = modos.add_parser('mensal')
    relatorio.add_argument('mes')
    relatorio.add_argument('arquivo', type=Path)
    args = parser.parse_args()
    ok = coletar(args.url, args.arquivo) if args.modo == 'coletar' else mensal(args.mes, args.arquivo)
    return 0 if ok else 1


if __name__ == '__main__':
    raise SystemExit(main())
