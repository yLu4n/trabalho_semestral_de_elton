# Aceite do FinAI

Registre a versão, o ambiente, a data, o conjunto de dados e os resultados de cada execução.

| Critério | Evidência |
| --- | --- |
| IA real | Configure o provedor e rode python scripts/avaliar_ia.py --confirmar-uso-real com uma conta fictícia com cota. Esperado: no mínimo 85% das 50 frases sem alterar tipo ou valor e p95 de até 6 s. O relatório fica em .runtime/avaliacao-ia-real.json. |
| Carga | Em uma base isolada, forneça um JSON com 200 objetos contendo email e senha de titulares fictícios. Rode python scripts/performance.py --contas-teste CONTAS.json --base-isolada. O cenário mistura painel, extrato e gravação de metas. Registre p95 de até 2 s e degradação de até 20% contra a referência. |
| Usabilidade | Observe cinco pessoas sem treinamento do envio da frase até a confirmação. Registre duração e interações por pessoa. Esperado: até três interações e menos de dez segundos. |
| Acessibilidade | Verifique WCAG 2.2 AA com ferramenta automatizada, teclado e leitor de tela em cadastro, lançamento e painel. Cubra 320 a 1920 px, contraste, estados de erro e alternativas tabulares aos gráficos. |
| HTTPS e SMTP | Valide o domínio e o certificado do gateway, TLS 1.2 ou superior e cookies seguros. Confira na caixa de teste os e-mails de verificação, recuperação e conclusão da exclusão. |
| Disponibilidade | A partir de um monitor externo, agende a cada cinco minutos python scripts/disponibilidade.py coletar https://DOMINIO/actuator/health REGISTRO.jsonl. Após fechar o mês UTC, execute python scripts/disponibilidade.py mensal AAAA-MM REGISTRO.jsonl. Intervalos sem amostra não contam como sucesso. Meta de 99%. |
| Backups | Agende backup.py full diariamente e backup.py incremental a cada seis horas, apontando --previous ao manifesto mais recente da cadeia. Use destino externo com retenção de pelo menos 30 dias. Faça um restauro isolado com backup.py combine --chain e valide dados e tempo de retorno. Esperado: ponto de recuperação de até 6 h e retorno em até 4 h. |

Os scripts produzem evidência quando executados com as dependências do ambiente de aceite. Para SMTP e IA, use contas e serviços de teste configurados. Os testes unitários podem ser executados com python scripts/dev.py test.
