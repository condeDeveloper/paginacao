package br.com.conde.paginacao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Um TRACO: a sequencia de paginas que o programa pede, na ordem em que pede.
 *
 * <p>E a unica entrada do problema. Nao existe tempo, nao existe disco, nao existe
 * processo: existe uma lista de numeros de pagina e uma quantidade de molduras na
 * memoria. Toda politica de substituicao e uma funcao que, quando a memoria esta
 * cheia e chega uma pagina que nao esta la, escolhe qual sai.
 *
 * <p>Os tracos deste arquivo nao sao decorativos. Cada um existe para separar uma
 * politica da outra, e o comentario de cada um diz qual.
 */
public record Traco(String nome, int[] paginas) {

    public Traco {
        paginas = paginas.clone();
    }

    @Override
    public int[] paginas() {
        return paginas.clone();
    }

    public int tamanho() {
        return paginas.length;
    }

    public int em(int i) {
        return paginas[i];
    }

    /** Quantas paginas DIFERENTES aparecem. Acima disso, mais molduras nao mudam nada. */
    public int distintas() {
        return (int) Arrays.stream(paginas).distinct().count();
    }

    /** O mesmo traco repetido, para medir regime permanente em vez de aquecimento. */
    public Traco repetido(int vezes) {
        int[] saida = new int[paginas.length * vezes];

        for (int i = 0; i < vezes; i++) System.arraycopy(paginas, 0, saida, i * paginas.length, paginas.length);

        return new Traco(nome + " x" + vezes, saida);
    }

    @Override
    public String toString() {
        return nome + " (" + paginas.length + " acessos, " + distintas() + " paginas)";
    }

    // ───────────────────────────── os tracos ─────────────────────────────

    /**
     * O TRACO DE BELADY, de 1969, que e o motivo deste repositorio existir.
     *
     * <p>Com TRES molduras a FIFO falha nove vezes. Com QUATRO, dez. Dar mais memoria
     * ao programa piora o resultado, e isso nao e bug de implementacao: e uma
     * propriedade da FIFO.
     *
     * <p>Doze acessos a cinco paginas. O caso inteiro cabe numa linha.
     */
    public static Traco deBelady() {
        return new Traco("Belady", new int[] {1, 2, 3, 4, 1, 2, 5, 1, 2, 3, 4, 5});
    }

    /**
     * O LACO: as paginas 0..n-1 em ordem, repetidas.
     *
     * <p>E o traco que derruba a LRU. Quando o laco tem uma pagina a MAIS do que
     * cabe na memoria, a LRU joga fora exatamente a pagina que vai ser pedida
     * agora, toda vez, e falha em cem por cento dos acessos.
     *
     * <p>Nao e um caso construido para humilhar a LRU: percorrer um vetor maior que o
     * cache num laco e a coisa mais comum que um programa faz.
     */
    public static Traco laco(int paginas, int voltas) {
        int[] saida = new int[paginas * voltas];

        for (int i = 0; i < saida.length; i++) saida[i] = i % paginas;

        return new Traco("laco de " + paginas, saida);
    }

    /** Acesso sequencial sem repeticao: nenhuma politica consegue fazer nada. */
    public static Traco sequencial(int paginas) {
        int[] saida = new int[paginas];

        for (int i = 0; i < paginas; i++) saida[i] = i;

        return new Traco("sequencial de " + paginas, saida);
    }

    /** Uniforme: toda pagina com a mesma chance, que e o pior caso para qualquer previsao. */
    public static Traco uniforme(int acessos, int paginas, long semente) {
        Sorteio sorteio = new Sorteio(semente);
        int[] saida = new int[acessos];

        for (int i = 0; i < acessos; i++) saida[i] = sorteio.ate(paginas);

        return new Traco("uniforme de " + paginas, saida);
    }

    /**
     * ZIPF: poucas paginas muito pedidas e muitas quase nunca.
     *
     * <p>E o feitio que programa de verdade tem, e e onde a LRU ganha das outras: as
     * paginas quentes ficam, as frias entram e saem.
     */
    public static Traco zipf(int acessos, int paginas, double expoente, long semente) {
        Sorteio sorteio = new Sorteio(semente);

        double[] acumulado = new double[paginas];
        double soma = 0;

        for (int i = 0; i < paginas; i++) {
            soma += 1.0 / Math.pow(i + 1, expoente);
            acumulado[i] = soma;
        }

        int[] saida = new int[acessos];

        for (int i = 0; i < acessos; i++) {
            double alvo = sorteio.fracao() * soma;
            int baixo = 0;
            int alto = paginas - 1;

            while (baixo < alto) {
                int meio = (baixo + alto) / 2;

                if (acumulado[meio] < alvo) baixo = meio + 1;
                else alto = meio;
            }

            saida[i] = baixo;
        }

        return new Traco("zipf " + expoente + " de " + paginas, saida);
    }

    /**
     * CONJUNTO DE TRABALHO: o programa fica um tempo num punhado de paginas, depois
     * muda de punhado.
     *
     * <p>E o traco em que as politicas que olham o passado recente valem a pena, e
     * tambem o que mostra o custo da mudanca de fase.
     */
    public static Traco conjuntoDeTrabalho(int fases, int porFase, int tamanhoDoConjunto, int paginas, long semente) {
        Sorteio sorteio = new Sorteio(semente);
        int[] saida = new int[fases * porFase];
        int k = 0;

        for (int f = 0; f < fases; f++) {
            int base = sorteio.ate(Math.max(1, paginas - tamanhoDoConjunto));

            for (int i = 0; i < porFase; i++) saida[k++] = base + sorteio.ate(tamanhoDoConjunto);
        }

        return new Traco("conjunto de trabalho de " + tamanhoDoConjunto, saida);
    }

    /**
     * O LACO COM RUIDO: um laco grande com algumas paginas sorteadas no meio.
     *
     * <p>Serve para separar a LRU pura da CLOCK: o ruido mexe no bit de referencia
     * sem mexer na ordem de uso.
     */
    public static Traco lacoComRuido(int paginas, int voltas, int ruidoACada, long semente) {
        Sorteio sorteio = new Sorteio(semente);
        List<Integer> saida = new ArrayList<>();

        for (int v = 0; v < voltas; v++)
            for (int i = 0; i < paginas; i++) {
                saida.add(i);

                if (ruidoACada > 0 && saida.size() % ruidoACada == 0) saida.add(paginas + sorteio.ate(paginas));
            }

        return new Traco("laco de " + paginas + " com ruido", saida.stream().mapToInt(Integer::intValue).toArray());
    }

    /** Os tracos do repositorio, com nome, na ordem em que as tabelas os mostram. */
    public static Map<String, Traco> todos() {
        Map<String, Traco> mapa = new LinkedHashMap<>();

        for (Traco t : List.of(
                deBelady(),
                sequencial(50),
                laco(10, 20),
                laco(11, 20),
                lacoComRuido(10, 20, 7, 1),
                uniforme(2_000, 20, 1),
                uniforme(2_000, 50, 2),
                zipf(2_000, 50, 1.0, 3),
                zipf(2_000, 50, 1.5, 4),
                conjuntoDeTrabalho(20, 100, 6, 40, 5)))
            mapa.put(t.nome(), t);

        return mapa;
    }
}
