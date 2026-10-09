package br.com.conde.paginacao;

import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O SEGUNDO JUIZ: a distancia de pilha, que calcula as faltas da LRU para todos os
 * tamanhos de memoria numa passada so e sem simular nada.
 *
 * <p>Ele nao sabe o que e moldura, nao sabe o que e vitima e nao chama o simulador.
 * Para os dois estarem errados do mesmo jeito, dois caminhos que nao se parecem
 * teriam de errar igual.
 */
class JuizTest {

    @Test
    @DisplayName("a distancia de pilha bate com o simulador da LRU em todo traco e todo k")
    void aDistanciaBateComOSimulador() {
        for (Traco traco : Traco.todos().values()) {
            int maximo = Math.min(12, traco.distintas() + 2);
            long[] peloJuiz = Juiz.faltasDaLruPorDistancia(traco, maximo);

            for (int k = 1; k <= maximo; k++)
                assertThat(peloJuiz[k])
                        .as("%s com %d molduras", traco.nome(), k)
                        .isEqualTo(Simulador.rodar(new Politicas.Lru(), traco, k).faltas());
        }
    }

    @Test
    @DisplayName("e bate tambem em tracos sorteados de feitios bem diferentes")
    void aDistanciaBateEmTracosSorteados() {
        for (long semente = 1; semente <= 30; semente++)
            for (Traco traco : new Traco[] {
                    Traco.uniforme(400, 12, semente),
                    Traco.zipf(400, 12, 1.2, semente),
                    Traco.conjuntoDeTrabalho(8, 50, 4, 12, semente)}) {

                long[] peloJuiz = Juiz.faltasDaLruPorDistancia(traco, 10);

                for (int k = 1; k <= 10; k++)
                    assertThat(peloJuiz[k])
                            .as("%s na semente %d com %d molduras", traco.nome(), semente, k)
                            .isEqualTo(Simulador.rodar(new Politicas.Lru(), traco, k).faltas());
            }
    }

    @Test
    @DisplayName("as faltas da LRU nunca sobem quando se da mais memoria")
    void aLruNuncaPioraComMaisMemoria() {
        for (Traco traco : Traco.todos().values()) {
            long[] faltas = Juiz.faltasDaLruPorDistancia(traco, 12);

            for (int k = 2; k <= 12; k++)
                assertThat(faltas[k])
                        .as("%s de %d para %d molduras", traco.nome(), k - 1, k)
                        .isLessThanOrEqualTo(faltas[k - 1]);
        }
    }

    /**
     * O ACHADO QUE ME CORRIGIU, em forma de teste: o OTIMO com desempate ingenuo NAO
     * tem a propriedade de inclusao, e com desempate por LRU tem.
     *
     * <p>Os dois dao o mesmo numero de faltas. A diferenca esta no conjunto residente,
     * e so no caso em que duas paginas nunca mais vao ser pedidas e tanto faz qual
     * sai para a conta de faltas.
     */
    @Test
    @DisplayName("o desempate decide se o otimo e politica de pilha")
    void oDesempateDecideSeOOtimoEhDePilha() {
        Traco traco = Traco.laco(10, 20);

        assertThat(Juiz.inclusao(() -> new Otimo(false), traco, 5).vale()).isFalse();
        assertThat(Juiz.inclusao(() -> new Otimo(true), traco, 5).vale()).isTrue();
    }

    @Test
    @DisplayName("LRU, MRU e o otimo com desempate tem a propriedade de inclusao em todo par")
    void asDePilhaTemInclusao() {
        for (Supplier<Politica> p : java.util.List.<Supplier<Politica>>of(
                Politicas.Lru::new, Politicas.Mru::new, () -> new Otimo(true)))

            for (Traco traco : Traco.todos().values())
                for (int k = 1; k < Math.min(8, traco.distintas()); k++) {
                    Juiz.Inclusao i = Juiz.inclusao(p, traco, k);

                    assertThat(i.vale())
                            .as("%s em %s com %d molduras, falhou no acesso %d",
                                    i.politica(), traco.nome(), k, i.instante())
                            .isTrue();
                }
    }

    @Test
    @DisplayName("FIFO e CLOCK nao tem, e falham no mesmo ponto do traco de Belady")
    void aFifoEaClockNaoTemInclusao() {
        Traco traco = Traco.deBelady();

        Juiz.Inclusao fifo = Juiz.inclusao(Politicas.Fifo::new, traco, 3);
        Juiz.Inclusao clock = Juiz.inclusao(Politicas.Clock::new, traco, 3);

        assertThat(fifo.vale()).isFalse();
        assertThat(clock.vale()).isFalse();
        assertThat(fifo.instante()).isEqualTo(clock.instante());
    }

    /**
     * A ponte entre as duas propriedades: quem tem inclusao nao sofre a anomalia.
     *
     * <p>Se o conjunto residente com k molduras esta sempre contido no de k mais uma,
     * todo acerto com k tambem e acerto com k mais uma, e entao as faltas nao podem
     * subir. E o teorema, e o teste o confere nos tracos do repositorio.
     */
    @Test
    @DisplayName("quem tem inclusao nunca sofre a anomalia")
    void quemTemInclusaoNaoSofreAnomalia() {
        for (Supplier<Politica> p : java.util.List.<Supplier<Politica>>of(
                Politicas.Lru::new, Politicas.Mru::new, () -> new Otimo(true)))

            for (Traco traco : Traco.todos().values())
                for (Juiz.Anomalia a : Juiz.anomalias(p, traco, 8))
                    assertThat(a.anomalia())
                            .as("%s em %s de %d para %d molduras",
                                    a.politica(), traco.nome(), a.molduras(), a.molduras() + 1)
                            .isFalse();
    }
}
