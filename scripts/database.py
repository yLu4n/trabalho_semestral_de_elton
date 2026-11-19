"""Gerencia a instância PostgreSQL de desenvolvimento mantida em .runtime."""
import os
import subprocess
import sys
from pathlib import Path
from urllib.parse import urlsplit

from dev import ROOT, RUNTIME, ambiente

DATA = RUNTIME / 'postgres'
LOG = RUNTIME / 'postgres.log'


def pg(programa, *args, env=None, check=True, capture_output=False):
    env = ambiente() if env is None else env
    diretorio = Path(env.get('PG_BIN', 'C:/Program Files/PostgreSQL/18/bin'))
    executavel = diretorio / (programa + '.exe' if os.name == 'nt' else programa)
    return subprocess.run([str(executavel), *map(str, args)], env=env,
                          check=check, capture_output=capture_output,
                          encoding='utf-8', errors='replace')


def iniciar():
    if not (ROOT / '.env').exists():
        subprocess.run([sys.executable, str(ROOT / 'scripts/dev.py'), 'configurar'], check=True)
    env = ambiente()
    destino = urlsplit(env['DATABASE_URL'].removeprefix('jdbc:'))
    if destino.scheme != 'postgresql' or destino.hostname not in ('127.0.0.1', 'localhost'):
        raise ValueError('DATABASE_URL deve apontar para o PostgreSQL de desenvolvimento local.')
    porta = destino.port or 5432
    banco = destino.path.lstrip('/')
    if not banco or '/' in banco:
        raise ValueError('Informe o nome do banco em DATABASE_URL.')
    usuario = env['DATABASE_USER']
    env['PGPASSWORD'] = env['DATABASE_PASSWORD']
    env['PGCLIENTENCODING'] = 'UTF8'
    if not DATA.exists():
        senha = RUNTIME / 'pg-password.tmp'
        senha.write_text(env['PGPASSWORD'], encoding='utf-8')
        try:
            pg('initdb', '-D', DATA, '-U', usuario, '--pwfile', senha,
               '--auth', 'scram-sha-256', '--encoding', 'UTF8', '--locale', 'C', env=env)
        finally:
            senha.unlink(missing_ok=True)
        with (DATA / 'postgresql.conf').open('a', encoding='utf-8') as config:
            config.write("\nlisten_addresses = '127.0.0.1'\nwal_level = replica\nsummarize_wal = on\nmax_wal_senders = 5\n")

    status = pg('pg_ctl', '-D', DATA, 'status', check=False, capture_output=True, env=env)
    if status.returncode:
        try:
            pg('pg_ctl', '-D', DATA, '-l', LOG, '-w', '-o', f'-p {porta}', 'start', env=env)
        except subprocess.CalledProcessError:
            if LOG.exists():
                print('\n'.join(LOG.read_text(encoding='utf-8', errors='replace').splitlines()[-8:]), file=sys.stderr)
            raise SystemExit(f'Falha ao iniciar o PostgreSQL na porta {porta}. Log: {LOG}')

    conexao = ('-h', '127.0.0.1', '-p', porta, '-U', usuario)
    bancos = pg('psql', *conexao, '-d', 'postgres', '-tAc',
                'SELECT datname FROM pg_database', env=env, capture_output=True)
    if banco not in bancos.stdout.splitlines():
        pg('createdb', *conexao, '--', banco, env=env)
    pg('psql', *conexao, '-d', banco, '-tAc', 'SELECT 1', env=env, capture_output=True)
    print(f'PostgreSQL de desenvolvimento pronto em 127.0.0.1:{porta}/{banco}.')


if __name__ == '__main__':
    acao = sys.argv[1] if len(sys.argv) > 1 else 'start'
    try:
        if acao == 'start':
            iniciar()
        elif acao == 'stop':
            pg('pg_ctl', '-D', DATA, '-m', 'fast', '-w', 'stop')
        else:
            raise SystemExit('Uso: python scripts/database.py start|stop')
    except (OSError, ValueError, KeyError, subprocess.CalledProcessError) as erro:
        detalhe = getattr(erro, 'stderr', None)
        raise SystemExit(detalhe.strip() if detalhe else str(erro)) from None
