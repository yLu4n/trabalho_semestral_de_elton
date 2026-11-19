CREATE TABLE usuario (
 id UUID PRIMARY KEY, nome VARCHAR(100) NOT NULL, email VARCHAR(254) NOT NULL UNIQUE,
 senha_hash VARCHAR(255) NOT NULL, nascimento DATE NOT NULL, email_verificado BOOLEAN NOT NULL,
 papel VARCHAR(20) NOT NULL, tentativas INTEGER NOT NULL, bloqueado_ate TIMESTAMPTZ,
 bloqueado BOOLEAN NOT NULL, exclusao_solicitada_em TIMESTAMPTZ, criado_em TIMESTAMPTZ NOT NULL, versao BIGINT NOT NULL
);
CREATE TABLE sessao (id UUID PRIMARY KEY, usuario_id UUID NOT NULL REFERENCES usuario ON DELETE CASCADE,
 criada_em TIMESTAMPTZ NOT NULL, ultima_atividade TIMESTAMPTZ NOT NULL);
CREATE INDEX idx_sessao_usuario ON sessao(usuario_id);
CREATE TABLE verificacao_email (id UUID PRIMARY KEY, usuario_id UUID NOT NULL REFERENCES usuario ON DELETE CASCADE,
 token_hash VARCHAR(64) NOT NULL UNIQUE, expira_em TIMESTAMPTZ NOT NULL);
CREATE TABLE categoria (id UUID PRIMARY KEY, usuario_id UUID REFERENCES usuario ON DELETE CASCADE,
 nome VARCHAR(80) NOT NULL, tipo VARCHAR(10) NOT NULL CHECK(tipo IN ('RECEITA','DESPESA')),
 cor VARCHAR(7) NOT NULL, ativa BOOLEAN NOT NULL);
CREATE UNIQUE INDEX idx_categoria_nome ON categoria(COALESCE(usuario_id, '00000000-0000-0000-0000-000000000000'::uuid),lower(nome),tipo);
CREATE TABLE lancamento (id UUID PRIMARY KEY, usuario_id UUID NOT NULL REFERENCES usuario ON DELETE CASCADE,
 categoria_id UUID NOT NULL REFERENCES categoria, tipo VARCHAR(10) NOT NULL,
 valor NUMERIC(14,2) NOT NULL CHECK(valor>0 AND valor<=1000000), data DATE NOT NULL,
 descricao VARCHAR(280) NOT NULL, estabelecimento VARCHAR(120), forma_pagamento VARCHAR(60),
 origem VARCHAR(10) NOT NULL, excluido_em TIMESTAMPTZ, criado_em TIMESTAMPTZ NOT NULL, versao BIGINT NOT NULL);
CREATE INDEX idx_lancamento_usuario_data ON lancamento(usuario_id,data DESC) WHERE excluido_em IS NULL;
CREATE INDEX idx_lancamento_retencao ON lancamento(excluido_em) WHERE excluido_em IS NOT NULL;
CREATE TABLE meta_orcamento (id UUID PRIMARY KEY, usuario_id UUID NOT NULL REFERENCES usuario ON DELETE CASCADE,
 categoria_id UUID NOT NULL REFERENCES categoria, mes VARCHAR(7) NOT NULL,
 limite NUMERIC(14,2) NOT NULL CHECK(limite>0), UNIQUE(usuario_id,categoria_id,mes));
CREATE TABLE correcao_categoria (id UUID PRIMARY KEY, usuario_id UUID NOT NULL REFERENCES usuario ON DELETE CASCADE,
 termo VARCHAR(280) NOT NULL, categoria_sugerida UUID REFERENCES categoria,
 categoria_confirmada UUID NOT NULL REFERENCES categoria, criada_em TIMESTAMPTZ NOT NULL);
CREATE INDEX idx_correcao_usuario ON correcao_categoria(usuario_id,criada_em DESC);
CREATE TABLE sugestao_lancamento (id UUID PRIMARY KEY, usuario_id UUID NOT NULL REFERENCES usuario ON DELETE CASCADE,
 texto_original VARCHAR(280) NOT NULL, dados TEXT NOT NULL, criada_em TIMESTAMPTZ NOT NULL);
CREATE TABLE uso_ia (id UUID PRIMARY KEY, usuario_id UUID NOT NULL REFERENCES usuario ON DELETE CASCADE,
 dia DATE NOT NULL, quantidade INTEGER NOT NULL, UNIQUE(usuario_id,dia));
CREATE TABLE registro_inferencia (id UUID PRIMARY KEY, modelo VARCHAR(120), operacao VARCHAR(30), latencia_ms BIGINT NOT NULL,
 custo_estimado NUMERIC(18,8) NOT NULL, resultado VARCHAR(40), criado_em TIMESTAMPTZ NOT NULL);
CREATE TABLE solicitacao_privacidade (id UUID PRIMARY KEY, usuario_id UUID REFERENCES usuario ON DELETE SET NULL,
 tipo VARCHAR(40) NOT NULL, estado VARCHAR(30) NOT NULL, criada_em TIMESTAMPTZ NOT NULL);
CREATE TABLE evento_auditoria (id UUID PRIMARY KEY, acao VARCHAR(80) NOT NULL, criado_em TIMESTAMPTZ NOT NULL);
INSERT INTO categoria(id,nome,tipo,cor,ativa) VALUES
 ('10000000-0000-0000-0000-000000000001','Alimentação','DESPESA','#c05e25',true),
 ('10000000-0000-0000-0000-000000000002','Moradia','DESPESA','#6455b0',true),
 ('10000000-0000-0000-0000-000000000003','Transporte','DESPESA','#277390',true),
 ('10000000-0000-0000-0000-000000000004','Saúde','DESPESA','#b13b65',true),
 ('10000000-0000-0000-0000-000000000005','Educação','DESPESA','#997313',true),
 ('10000000-0000-0000-0000-000000000006','Lazer','DESPESA','#635acc',true),
 ('10000000-0000-0000-0000-000000000007','Outras despesas','DESPESA','#64748b',true),
 ('10000000-0000-0000-0000-000000000008','Salário','RECEITA','#15765b',true),
 ('10000000-0000-0000-0000-000000000009','Outras receitas','RECEITA','#287c75',true);
