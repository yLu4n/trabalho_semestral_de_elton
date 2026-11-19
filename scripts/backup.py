"""Backup físico PostgreSQL 18 completo/incremental e combinação para restauração.
Requer usuário com REPLICATION, summarize_wal=on e pg_hba apropriado.
Não interrompe o banco nem sobrescreve diretórios existentes.
"""
import argparse, subprocess, os, json
from pathlib import Path
from datetime import datetime, timezone
from dev import ambiente, RUNTIME
parser=argparse.ArgumentParser();parser.add_argument('acao',choices=['full','incremental','combine']);parser.add_argument('--previous',type=Path);parser.add_argument('--output',type=Path);parser.add_argument('--chain',nargs='+',type=Path);args=parser.parse_args()
env=ambiente();bin=Path(env.get('PG_BIN','C:/Program Files/PostgreSQL/18/bin'))
raiz=Path(env.get('BACKUP_DIR',str(RUNTIME/'backups'))).resolve();raiz.mkdir(parents=True,exist_ok=True)
destino=(args.output or raiz/(datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')+'-'+args.acao)).resolve()
if destino.exists():raise SystemExit('Escolha um diretório novo. O comando não sobrescreve backups.')
env['PGPASSWORD']=env.get('BACKUP_PASSWORD',env.get('DATABASE_PASSWORD',''))
def run(nome,params):subprocess.run([str(bin/(nome+'.exe' if os.name=='nt' else nome)),*map(str,params)],check=True,env=env)
if args.acao=='combine':
    if not args.chain:parser.error('--chain exige a cadeia completa em ordem, do full aos incrementais')
    run('pg_combinebackup',['-o',destino,*[p.resolve() for p in args.chain]])
else:
    params=['-h',env.get('BACKUP_HOST','127.0.0.1'),'-p',env.get('BACKUP_PORT','55432'),'-U',env.get('BACKUP_USER','finai'),'-D',destino,'--format=plain','--wal-method=stream','--checkpoint=fast','--no-password']
    if args.acao=='incremental':
        if not args.previous or not (args.previous/'backup_manifest').exists():parser.error('--previous deve apontar ao backup anterior com backup_manifest')
        params.append('--incremental='+str((args.previous/'backup_manifest').resolve()))
    run('pg_basebackup',params)
run('pg_verifybackup',[destino])
print('Backup verificado:',destino)
