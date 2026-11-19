"""Ferramentas locais: prepara ambiente, dependências e processos sem instalação global."""
from pathlib import Path
from urllib.parse import urlsplit
import socket
import http.client
import json
import time
from contextlib import contextmanager, nullcontext
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
def executar(args,cwd=ROOT,env=None):
    try:
        return subprocess.call(args,cwd=cwd,env=ambiente() if env is None else env,shell=os.name=='nt' and str(args[0]).endswith('.cmd'))
    except KeyboardInterrupt:
        return 130


def assegurar_banco_local():
    url=ambiente().get('DATABASE_URL', '')
    destino=urlsplit(url.removeprefix('jdbc:'))
    if destino.scheme!='postgresql' or destino.hostname not in ('localhost','127.0.0.1'):
        return
    try:
        with socket.create_connection(('127.0.0.1', destino.port or 5432), timeout=2):
            return
    except OSError:
        if not (RUNTIME/'postgres'/'PG_VERSION').is_file():
            raise SystemExit('PostgreSQL local indisponível. Execute python scripts/database.py start.')
    print('PostgreSQL local parado. Iniciando o banco do projeto…', flush=True)
    if subprocess.call([sys.executable,str(ROOT/'scripts/database.py'),'start'],cwd=ROOT):
        raise SystemExit('O backend não foi iniciado. Veja .runtime/postgres.log.')
def configurar_ollama():
    portavel=RUNTIME/'ollama-bin'/('ollama.exe' if os.name=='nt' else 'ollama')
    ollama=str(portavel) if portavel.is_file() else shutil.which('ollama')
    if not ollama:
        raise SystemExit('Instale o Ollama ou disponibilize o executável em .runtime/ollama-bin.')
    env=os.environ.copy()
    env.update(OLLAMA_HOST='127.0.0.1:11434', OLLAMA_MODELS=str(RUNTIME/'ollama-models'), OLLAMA_NO_CLOUD='1')
    env.pop('AI_API_KEY', None)
    if os.name=='nt' and portavel.is_file():
        perfil_ollama=RUNTIME/'ollama-profile'
        perfil_ollama.mkdir(exist_ok=True)
        env['USERPROFILE']=str(perfil_ollama)
    return ollama,env


def modelos_ollama():
    conexao=http.client.HTTPConnection('127.0.0.1',11434,timeout=2)
    try:
        conexao.request('GET','/api/tags')
        resposta=conexao.getresponse()
        if resposta.status!=200:
            return None
        return {item['name'] for item in json.loads(resposta.read()).get('models',[])}
    except (OSError, ValueError, KeyError, http.client.HTTPException):
        return None
    finally:
        conexao.close()


@contextmanager
def manter_ollama_local():
    configuracao=ambiente()
    destino=urlsplit(configuracao.get('AI_ENDPOINT',''))
    if (configuracao.get('AI_PROVIDER','').lower()!='ollama'
            or destino.hostname not in ('localhost','127.0.0.1') or destino.port!=11434):
        yield
        return
    modelos=modelos_ollama()
    processo=None
    try:
        if modelos is None:
            try:
                ollama,env=configurar_ollama()
            except (SystemExit, OSError) as erro:
                print(f'Ollama indisponível: {erro} O lançamento manual continua disponível.',flush=True)
                yield
                return
            log=RUNTIME/'ollama.log'
            print('Iniciando o Ollama local…',flush=True)
            try:
                with log.open('ab') as saida:
                    processo=subprocess.Popen([ollama,'serve'],env=env,stdout=saida,stderr=subprocess.STDOUT,
                        creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
            except OSError as erro:
                print(f'Não foi possível iniciar o Ollama: {erro} O lançamento manual continua disponível.',flush=True)
                yield
                return
            limite=time.monotonic()+45
            while time.monotonic()<limite and processo.poll() is None:
                modelos=modelos_ollama()
                if modelos is not None:
                    break
                time.sleep(.25)
        if modelos is None:
            print('Ollama indisponível. Veja .runtime/ollama.log. O lançamento manual continua disponível.',flush=True)
        elif configuracao.get('AI_MODEL','qwen2.5:3b') not in modelos:
            print('Modelo local ausente. Rode python scripts/dev.py ollama-pull com o serviço ativo.',flush=True)
        else:
            print('Ollama local pronto.',flush=True)
        yield
    finally:
        if processo is not None and processo.poll() is None:
            processo.terminate()
            try:
                processo.wait(timeout=5)
            except subprocess.TimeoutExpired:
                processo.kill()
                processo.wait()

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
    elif acao in ('ollama-local','ollama-pull'):
        ollama,env=configurar_ollama()
        argumentos=['serve'] if acao=='ollama-local' else ['pull',ambiente().get('AI_MODEL','qwen2.5:3b')]
        sys.exit(executar([ollama,*argumentos],env=env))
    elif acao=='ia-local':
        node=shutil.which('node')
        if not node:
            raise SystemExit('Node.js é necessário para executar a simulação local.')
        os.environ['PORT']='4010'
        print('Simulação da IA em http://127.0.0.1:4010/llm',flush=True)
        sys.exit(executar([node,str(ROOT/'postman/mocks/llm/default.js')]))
    elif acao in ('backend','backend-simulation','build','test'):
        if acao in ('backend','backend-simulation'):
            assegurar_banco_local()
            opcoes_java=('JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS')
            if os.name=='nt' and not any('javax.net.ssl.trustStore' in os.getenv(k,'') for k in opcoes_java):
                atuais=os.getenv('JAVA_TOOL_OPTIONS','')
                os.environ['JAVA_TOOL_OPTIONS']=(atuais+' -Djavax.net.ssl.trustStoreType=Windows-ROOT').strip()
        if acao=='backend-simulation':
            os.environ['AI_PROVIDER']='openai'
            os.environ['AI_ENDPOINT']='http://127.0.0.1:4010/llm/v1/chat/completions'
            os.environ['AI_API_KEY']='chave-ficticia-apenas-simulacao-local'
            os.environ['AI_MODEL']='llm-simulado'
        objetivos={'backend':['org.springframework.boot:spring-boot-maven-plugin:3.5.6:run'],'backend-simulation':['org.springframework.boot:spring-boot-maven-plugin:3.5.6:run'],'build':['-DskipTests','package'],'test':['verify']}[acao]
        with manter_ollama_local() if acao=='backend' else nullcontext():
            sys.exit(executar([maven(),'-B',*objetivos,*sys.argv[2:]],ROOT/'backend'))
    else: print('Uso: python scripts/dev.py configurar | install | frontend | build-frontend | ollama-local | ollama-pull | ia-local | backend | backend-simulation | build | test')
