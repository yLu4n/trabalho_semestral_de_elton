const http = require("http");
// Simula somente o contrato utilizado pelo adaptador. Nunca encaminha chamadas.
let modo = "valido";
http
  .createServer(async (req, res) => {
    const reply = (status, value) => {
      if (res.destroyed || res.writableEnded) return;
      res.writeHead(status, { "Content-Type": "application/json" });
      res.end(JSON.stringify(value));
    };
    const rota = new URL(req.url, "http://localhost").pathname.replace(/^\/llm(?=\/)/, "");
    if (rota.startsWith("/controle/")) {
      modo = rota.split("/").pop();
      return reply(200, { modo });
    }
    if (req.method !== "POST" || rota !== "/v1/chat/completions")
      return reply(501, { error: "Endpoint não modelado" });
    if (modo === "erro")
      return reply(503, { error: { message: "Falha transitória simulada" } });
    if (modo === "cota")
      return reply(429, { error: { message: "Cota simulada" } });
    let body = req.body;
    if (!body) {
      let raw = "";
      try {
        for await (const chunk of req) raw += chunk;
        body = JSON.parse(raw);
      } catch {
        return reply(400, { error: "Corpo da requisição inválido" });
      }
    }
    if (
      !Array.isArray(body.messages) ||
      body.store !== false ||
      body.response_format?.type !== "json_object"
    )
      return reply(400, { error: "Contrato inválido" });
    if (modo === "timeout") {
      await new Promise((resolve) => setTimeout(resolve, 9000));
      if (res.destroyed || res.writableEnded) return;
    }
    let dados;
    try {
      dados = JSON.parse(body.messages[1]?.content);
    } catch {
      return reply(400, { error: "Mensagem inválida" });
    }
    let content;
    if (dados.texto) {
      const c = dados.categorias.find((c) => c.tipo === "DESPESA");
      content = {
        campos: {
          tipo: "DESPESA",
          valor: 38,
          data: dados.hoje,
          categoriaId: c?.id,
          descricao: "Almoço de teste",
          estabelecimento: null,
          formaPagamento: "Pix",
        },
        confianca: {
          tipo: 0.99,
          valor: 0.99,
          data: 0.95,
          categoriaId: 0.95,
          descricao: 0.9,
          estabelecimento: 0,
          formaPagamento: 0.9,
        },
      };
      if (modo === "invalido")
        content.campos.categoriaId = "00000000-0000-0000-0000-000000000099";
      if (modo === "baixa-confianca") {
        content.confianca.valor = 0.5;
        content.confianca.data = 0.4;
      }
    } else {
      content = {
        texto: `O maior destaque apresentou ${dados.direcaoDoMaiorDestaque}, com tendencia de ${dados.tendenciaDoTotal}.`,
      };
    }
    reply(200, {
      id: "chatcmpl-simulado",
      model: "llm-simulado",
      choices: [
        {
          finish_reason: "stop",
          message: { role: "assistant", content: JSON.stringify(content) },
        },
      ],
      usage: { prompt_tokens: 100, completion_tokens: 80 },
    });
  })
  .listen(Number(process.env.PORT || 4011), "127.0.0.1");
