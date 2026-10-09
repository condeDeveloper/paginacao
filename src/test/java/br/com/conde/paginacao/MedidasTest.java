package br.com.conde.paginacao;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os testes das TABELAS que o medidor imprime.
 *
 * <p>Toda frase do README que e um numero esta aqui como afirmacao. Texto nao quebra
 * quando deixa de ser verdade; teste quebra.
 */
class MedidasTest {

    @Test
    @DisplayName("em toda linha o otimo e piso e a coluna da LRU bate com a distancia de pilha")
    void todaLinhaFecha() {
        List<Medidas.LinhaDefinicao> linhas = Medidas.contraOOtimo();

        assertThat(linhas).hasSize(10);
        assertThat(linhas).allSatisfy(l -> {
            assertThat(l.otimoEhPiso()).as(l.traco()).isTrue();
            assertThat(l.lruBateComOJuiz()).as(l.traco()).isTrue();
        });
    }

    @Test
    @DisplayName("no laco a LRU falha em todo acesso e a MRU empata com o otimo")
    void oLacoNaTabela() {
        Medidas.LinhaDefinicao laco = Medidas.contraOOtimo().stream()
                .filter(l -> l.traco().equals("laco de 10"))
                .findFirst()
                .orElseThrow();

        assertThat(laco.politicas().get("LRU")).isEqualTo(laco.acessos());
        assertThat(laco.politicas().get("FIFO")).isEqualTo(laco.acessos());
        assertThat(laco.politicas().get("CLOCK")).isEqualTo(laco.acessos());
        assertThat(laco.politicas().get("MRU")).isEqualTo(laco.otimo());

        // e o sorteio puro ganha das tres
        assertThat(laco.politicas().get("ALEATORIA")).isLessThan(laco.politicas().get("LRU"));
    }

    @Test
    @DisplayName("a anomalia de Belady aparece na FIFO e na CLOCK de 3 para 4 molduras")
    void aAnomaliaNaTabela() {
        List<Medidas.LinhaAnomalia> linhas = Medidas.anomaliaNoTracoDeBelady();

        assertThat(linhas).hasSize(8 * 4);

        List<Medidas.LinhaAnomalia> comAnomalia = linhas.stream().filter(Medidas.LinhaAnomalia::anomalia).toList();

        assertThat(comAnomalia).hasSize(2);
        assertThat(comAnomalia).allSatisfy(l -> {
            assertThat(l.molduras()).isEqualTo(3);
            assertThat(l.faltas()).isEqualTo(9);
            assertThat(l.aMais()).isEqualTo(10);
        });
        assertThat(comAnomalia).extracting(Medidas.LinhaAnomalia::politica).containsExactly("FIFO", "CLOCK");
    }

    /**
     * O ACHADO QUE ME CORRIGIU: quem mais sofre a anomalia nao e a FIFO, e o sorteio
     * puro, por uma ordem de grandeza.
     *
     * <p>Eu pus a FIFO no repositorio como "a politica da anomalia de Belady", porque
     * e assim que todo livro conta. Numa varredura de quinhentos tracos sorteados a
     * FIFO sofre em quatro, e a politica ALEATORIA sofre em dezenas, com um salto
     * varias vezes maior.
     */
    @Test
    @DisplayName("o sorteio puro sofre a anomalia muito mais que a FIFO")
    void oSorteioSofreMaisQueAFifo() {
        List<Medidas.LinhaCaca> linhas = Medidas.cacarAnomalias(500, 8, 60, 6);

        Medidas.LinhaCaca fifo = achar(linhas, "FIFO");
        Medidas.LinhaCaca sorteio = achar(linhas, "ALEATORIA");

        assertThat(fifo.comAnomalia()).isGreaterThan(0);
        assertThat(sorteio.comAnomalia()).isGreaterThan(10 * fifo.comAnomalia());
        assertThat(sorteio.piorSalto()).isGreaterThan(fifo.piorSalto());

        // e as de pilha nao sofrem nunca
        assertThat(achar(linhas, "LRU").comAnomalia()).isZero();
        assertThat(achar(linhas, "MRU").comAnomalia()).isZero();
        assertThat(achar(linhas, "OTIMO+LRU").comAnomalia()).isZero();
    }

    /**
     * E o segundo lado do mesmo achado: a anomalia some quando o traco fica longo.
     *
     * <p>Com dois mil acessos a doze paginas, NENHUMA politica sofre a anomalia em
     * quinhentos tracos. Ela precisa de traco curto em relacao ao numero de paginas.
     */
    @Test
    @DisplayName("a anomalia some em tracos longos")
    void aAnomaliaSomeEmTracosLongos() {
        List<Medidas.LinhaCaca> curtos = Medidas.cacarAnomalias(200, 8, 60, 6);
        List<Medidas.LinhaCaca> longos = Medidas.cacarAnomalias(200, 12, 2_000, 6);

        assertThat(curtos.stream().mapToInt(Medidas.LinhaCaca::comAnomalia).sum()).isGreaterThan(0);
        assertThat(longos.stream().mapToInt(Medidas.LinhaCaca::comAnomalia).sum()).isZero();
    }

    @Test
    @DisplayName("so LRU, MRU e o otimo com desempate tem a propriedade de inclusao em todos os pares")
    void aInclusaoNaTabela() {
        List<Medidas.LinhaInclusao> linhas = Medidas.inclusaoPorPolitica(8);

        assertThat(linhas).hasSize(8);

        for (String nome : new String[] {"LRU", "MRU", "OTIMO+LRU"}) {
            Medidas.LinhaInclusao l = achar(linhas, nome, Medidas.LinhaInclusao::politica);

            assertThat(l.paresQueValem()).as(nome).isEqualTo(l.pares());
            assertThat(l.primeiraFalha()).as(nome).isEmpty();
        }

        for (String nome : new String[] {"OTIMO", "FIFO", "CLOCK", "LFU", "ALEATORIA"}) {
            Medidas.LinhaInclusao l = achar(linhas, nome, Medidas.LinhaInclusao::politica);

            assertThat(l.paresQueValem()).as(nome).isLessThan(l.pares());
            assertThat(l.primeiraFalha()).as(nome).isNotEmpty();
        }
    }

    @Test
    @DisplayName("a FIFO e a CLOCK quebram a inclusao no mesmo ponto do traco de Belady")
    void aFifoEaClockQuebramJuntas() {
        List<Medidas.LinhaInclusao> linhas = Medidas.inclusaoPorPolitica(8);

        assertThat(achar(linhas, "FIFO", Medidas.LinhaInclusao::politica).primeiraFalha())
                .isEqualTo(achar(linhas, "CLOCK", Medidas.LinhaInclusao::politica).primeiraFalha())
                .startsWith("Belady");
    }

    @Test
    @DisplayName("no laco, so a MRU e o otimo saem do buraco")
    void aTabelaDoLaco() {
        List<Medidas.LinhaLaco> linhas = Medidas.oLaco(4);

        assertThat(linhas).hasSize(5);

        for (Medidas.LinhaLaco l : linhas) {
            assertThat(l.politicas().get("MRU")).as("laco de %d", l.paginasDoLaco()).isEqualTo(l.otimo());

            if (l.paginasDoLaco() > l.molduras())
                assertThat(l.politicas().get("LRU"))
                        .as("laco de %d", l.paginasDoLaco())
                        .isEqualTo(l.acessos());
        }
    }

    /**
     * O OUTRO ACHADO QUE ME CORRIGIU: a CLOCK quase nao economiza OPERACOES.
     *
     * <p>Eu escrevi que ela existe para nao pagar a varredura da LRU. Nos tracos
     * deste repositorio ela gasta cerca de oitenta por cento das operacoes da LRU, e
     * nao uma fracao pequena. A vantagem real dela nao esta no numero de operacoes:
     * esta em um bit por moldura ser uma coisa que o hardware liga de graca, e disso
     * a contagem de operacoes nao sabe nada.
     */
    @Test
    @DisplayName("a CLOCK economiza pouca operacao, e a aleatoria e a mais barata de todas")
    void oCustoNaTabela() {
        List<Medidas.LinhaCusto> linhas = Medidas.oCusto(4);

        Medidas.LinhaCusto lru = achar(linhas, "LRU", Medidas.LinhaCusto::politica);
        Medidas.LinhaCusto clock = achar(linhas, "CLOCK", Medidas.LinhaCusto::politica);
        Medidas.LinhaCusto sorteio = achar(linhas, "ALEATORIA", Medidas.LinhaCusto::politica);
        Medidas.LinhaCusto fifo = achar(linhas, "FIFO", Medidas.LinhaCusto::politica);

        assertThat(clock.operacoes()).isBetween((long) (lru.operacoes() * 0.7), lru.operacoes());
        assertThat(sorteio.operacoes()).isLessThan(lru.operacoes() / 3);
        assertThat(fifo.operacoes()).isLessThan(lru.operacoes() / 2);

        // e o sorteio, que e o mais barato, nao e o pior em faltas
        assertThat(sorteio.faltas()).isLessThan(achar(linhas, "MRU", Medidas.LinhaCusto::politica).faltas());
    }

    private static Medidas.LinhaCaca achar(List<Medidas.LinhaCaca> linhas, String nome) {
        return linhas.stream().filter(l -> l.politica().equals(nome)).findFirst().orElseThrow();
    }

    private static <T> T achar(List<T> linhas, String nome, java.util.function.Function<T, String> campo) {
        return linhas.stream().filter(l -> campo.apply(l).equals(nome)).findFirst().orElseThrow();
    }
}
