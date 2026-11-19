package br.com.finai.service;

import br.com.finai.model.*;
import br.com.finai.repository.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class CotaIaServiceTest {
    @Test
    void limitaDiaEMes() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        UsoIaRepository usos = mock(UsoIaRepository.class);
        Usuario u = new Usuario();
        when(usuarios.bloquearParaAtualizacao(u.id)).thenReturn(Optional.of(u));
        CotaIaService service = new CotaIaService(usuarios, usos);
        UsoIa uso = new UsoIa();
        when(usos.findByUsuarioIdAndDia(eq(u.id), any())).thenReturn(Optional.of(uso));
        when(usos.findByUsuarioIdAndDiaBetween(eq(u.id), any(), any())).thenReturn(List.of(uso));
        service.reservar(u.id);
        assertThat(uso.quantidade).isEqualTo(1);
        uso.quantidade = 60;
        assertThatThrownBy(() -> service.reservar(u.id)).isInstanceOf(RegraNegocioException.class);
        UsoIa anterior = new UsoIa();
        anterior.quantidade = 399;
        uso.quantidade = 1;
        when(usos.findByUsuarioIdAndDiaBetween(eq(u.id), any(), any())).thenReturn(List.of(uso, anterior));
        assertThatThrownBy(() -> service.reservar(u.id)).isInstanceOf(RegraNegocioException.class);
    }
}
