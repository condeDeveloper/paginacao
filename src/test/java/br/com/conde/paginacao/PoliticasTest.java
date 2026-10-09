package br.com.conde.paginacao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PoliticasTest {

    /**
     * A ANOMALIA DE BELADY, no traco de 1969, em duas linhas.
     *
     * <p>Doze acessos, cinco paginas. Com TRES molduras a FIFO falha nove vezes; com
     * QUATRO, dez. Dar mais memoria ao programa piorou o resultado.
     */
    @Test
    @DisplayName("a FIFO falha 9 vezes com 3 molduras e 10 com 4, no traco de Belady")
    void aAnomaliaDeBelady() {
        Traco traco = Traco.deBelady();

        assertThat(Simulador.rodar(new Politicas.Fifo(), traco, 3).faltas()).isEqualTo(9);
        assertThat(Simulador.rodar(new Politicas.Fifo(), traco, 4).faltas()).isEqualTo(10);

        // a LRU no mesmo traco so melhora
        assertThat(Simulador.rodar(new Politicas.Lru(), traco, 3).faltas()).isEqualTo(10);
        assertThat(Simulador.rodar(new Politicas.Lru(), traco, 4).faltas()).isEqualTo(8);
    }

    /**
     * O ACHADO PRINCIPAL: no LACO, a LRU falha em CEM POR CENTO dos acessos, e a MRU
     * empata com o otimo.
     *
     * <p>Com cinco paginas e quatro molduras, a LRU joga fora exatamente a pagina que
     * vai ser pedida no proximo acesso, toda vez. Nao e um caso construido para
     * humilhar a LRU: percorrer um vetor maior que o cache em circulo e a coisa mais
     * comum que um programa faz.
     */
    @Test
    @DisplayName("no laco com uma pagina a mais que as molduras, a LRU erra tudo e a MRU empata com o otimo")
    void oLacoDerrubaALru() {
        Traco traco = Traco.laco(5, 20);

        long otimo = Simulador.rodar(new Otimo(), traco, 4).faltas();
        long lru = Simulador.rodar(new Politicas.Lru(), traco, 4).faltas();
        long mru = Simulador.rodar(new Politicas.Mru(), traco, 4).faltas();
        long sorteio = Simulador.rodar(new Politicas.Aleatoria(7), traco, 4).faltas();

        assertThat(lru).isEqualTo(traco.tamanho());
        assertThat(mru).isEqualTo(otimo);
        assertThat(sorteio).isLessThan(lru);

        // a FIFO e a CLOCK caem no mesmo buraco
        assertThat(Simulador.rodar(new Politicas.Fifo(), traco, 4).faltas()).isEqualTo(traco.tamanho());
        assertThat(Simulador.rodar(new Politicas.Clock(), traco, 4).faltas()).isEqualTo(traco.tamanho());
    }

    @Test
    @DisplayName("e com o laco cabendo na memoria, todas empatam")
    void oLacoQueCabe() {
        Traco traco = Traco.laco(4, 20);

        for (var p : Politicas.comOtimo())
            assertThat(Simulador.rodar(p.get(), traco, 4).faltas())
                    .as(p.get().nome())
                    .isEqualTo(4);
    }

    /**
     * A POLUICAO DA LFU: ela guarda a contagem por pagina e nao a zera quando a
     * pagina sai, entao uma pagina muito pedida numa fase antiga fica na memoria para
     * sempre.
     *
     * <p>No traco de conjunto de trabalho, que muda de fase vinte vezes, isso custa
     * dez vezes mais faltas que a LRU.
     */
    @Test
    @DisplayName("a LFU e envenenada pela fase antiga no conjunto de trabalho")
    void aLfuEhEnvenenadaPelaFaseAntiga() {
        Traco traco = Traco.conjuntoDeTrabalho(20, 100, 6, 40, 5);

        long lru = Simulador.rodar(new Politicas.Lru(), traco, 8).faltas();
        long lfu = Simulador.rodar(new Politicas.Lfu(), traco, 8).faltas();

        assertThat(lfu).isGreaterThan(10 * lru);

        // e no traco em que a frequencia nao mente, ela ganha da LRU
        Traco zipf = Traco.zipf(2_000, 50, 1.5, 4);

        assertThat(Simulador.rodar(new Politicas.Lfu(), zipf, 8).faltas())
                .isLessThan(Simulador.rodar(new Politicas.Lru(), zipf, 8).faltas());
    }

    /**
     * A CLOCK e uma aproximacao da LRU, e a tabela mede o tamanho da aproximacao.
     *
     * <p>Nos tracos do repositorio ela fica a menos de tres por cento da LRU em
     * faltas, usando um bit por moldura em vez de uma ordem completa de uso.
     */
    @Test
    @DisplayName("a CLOCK fica perto da LRU em faltas")
    void aClockFicaPertoDaLru() {
        long lru = 0;
        long clock = 0;

        for (Traco traco : Traco.todos().values()) {
            lru += Simulador.rodar(new Politicas.Lru(), traco, 8).faltas();
            clock += Simulador.rodar(new Politicas.Clock(), traco, 8).faltas();
        }

        assertThat(clock).isBetween(lru, (long) (lru * 1.03));
    }

    @Test
    @DisplayName("o traco sequencial nao da chance a politica nenhuma")
    void oSequencialNaoDaChance() {
        Traco traco = Traco.sequencial(50);

        for (var p : Politicas.comOtimo())
            assertThat(Simulador.rodar(p.get(), traco, 8).faltas())
                    .as(p.get().nome())
                    .isEqualTo(50);
    }

    @Test
    @DisplayName("com uma moldura so, toda politica da a mesma resposta")
    void comUmaMolduraTodasEmpatam() {
        for (Traco traco : Traco.todos().values()) {
            long esperado = Simulador.rodar(new Otimo(), traco, 1).faltas();

            for (var p : Politicas.todas())
                assertThat(Simulador.rodar(p.get(), traco, 1).faltas())
                        .as("%s em %s", p.get().nome(), traco.nome())
                        .isEqualTo(esperado);
        }
    }

    @Test
    @DisplayName("o simulador nao aceita memoria de zero molduras")
    void zeroMolduras() {
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> Simulador.rodar(new Politicas.Fifo(), Traco.deBelady(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
