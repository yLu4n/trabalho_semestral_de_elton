package br.com.finai.adapter;

import br.com.finai.repository.RegistroInferenciaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AdaptadorLlmTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final RegistroInferenciaRepository auditoria = mock(RegistroInferenciaRepository.class);

    @Test
    void ollamaRecebeJsonSemCredencialDaOpenAi() throws Exception {
        var autenticacao = new AtomicReference<String>();
        var entrada = new AtomicReference<JsonNode>();
        var servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try {
            servidor.createContext("/v1/chat/completions", pedido -> {
                try (pedido) {
                    autenticacao.set(pedido.getRequestHeaders().getFirst("Authorization"));
                    entrada.set(mapper.readTree(pedido.getRequestBody()));
                    var bytes = """
                            {"choices":[{"finish_reason":"stop","message":{"content":"{\\"valor\\":38}"}}]}
                            """.getBytes(StandardCharsets.UTF_8);
                    pedido.sendResponseHeaders(200, bytes.length);
                    pedido.getResponseBody().write(bytes);
                }
            });
            servidor.start();
            var resposta = adaptador("http://127.0.0.1:" + servidor.getAddress().getPort() + "/v1/chat/completions",
                    "qwen2.5:3b").gerar("INTERPRETACAO", "Retorne JSON.", Map.of("texto", "Almocei por 38"), false);
            assertThat(resposta.path("valor").asInt()).isEqualTo(38);
            assertThat(autenticacao.get()).isNull();
            assertThat(entrada.get().has("store")).isFalse();
            assertThat(entrada.get().at("/response_format/type").asText()).isEqualTo("json_schema");
            verify(auditoria).save(argThat(registro -> registro.resultado.equals("RECEBIDA")));
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void modoOllamaRecusaEnderecoRemotoEModeloDeNuvem() {
        assertThatThrownBy(() -> adaptador("https://example.test/v1/chat/completions", "qwen2.5:3b")
                .gerar("INTERPRETACAO", "Retorne JSON.", Map.of(), false)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> adaptador("http://127.0.0.1:11434/v1/chat/completions", "modelo:cloud")
                .gerar("INTERPRETACAO", "Retorne JSON.", Map.of(), false)).isInstanceOf(IllegalStateException.class);
    }

    private AdaptadorLlm adaptador(String endpoint, String modelo) {
        return new AdaptadorLlm(mapper, auditoria, "ollama", endpoint, "credencial-ficticia", modelo,
                BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
