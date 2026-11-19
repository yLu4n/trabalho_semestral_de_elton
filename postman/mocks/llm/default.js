const http = require("http");
// Simula somente o contrato utilizado pelo adaptador. Nunca encaminha chamadas.
let modo = "valido";
http
  .createServer(async (req, res) => {
    const reply = (status, value) => {
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
    if (modo === "timeout")
      await new Promise((resolve) => setTimeout(resolve, 9000));
    let body = req.body;
    if (!body) {
      let raw = "";
      for await (const chunk of req) raw += chunk;
      body = JSON.parse(raw);
    }
    if (
      !Array.isArray(body.messages) ||
      body.store !== false ||
      body.response_format?.type !== "json_object"
    )
      return reply(400, { error: "Contrato inválido" });
    const dados = JSON.parse(body.messages[1].content);
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
      const sorted = [...dados.categorias].sort(
        (a, b) =>
          Math.abs(Number(b.atual) - Number(b.anterior)) -
          Math.abs(Number(a.atual) - Number(a.anterior)),
      );
      const diff = dados.categorias.reduce(
        (sum, c) => sum + Number(c.atual) - Number(c.anterior),
        0,
      );
      content = {
        destaques: sorted.slice(0, 3).map((c) => c.categoria),
        tendencia: diff > 0 ? "ALTA" : diff < 0 ? "QUEDA" : "ESTAVEL",
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
