"""Promove uma conta existente e verificada a ADMIN ou DPO pelo operador do banco.
O cadastro público sempre cria TITULAR; não existe senha administrativa padrão.
"""
import argparse,subprocess,re
from dev import ambiente
from pathlib import Path
parser=argparse.ArgumentParser();parser.add_argument('--email',required=True);parser.add_argument('--papel',choices=['ADMIN','DPO','TITULAR'],required=True);args=parser.parse_args()
if not re.fullmatch(r'[A-Za-z0-9._+@-]{3,254}',args.email):parser.error('E-mail inválido para esta ferramenta de operação.')
env=ambiente();env['PGPASSWORD']=env['DATABASE_PASSWORD'];bin=Path(env.get('PG_BIN','C:/Program Files/PostgreSQL/18/bin'))
sql="UPDATE usuario SET papel='"+args.papel+"',versao=versao+1 WHERE lower(email)=lower('"+args.email+"') AND email_verificado=true; DELETE FROM sessao WHERE usuario_id IN (SELECT id FROM usuario WHERE lower(email)=lower('"+args.email+"'));"
subprocess.run([str(bin/'psql.exe'),'-h',env.get('BACKUP_HOST','127.0.0.1'),'-p',env.get('BACKUP_PORT','55432'),'-U',env['DATABASE_USER'],'-d','finai','-v','ON_ERROR_STOP=1'],input=sql,text=True,env=env,check=True)
print('Operação concluída. A conta deve entrar novamente para usar o novo papel.')
