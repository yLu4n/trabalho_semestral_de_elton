"""PostgreSQL isolado de desenvolvimento: não altera instalações ou bancos existentes."""
from pathlib import Path
import subprocess, os, sys
from dev import ROOT, RUNTIME, ambiente
PG_BIN=Path(os.environ.get('PG_BIN','C:/Program Files/PostgreSQL/18/bin'))
DATA=RUNTIME/'postgres'
def pg(programa,*args,env=None):
    subprocess.run([str(PG_BIN/(programa+'.exe' if os.name=='nt' else programa)),*map(str,args)],check=True,env=env or ambiente())
if __name__=='__main__':
    acao=sys.argv[1] if len(sys.argv)>1 else 'start'
    if acao=='start':
        if not (ROOT/'.env').exists(): subprocess.run([sys.executable,str(ROOT/'scripts/dev.py'),'configurar'],check=True)
        if not DATA.exists():
            senha=RUNTIME/'pg-password.tmp';senha.write_text(ambiente()['DATABASE_PASSWORD'],encoding='utf-8')
            try: pg('initdb','-D',DATA,'-U','finai','--pwfile',senha,'--auth','scram-sha-256','--encoding','UTF8','--locale','C')
            finally: senha.unlink(missing_ok=True)
            with (DATA/'postgresql.conf').open('a',encoding='utf-8') as f: f.write("\nlisten_addresses = '127.0.0.1'\nport = 55432\nwal_level = replica\nsummarize_wal = on\nmax_wal_senders = 5\n")
        status=subprocess.run([str(PG_BIN/'pg_ctl.exe'),'-D',str(DATA),'status'],capture_output=True)
        if status.returncode: pg('pg_ctl','-D',DATA,'-l',RUNTIME/'postgres.log','-w','start')
        env=ambiente();env['PGPASSWORD']=env['DATABASE_PASSWORD']
        resultado=subprocess.run([str(PG_BIN/'psql.exe'),'-h','127.0.0.1','-p','55432','-U','finai','-d','postgres','-tAc',"SELECT 1 FROM pg_database WHERE datname='finai'"],capture_output=True,text=True,env=env,check=True)
        if resultado.stdout.strip()!='1': pg('createdb','-h','127.0.0.1','-p','55432','-U','finai','finai',env=env)
        print('PostgreSQL de desenvolvimento pronto em 127.0.0.1:55432/finai.')
    elif acao=='stop': pg('pg_ctl','-D',DATA,'-m','fast','-w','stop')
    else: raise SystemExit('Uso: python scripts/database.py start|stop')
