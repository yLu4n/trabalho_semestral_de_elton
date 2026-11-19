"""Ferramentas locais: prepara ambiente, dependências e processos sem instalação global."""
from pathlib import Path
import os, sys, subprocess, secrets, shutil, urllib.request, zipfile
ROOT=Path(__file__).resolve().parents[1]
RUNTIME=ROOT/'.runtime'
RUNTIME.mkdir(exist_ok=True)
def ambiente():
    env=os.environ.copy()
    arquivo=ROOT/'.env'
    if arquivo.exists():
        for linha in arquivo.read_text(encoding='utf-8').splitlines():
            if linha and not linha.startswith('#') and '=' in linha:
                chave,valor=linha.split('=',1); env.setdefault(chave,valor)
    candidatos=list(Path('C:/Program Files/Java').glob('jdk-21*')) if os.name=='nt' else []
    if candidatos: env['JAVA_HOME']=str(candidatos[0])
    env['NG_CLI_ANALYTICS']='false'
    env.setdefault('NG_BUILD_MAX_WORKERS','2')
    return env
def maven():
    if shutil.which('mvn'): return shutil.which('mvn')
    candidatos=list((Path.home()/'.m2/wrapper/dists').glob('apache-maven-*/**/bin/mvn.cmd'))
    if candidatos: return str(sorted(candidatos)[-1])
    destino=RUNTIME/'apache-maven-3.9.15'
    if not destino.exists():
        arquivo=RUNTIME/'maven.zip'; urllib.request.urlretrieve('https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.15/apache-maven-3.9.15-bin.zip',arquivo)
        with zipfile.ZipFile(arquivo) as z: z.extractall(RUNTIME)
    return str(destino/'bin'/('mvn.cmd' if os.name=='nt' else 'mvn'))
def executar(args,cwd=ROOT):
    return subprocess.call(args,cwd=cwd,env=ambiente(),shell=os.name=='nt' and str(args[0]).endswith('.cmd'))
if __name__=='__main__':
    acao=sys.argv[1] if len(sys.argv)>1 else 'ajuda'
    if acao=='configurar':
        arquivo=ROOT/'.env'
        if not arquivo.exists(): arquivo.write_text('DATABASE_URL=jdbc:postgresql://localhost:55432/finai\nDATABASE_USER=finai\nDATABASE_PASSWORD='+secrets.token_urlsafe(24)+'\nSESSION_SECRET='+secrets.token_urlsafe(48)+'\nMAIL_MODE=file\n',encoding='utf-8')
        print('Configuração local pronta. Credenciais mantidas no arquivo .env ignorado pelo Git.')
    elif acao in ('install','frontend','build-frontend'):
        npm=shutil.which('npm.cmd') or shutil.which('npm')
        comando={'install':['install','--no-audit','--no-fund','--cache',str(RUNTIME/'npm-cache')],'frontend':['start'],'build-frontend':['run','build']}[acao]
        sys.exit(executar([npm,*comando],ROOT/'frontend'))
    elif acao in ('backend','backend-simulation','build','test'):
        if acao=='backend-simulation':
            os.environ['AI_ENDPOINT']='http://127.0.0.1:4010/llm/v1/chat/completions'
            os.environ['AI_API_KEY']='chave-ficticia-apenas-simulacao-local'
            os.environ['AI_MODEL']='llm-simulado'
        objetivos={'backend':['org.springframework.boot:spring-boot-maven-plugin:3.5.6:run'],'backend-simulation':['org.springframework.boot:spring-boot-maven-plugin:3.5.6:run'],'build':['-DskipTests','package'],'test':['verify']}[acao]
        sys.exit(executar([maven(),'-B',*objetivos,*sys.argv[2:]],ROOT/'backend'))
    else: print('Uso: python scripts/dev.py configurar | install | frontend | build-frontend | backend | build | test')
