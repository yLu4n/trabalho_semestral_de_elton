# FinAI | Projeto Elton

O FinAI é um sistema de finanças pessoais. O titular pode registrar uma receita ou despesa manualmente ou descrever a movimentação em linguagem natural. Quando a IA interpreta uma frase, o usuário pode revisar, corrigir ou descartar a sugestão antes de salvar.

## Funcionalidades

- Cadastro, verificação de e-mail, recuperação de senha por vínculo de uso único e sessões autenticadas. Contas com e-mail pendente podem criar até cinco lançamentos.
- Extrato com filtros, lixeira e restauração de lançamentos, categorias, metas mensais, painel financeiro e exportação CSV em UTF-8.
- Interpretação de lançamentos, aprendizado por correções de categoria e diagnóstico mensal com dados agregados.
- Solicitações de privacidade, exclusão de conta com confirmação por e-mail e áreas de operação para administração e encarregado de dados (DPO).

## Tecnologias

- Backend em Java 21 com Spring Boot, Spring Security, JPA e Flyway.
- Frontend em Angular 22 com TypeScript.
- PostgreSQL 18. Scripts de desenvolvimento e aceite em Python.

## Estrutura do projeto

| Pasta                                 | Conteúdo                                                                         |
| ------------------------------------- | -------------------------------------------------------------------------------- |
| [backend](backend/)                   | API REST, autenticação, regras de negócio, integração com IA e migrações Flyway. |
| [frontend](frontend/)                 | Interface Angular para titulares, administradores e DPO.                         |
| [scripts](scripts/)                   | Inicialização, testes, avaliação da IA, carga, disponibilidade e backups.        |
| [infra](infra/)                       | Configurações de referência de proxy e HTTPS.                                    |
| [docs](docs/)                         | Requisitos, diagramas, contrato da API e roteiro de aceite.                      |
| [postman](postman/) e [tests](tests/) | Coleção e simulação Postman, além de frases sintéticas para avaliar a IA.        |

## Executar no Windows

### Pré-requisitos

Instale o JDK 21, PostgreSQL 18, Python 3 e Node.js com npm. Para gerar sugestões com IA local, instale também o Ollama e baixe o modelo conforme a seção abaixo. O Angular CLI deste projeto aceita Node.js 22 a partir de 22.22.3, Node.js 24 a partir de 24.15 ou Node.js 26 e superior. Os scripts usam o Maven disponível no ambiente e podem baixá-lo se necessário.

Se os executáveis do PostgreSQL estiverem em outra pasta, defina a variável de ambiente PG_BIN antes de rodar o script de banco:

```powershell
$env:PG_BIN = 'C:\caminho\para\PostgreSQL\18\bin'
```

### Instalação portátil com o atalho local

Se esta cópia foi preparada com o atalho `Iniciar-FinAI.cmd` na pasta `outputs` ao lado do repositório, execute o comando abaixo no PowerShell a partir da raiz do projeto. O atalho usa as ferramentas portáteis em `../work/finai-tools`. Esses arquivos fazem parte da instalação local e ficam fora do repositório.

```powershell
& '..\outputs\Iniciar-FinAI.cmd'
```

Acesse [FinAI no navegador](http://localhost:4200). Nesta instalação, a API usa a porta `8081` e o PostgreSQL usa a porta `54330`, com dados em `.runtime/postgres-local`. O atalho configura essas opções para os processos que ele inicia.

Para reiniciar apenas o backend ou o frontend, encerre o processo correspondente com **Ctrl+C** e use o comando abaixo. Para encerrar o banco após parar o backend, use `parar-banco`.

```powershell
& '..\outputs\Iniciar-FinAI.cmd' backend
& '..\outputs\Iniciar-FinAI.cmd' frontend
& '..\outputs\Iniciar-FinAI.cmd' parar-banco
```

### Inicialização de um clone novo

Com os pré-requisitos instalados, execute na raiz do repositório no PowerShell:

```powershell
python scripts/dev.py configurar
python scripts/database.py start
python scripts/dev.py install
```

O primeiro comando cria o arquivo .env local com uma senha para o PostgreSQL e um segredo de sessão. O banco guarda os dados em .runtime/postgres e usa a porta indicada em DATABASE_URL, no arquivo .env. O Flyway aplica as migrações pendentes ao iniciar o backend.

Se o log .runtime/postgres.log indicar que o Windows bloqueou a porta 55432, escolha uma porta livre. Por exemplo, use DATABASE_URL=jdbc:postgresql://127.0.0.1:54329/finai e BACKUP_PORT=54329 no arquivo .env. Depois execute novamente python scripts/database.py start. Preserve a pasta .runtime/postgres.

Em um terminal, inicie o backend. Se o banco local do projeto já foi inicializado e estiver parado, o script tenta iniciá-lo antes de chamar o Maven:

```powershell
python scripts/dev.py backend
```

Em outro terminal, inicie o frontend:

```powershell
python scripts/dev.py frontend
```

Acesse [FinAI no navegador](http://localhost:4200), a mesma origem usada nos links de e-mail da configuração padrão. O frontend encaminha as chamadas ao backend na porta 8080. Para verificar o e-mail ou testar a recuperação de senha no modo local, abra a mensagem gerada em backend/var/mail e use o vínculo indicado. Após o cadastro, entre com seu e-mail e senha.

Ao encerrar, pare o frontend e o backend com Ctrl+C nos respectivos terminais. Depois, pare o banco local:

```powershell
python scripts/database.py stop
```

## Configuração

Consulte [.env.example](.env.example) e mantenha as credenciais no arquivo local .env. O Git ignora esse arquivo, o banco de desenvolvimento, os modelos baixados e os e-mails de teste. O arquivo .env.example contém somente valores de exemplo.

| Serviço        | Configuração                                                                                                                                                                 |
| -------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Banco          | DATABASE_URL, DATABASE_USER e DATABASE_PASSWORD                                                                                                                              |
| Sessão e links | SESSION_SECRET, PUBLIC_URL e COOKIE_SECURE                                                                                                                                   |
| IA             | AI_PROVIDER, AI_MODEL e AI_ENDPOINT. Use AI_API_KEY para provedores que exigem chave. AI_INPUT_COST e AI_OUTPUT_COST são opcionais e servem para estimar custos por chamada. |
| E-mail         | MAIL_MODE=file grava mensagens de desenvolvimento em arquivos. MAIL_MODE=smtp usa MAIL_FROM e as variáveis SMTP_*.                                                          |

O lançamento manual continua disponível quando a IA está desativada ou indisponível. No Windows, o script de inicialização usa os certificados confiáveis do sistema para o Java, caso você não tenha configurado um repositório de certificados próprio. Para usar um provedor real, configure as credenciais antes de iniciar o backend. O projeto também inclui uma simulação Postman. Para testar sem chave, rode python scripts/dev.py ia-local em um terminal. Em outro terminal, execute python scripts/dev.py backend-simulation no lugar de backend. O simulador roda na porta 4010 e devolve respostas de teste.

Para uma implantação com o perfil Spring production, configure PUBLIC_URL com a origem HTTPS, COOKIE_SECURE=true e MAIL_MODE=smtp. A pasta [infra](infra/) contém as configurações de referência para Caddy e Nginx.

### IA local com Ollama

Em uma cópia nova do repositório, [instale o Ollama para Windows](https://ollama.com/download/windows). O script procura o executável em .runtime/ollama-bin ou no PATH. Ele sobe o serviço apenas em 127.0.0.1, desativa recursos de nuvem e salva os modelos baixados em .runtime/ollama-models. A versão portátil também guarda seus arquivos de configuração em .runtime/ollama-profile.

No arquivo .env, use:

```dotenv
AI_PROVIDER=ollama
AI_ENDPOINT=http://127.0.0.1:11434/v1/chat/completions
AI_MODEL=qwen2.5:3b
AI_INPUT_COST=0
AI_OUTPUT_COST=0
```

O Ollama local dispensa chave de API. Na primeira vez, execute em um terminal:

```powershell
python scripts/dev.py ollama-local
```

Com o serviço ativo, execute em outro terminal:

```powershell
python scripts/dev.py ollama-pull
python scripts/dev.py backend
```

Nas próximas vezes, o comando de backend inicia o Ollama quando necessário e tenta carregar o modelo na memória antes de disponibilizar a API. Aguarde a mensagem **Ollama local pronto**. O log do serviço iniciado pelo projeto fica em .runtime/ollama.log. Cada chamada ao provedor tem limite de oito segundos. As metas de latência e acurácia estão no [roteiro de aceite](docs/aceite.md). Veja a [documentação do Ollama para Windows](https://docs.ollama.com/windows).

### Interpretação de lançamentos

Na tela de lançamento por IA, escreva uma frase como “Paguei R$ 52,30 no hospital ontem” e revise o rascunho antes de salvar. O FinAI reconhece vocabulário em português do Brasil para as nove categorias padrão e procura pelo nome das categorias personalizadas ativas. Por exemplo, ônibus e Uber indicam Transporte, hospital e farmácia indicam Saúde, e Uber Eats indica Alimentação.

Quando a frase indica uma categoria clara e o Ollama falha, o FinAI pode abrir um rascunho com a categoria e preencher o valor explícito e a data relativa quando reconhecidos. Se faltar o valor, preencha-o no formulário. A cota de interpretação é liberada quando a chamada termina sem uma sugestão válida.

### Diagnóstico financeiro mensal

Em **Visão geral**, selecione o mês no topo e role até **Um olhar para o mês**. Clique em **Gerar diagnóstico mensal**. O resultado aparece na mesma página, abaixo do cartão.

O backend calcula os totais e até três das maiores variações de despesas em relação ao mês anterior. Com Ollama configurado, o modelo gera o comentário qualitativo a partir das tendências calculadas. Os valores e os nomes das categorias são exibidos pelo FinAI. O diagnóstico tem limite de 250 palavras e não consome a cota de interpretação de lançamentos. Para conferir o fluxo, registre despesas em dois meses consecutivos e gere o diagnóstico para o mês mais recente.

## Testes e documentação

A versão atual registra logins bem-sucedidos em uma trilha que permanece após o logout. Na confirmação de sugestões da IA, a trilha identifica os campos alterados. O cálculo do estado das metas usa valores exatos, com arredondamento na exibição.

```powershell
python scripts/dev.py test
python scripts/dev.py build-frontend
```

O teste HTTP python scripts/smoke.py usa o PostgreSQL e o backend em modo de simulação. Inicie o simulador com python scripts/dev.py ia-local, depois inicie o backend com python scripts/dev.py backend-simulation.

- [Roteiro de aceite](docs/aceite.md) para IA real, carga, usabilidade, acessibilidade, disponibilidade, HTTPS, SMTP e restauração de backups.
- [Contrato OpenAPI](docs/openapi.json) e [coleção Postman](postman/FinAI.postman_collection.json).
- [Diagramas BPMN e UML de classes](docs/diagramas/) e [requisitos originais](docs/requisitos-originais.txt).
