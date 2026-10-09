package br.com.conde.paginacao;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * O OTIMO DE BELADY, de 1966, que e o JUIZ deste repositorio.
 *
 * <p>A regra e uma linha: joga fora a pagina cujo PROXIMO uso esta mais longe no
 * futuro. Se alguma pagina residente nunca mais for pedida, ela sai.
 *
 * <p>Isso e impossivel de implementar num sistema operacional de verdade, porque
 * exige ler o futuro. E e justamente por isso que ele serve de juiz e nao de
 * algoritmo: ele da o menor numero de faltas que QUALQUER politica poderia dar
 * naquele traco com aquelas molduras, e o teorema de Belady garante isso. Nenhuma
 * politica deste repositorio pode falhar menos que ele em nenhuma linha de nenhuma
 * tabela, e se alguma falhar, o erro esta na minha implementacao e nao no teorema.
 *
 * <p>A implementacao ingenua varre o resto do traco a cada falta, e custa o tamanho
 * do traco vezes as molduras. Esta aqui guarda, para cada instante, o indice da
 * PROXIMA ocorrencia daquela mesma pagina, calculado numa passada de tras para a
 * frente. Dai a escolha da vitima custa so uma varredura das molduras.
 */
public final class Otimo implements Politica {

    /** Nunca mais sera pedida: o maior futuro possivel. */
    public static final int NUNCA = Integer.MAX_VALUE;

    private final boolean desempataPorLru;

    private int[] proximaOcorrencia = new int[0];
    private int[] proximoUsoDaMoldura = new int[0];
    private int[] ultimoUso = new int[0];
    private long operacoes;

    /** O otimo com desempate pela ORDEM DAS MOLDURAS, que e o jeito ingenuo. */
    public Otimo() {
        this(false);
    }

    /**
     * @param desempataPorLru entre paginas com o mesmo futuro, joga fora a usada ha
     *                        mais tempo em vez da primeira moldura
     */
    public Otimo(boolean desempataPorLru) {
        this.desempataPorLru = desempataPorLru;
    }

    @Override
    public String nome() {
        return desempataPorLru ? "OTIMO+LRU" : "OTIMO";
    }

    @Override
    public void comecar(Traco traco, int molduras) {
        proximaOcorrencia = proximasOcorrencias(traco);
        proximoUsoDaMoldura = new int[molduras];
        ultimoUso = new int[molduras];

        Arrays.fill(proximoUsoDaMoldura, NUNCA);
        Arrays.fill(ultimoUso, -1);
    }

    @Override
    public void acertou(int moldura, int pagina, int instante) {
        operacoes++;
        proximoUsoDaMoldura[moldura] = proximaOcorrencia[instante];
        ultimoUso[moldura] = instante;
    }

    @Override
    public void carregou(int moldura, int pagina, int instante) {
        operacoes++;
        proximoUsoDaMoldura[moldura] = proximaOcorrencia[instante];
        ultimoUso[moldura] = instante;
    }

    @Override
    public int vitima(int[] residente, int pagina, int instante) {
        int escolhida = 0;

        for (int i = 1; i < residente.length; i++) {
            operacoes++;

            if (proximoUsoDaMoldura[i] > proximoUsoDaMoldura[escolhida]) {
                escolhida = i;
            } else if (desempataPorLru
                    && proximoUsoDaMoldura[i] == proximoUsoDaMoldura[escolhida]
                    && ultimoUso[i] < ultimoUso[escolhida]) {
                escolhida = i;
            }
        }

        return escolhida;
    }

    @Override
    public long operacoes() {
        return operacoes;
    }

    /**
     * Para cada instante, o indice da PROXIMA vez que aquela mesma pagina sera
     * pedida, ou {@link #NUNCA}.
     *
     * <p>Numa passada de tras para a frente, guardando a ultima posicao vista de cada
     * pagina.
     */
    public static int[] proximasOcorrencias(Traco traco) {
        int[] saida = new int[traco.tamanho()];
        Map<Integer, Integer> vistaEm = new HashMap<>();

        for (int i = traco.tamanho() - 1; i >= 0; i--) {
            Integer depois = vistaEm.get(traco.em(i));

            saida[i] = depois == null ? NUNCA : depois;
            vistaEm.put(traco.em(i), i);
        }

        return saida;
    }

    /**
     * O OTIMO escrito do jeito INGENUO: a cada falta, varre o resto do traco
     * procurando cada pagina residente.
     *
     * <p>Ele existe para conferir o otimo rapido. As duas implementacoes precisam dar
     * exatamente o mesmo numero de faltas em todo traco, e a diferenca entre elas e
     * so o custo: esta aqui paga o tamanho do traco a cada falta.
     *
     * <p>Um juiz conferido por outro juiz escrito de outro jeito vale mais do que um
     * juiz conferido duas vezes pelo mesmo caminho.
     */
    public static final class Ingenuo implements Politica {

        private Traco traco;
        private long operacoes;

        @Override
        public String nome() {
            return "OTIMO ingenuo";
        }

        @Override
        public void comecar(Traco traco, int molduras) {
            this.traco = traco;
        }

        @Override
        public int vitima(int[] residente, int pagina, int instante) {
            int escolhida = 0;
            int maisLonge = -1;

            for (int i = 0; i < residente.length; i++) {
                int quando = NUNCA;

                for (int j = instante + 1; j < traco.tamanho(); j++) {
                    operacoes++;

                    if (traco.em(j) == residente[i]) {
                        quando = j;
                        break;
                    }
                }

                if (quando > maisLonge) {
                    maisLonge = quando;
                    escolhida = i;
                }
            }

            return escolhida;
        }

        @Override
        public long operacoes() {
            return operacoes;
        }
    }
}
