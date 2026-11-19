ALTER TABLE usuario ADD COLUMN lancamentos_experimentais INTEGER NOT NULL DEFAULT 0;
ALTER TABLE verificacao_email ADD COLUMN recuperacao BOOLEAN NOT NULL DEFAULT FALSE;
CREATE UNIQUE INDEX verificacao_recuperacao_usuario ON verificacao_email (usuario_id) WHERE recuperacao;

CREATE TABLE aviso_exclusao (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expira_em TIMESTAMPTZ NOT NULL
);
