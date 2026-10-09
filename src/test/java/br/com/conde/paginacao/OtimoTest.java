package br.com.conde.paginacao;

import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O OTIMO e o juiz, entao ele e a coisa deste repositorio que mais precisa ser
 * julgada.
 *
 * <p>Tres conferencias, e elas nao se parecem: o teorema de Belady diz que ele e
 * piso de todas as politicas; a implementacao rapida tem de bater com a ingenua, que
 * varre o resto do traco; e os dois desempates tem de dar o mesmo numero de faltas,
 * porque os dois sao otimos.
 */
class OtimoTest {

    @Test
    @DisplayName("o otimo e piso de toda politica, em todo traco e toda quantidade de molduras")
    void oOtimoEhPisoDeTodas() {
        for (Traco traco : Traco.todos().values())
            for (int k = 1; k <= Math.min(10, traco.distintas()); k++) {
                long otimo = Simulador.rodar(new Otimo(), traco, k).faltas();

                for (Supplier<Politica> p : Politicas.todas()) {
                    long faltas = Simulador.rodar(p.get(), traco, k).faltas();

                    assertThat(faltas)
                            .as("%s em %s com %d molduras", p.get().nome(), traco.nome(), k)
                            .isGreaterThanOrEqualTo(otimo);
                }
            }
    }

    @Test
    @DisplayName("e tambem em tracos sorteados de feitios bem diferentes")
    void oOtimoEhPisoEmTracosSorteados() {
        for (long semente = 1; semente <= 40; semente++) {
            Traco traco = Traco.uniforme(300, 9, semente);

            for (int k = 1; k <= 6; k++) {
                long otimo = Simulador.rodar(new Otimo(), traco, k).faltas();

                for (Supplier<Politica> p : Politicas.todas())
                    assertThat(Simulador.rodar(p.get(), traco, k).faltas())
                            .as("%s na semente %d com %d molduras", p.get().nome(), semente, k)
                            .isGreaterThanOrEqualTo(otimo);
            }
        }
    }

    /**
     * A implementacao RAPIDA e a INGENUA dao o mesmo numero de faltas.
     *
     * <p>A ingenua varre o resto do traco atras de cada pagina residente; a rapida
     * guarda a proxima ocorrencia de cada instante numa passada de tras para a
     * frente. Duas contas que nao se parecem chegando ao mesmo numero valem mais do
     * que uma conta conferida duas vezes.
     */
    @Test
    @DisplayName("o otimo rapido bate com o otimo ingenuo em todo traco")
    void oRapidoBateComOIngenuo() {
        for (Traco traco : Traco.todos().values())
            for (int k = 1; k <= Math.min(8, traco.distintas()); k++)
                assertThat(Simulador.rodar(new Otimo.Ingenuo(), traco, k).faltas())
                        .as("%s com %d molduras", traco.nome(), k)
                        .isEqualTo(Simulador.rodar(new Otimo(), traco, k).faltas());
    }

    /**
     * E o ingenuo paga muito mais pela MESMA resposta, de um jeito que eu nao tinha
     * previsto: o preco dele depende de quao ESPALHADO e o traco.
     *
     * <p>Ele varre o traco a frente ate achar a proxima ocorrencia de cada pagina
     * residente. Com poucas paginas essa ocorrencia esta logo ali e ele custa dez
     * vezes o rapido; com quatrocentas paginas ela esta longe e ele custa cento e
     * dezoito vezes. O rapido quase nao se mexe.
     */
    @Test
    @DisplayName("o ingenuo custa mais quanto mais espalhado for o traco, e o rapido quase nao se mexe")
    void oIngenuoPagaMuitoMais() {
        long razaoPoucasPaginas = razaoDoJuiz(20);
        long razaoMuitasPaginas = razaoDoJuiz(400);

        assertThat(razaoPoucasPaginas).isGreaterThan(5);
        assertThat(razaoMuitasPaginas).isGreaterThan(10 * razaoPoucasPaginas);

        // e o rapido cresce pouco entre os dois extremos
        long rapidoPoucas = Simulador.rodar(new Otimo(), Traco.uniforme(2_000, 20, 1), 4).operacoes();
        long rapidoMuitas = Simulador.rodar(new Otimo(), Traco.uniforme(2_000, 400, 1), 4).operacoes();

        assertThat(rapidoMuitas).isLessThan(2 * rapidoPoucas);
    }

    private static long razaoDoJuiz(int paginas) {
        Traco traco = Traco.uniforme(2_000, paginas, 1);

        Simulador.Resultado rapido = Simulador.rodar(new Otimo(), traco, 4);
        Simulador.Resultado ingenuo = Simulador.rodar(new Otimo.Ingenuo(), traco, 4);

        assertThat(ingenuo.faltas()).as("faltas com %d paginas", paginas).isEqualTo(rapido.faltas());

        return ingenuo.operacoes() / rapido.operacoes();
    }

    /**
     * O ACHADO QUE ME CORRIGIU: os dois DESEMPATES do otimo dao exatamente o mesmo
     * numero de faltas, e so um deles e politica de pilha.
     *
     * <p>Quando duas paginas residentes nunca mais vao ser pedidas, tanto faz qual
     * sai: nenhuma das duas causa falta nenhuma depois. A conta de faltas nao
     * distingue as duas escolhas. O CONJUNTO RESIDENTE distingue, e e sobre o
     * conjunto que a propriedade de inclusao fala.
     */
    @Test
    @DisplayName("os dois desempates do otimo dao as mesmas faltas")
    void osDoisDesempatesDaoAsMesmasFaltas() {
        for (Traco traco : Traco.todos().values())
            for (int k = 1; k <= Math.min(10, traco.distintas()); k++)
                assertThat(Simulador.rodar(new Otimo(true), traco, k).faltas())
                        .as("%s com %d molduras", traco.nome(), k)
                        .isEqualTo(Simulador.rodar(new Otimo(false), traco, k).faltas());
    }

    @Test
    @DisplayName("a proxima ocorrencia e calculada certo, inclusive no fim do traco")
    void proximasOcorrencias() {
        Traco traco = new Traco("teste", new int[] {1, 2, 1, 3, 2});
        int[] proxima = Otimo.proximasOcorrencias(traco);

        assertThat(proxima[0]).isEqualTo(2);
        assertThat(proxima[1]).isEqualTo(4);
        assertThat(proxima[2]).isEqualTo(Otimo.NUNCA);
        assertThat(proxima[3]).isEqualTo(Otimo.NUNCA);
        assertThat(proxima[4]).isEqualTo(Otimo.NUNCA);
    }

    @Test
    @DisplayName("com molduras suficientes para todas as paginas, so sobram as faltas obrigatorias")
    void soAsObrigatorias() {
        for (Traco traco : Traco.todos().values()) {
            int k = traco.distintas();

            for (Supplier<Politica> p : Politicas.comOtimo())
                assertThat(Simulador.rodar(p.get(), traco, k).faltas())
                        .as("%s em %s", p.get().nome(), traco.nome())
                        .isEqualTo(traco.distintas());
        }
    }
}
