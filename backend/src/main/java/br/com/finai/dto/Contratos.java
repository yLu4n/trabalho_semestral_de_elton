package br.com.finai.dto;

import br.com.finai.model.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;

public final class Contratos {
    private Contratos() {
    }

    public record Cadastro(@NotBlank @Size(max = 100) String nome, @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String senha, @NotNull @Past LocalDate nascimento,
            @AssertTrue boolean aceitaPrivacidade) {
    }

    public record Login(@NotBlank @Email String email, @NotBlank @Size(max = 72) String senha) {
    }

    public record Senha(@NotBlank String senha) {
    }

    public record RecuperacaoSenha(@NotBlank @Email @Size(max = 254) String email) {
    }

    public record RedefinicaoSenha(@NotBlank @Size(max = 100) String token,
            @NotBlank @Size(min = 8, max = 72) String senha) {
    }

    public record Perfil(UUID id, String nome, String email, Papel papel, boolean emailVerificado) {
        public static Perfil de(Usuario u) {
            return new Perfil(u.id, u.nome, u.email, u.papel, u.emailVerificado);
        }
    }

    public record CategoriaEntrada(@NotBlank @Size(max = 80) String nome, @NotNull TipoLancamento tipo,
            @NotNull @Pattern(regexp = "#[0-9a-fA-F]{6}") String cor, boolean ativa) {
    }

    public record LancamentoEntrada(@NotNull TipoLancamento tipo,
            @NotNull @DecimalMin("0.01") @DecimalMax("1000000.00") @Digits(integer = 7, fraction = 2) BigDecimal valor,
            @NotNull LocalDate data, @NotNull UUID categoriaId, @NotBlank @Size(max = 280) String descricao,
            @Size(max = 120) String estabelecimento, @Size(max = 60) String formaPagamento) {
    }

    public record MetaEntrada(@NotNull UUID categoriaId, @NotNull @Pattern(regexp = "\\d{4}-\\d{2}") String mes,
            @NotNull @DecimalMin("0.01") @DecimalMax("1000000.00") @Digits(integer = 7, fraction = 2) BigDecimal limite) {
    }

    public record TextoEntrada(@NotBlank @Size(max = 280) String texto) {
    }

    public record Confirmacao(@AssertTrue boolean confirmado,
            @NotNull @jakarta.validation.Valid LancamentoEntrada lancamento) {
    }

    public record Filtro(LocalDate inicio, LocalDate fim, UUID categoriaId, TipoLancamento tipo, String texto) {
    }

    public record Totais(BigDecimal receitas, BigDecimal despesas, BigDecimal saldo) {
    }

    public record Pagina(List<Lancamento> itens, int pagina, int paginas, long quantidade, Totais totais) {
    }

    public record Distribuicao(UUID categoriaId, String categoria, String cor, BigDecimal valor,
            BigDecimal percentual) {
    }

    public record MesSerie(String mes, BigDecimal receitas, BigDecimal despesas) {
    }

    public record Painel(Totais totais, List<Distribuicao> distribuicao, List<MesSerie> serie,
            BigDecimal projecaoDespesas, String mesProjecao) {
    }

    public record MetaVista(UUID id, UUID categoriaId, String categoria, String cor, String mes, BigDecimal limite,
            BigDecimal consumido, BigDecimal percentual, String estado) {
    }

    public record SugestaoVista(UUID id, Map<String, Object> campos, Map<String, BigDecimal> confianca,
            List<String> pendentes, boolean conferirValor, boolean manual, String aviso) {
    }

    public record Variacao(String categoria, BigDecimal anterior, BigDecimal atual, BigDecimal diferenca) {
    }

    public record Diagnostico(String texto, String aviso, List<Variacao> variacoes) {
    }
}
