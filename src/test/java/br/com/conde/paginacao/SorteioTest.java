package br.com.conde.paginacao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SorteioTest {

    @Test
    @DisplayName("a mesma semente da a mesma sequencia, e e por isso que o sorteio esta escrito aqui")
    void mesmaSementeMesmaSequencia() {
        Sorteio um = new Sorteio(7);
        Sorteio outro = new Sorteio(7);

        for (int i = 0; i < 1_000; i++) assertThat(um.proximo()).isEqualTo(outro.proximo());
    }

    @Test
    @DisplayName("sementes diferentes dao sequencias diferentes")
    void sementesDiferentes() {
        Sorteio um = new Sorteio(1);
        Sorteio outro = new Sorteio(2);

        int iguais = 0;

        for (int i = 0; i < 1_000; i++) if (um.proximo() == outro.proximo()) iguais++;

        assertThat(iguais).isLessThan(5);
    }

    /**
     * O DESLOCAMENTO de trinta e dois bits nao e enfeite.
     *
     * <p>Os bits de baixo de um congruente linear tem periodo curto: o ultimo bit
     * alterna par e impar a cada passo. Um traco de paginas sorteado com esse bit
     * seria um ziguezague entre duas paginas, e toda medida deste repositorio
     * mediria o ziguezague em vez da politica.
     */
    @Test
    @DisplayName("os bits de baixo alternariam a cada passo, e por isso o sorteio usa os de cima")
    void osBitsDeBaixoNaoServiriam() {
        Sorteio sorteio = new Sorteio(1);
        int trocas = 0;
        int anterior = -1;

        for (int i = 0; i < 1_000; i++) {
            int atual = sorteio.ate(2);

            if (anterior >= 0 && atual != anterior) trocas++;

            anterior = atual;
        }

        // com os bits de baixo isso daria 999 trocas, uma por passo
        assertThat(trocas).isBetween(400, 600);
    }

    @Test
    @DisplayName("a fracao fica entre zero e um e cobre o intervalo")
    void fracaoCobreOIntervalo() {
        Sorteio sorteio = new Sorteio(3);
        double menor = 1;
        double maior = 0;

        for (int i = 0; i < 10_000; i++) {
            double f = sorteio.fracao();

            assertThat(f).isBetween(0.0, 1.0);

            menor = Math.min(menor, f);
            maior = Math.max(maior, f);
        }

        assertThat(menor).isLessThan(0.01);
        assertThat(maior).isGreaterThan(0.99);
    }

    @Test
    @DisplayName("o limite precisa ser positivo")
    void limitePositivo() {
        assertThatThrownBy(() -> new Sorteio(1).ate(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("semente zero nao trava o gerador")
    void sementeZero() {
        Sorteio sorteio = new Sorteio(0);

        assertThat(sorteio.proximo()).isNotEqualTo(sorteio.proximo());
    }
}
