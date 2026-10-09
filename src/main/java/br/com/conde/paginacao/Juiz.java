package br.com.conde.paginacao;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * O JUIZ, que sao na verdade TRES juizes independentes.
 *
 * <p><b>1. O otimo de Belady.</b> Nenhuma politica pode falhar menos que ele no
 * mesmo traco com as mesmas molduras. E um teorema, nao uma observacao, e por isso
 * serve de piso em toda linha de toda tabela.
 *
 * <p><b>2. A distancia de pilha, de Mattson e companhia, 1970.</b> As faltas da LRU
 * para TODOS os tamanhos de memoria, calculadas numa unica passada e SEM SIMULAR
 * nada. Para cada acesso, conte quantas paginas DIFERENTES foram pedidas desde a
 * ultima vez que aquela pagina apareceu; chame isso de distancia. Com k molduras, a
 * LRU falha naquele acesso se e somente se a distancia for maior ou igual a k.
 *
 * <p>Esse segundo juiz nao sabe o que e moldura, nao sabe o que e vitima e nao
 * chama o simulador. Quando ele e o simulador concordam em todo traco e todo k, as
 * duas coisas que poderiam estar erradas teriam de estar erradas do mesmo jeito.
 *
 * <p><b>3. A propriedade de inclusao.</b> Uma politica e de PILHA quando o conjunto
 * residente com k molduras esta contido no conjunto com k mais uma, no mesmo
 * instante, sempre. Quem tem essa propriedade nao pode sofrer a anomalia de Belady:
 * um conjunto contido no outro nao pode ter MENOS acertos. A LRU e o otimo tem; a
 * FIFO nao.
 */
public final class Juiz {

    private Juiz() {
    }

    // ───────────────────── 2. a distancia de pilha ─────────────────────

    /**
     * As faltas da LRU para cada k de 1 ate o maximo, pela DISTANCIA DE PILHA.
     *
     * <p>A posicao da pagina numa lista mantida em ordem de uso recente E o numero de
     * paginas distintas desde o ultimo uso dela. Pagina nunca vista tem distancia
     * infinita e falha com qualquer memoria: e a falta obrigatoria.
     *
     * <p>A lista e percorrida linearmente de proposito. A versao com arvore de
     * contagem seria mais rapida e mediria a mesma coisa; aqui o que importa e que o
     * caminho seja obviamente correto, porque ele e juiz.
     */
    public static long[] faltasDaLruPorDistancia(Traco traco, int maximo) {
        List<Integer> pilha = new ArrayList<>();
        long[] histograma = new long[maximo + 2];
        long obrigatorias = 0;

        for (int t = 0; t < traco.tamanho(); t++) {
            int pagina = traco.em(t);
            int posicao = pilha.indexOf(pagina);

            if (posicao < 0) {
                obrigatorias++;
            } else {
                pilha.remove(posicao);

                // distancia maior que o maior k que nos interessa cai no ultimo balde
                histograma[Math.min(posicao, maximo + 1)]++;
            }

            pilha.add(0, pagina);
        }

        long[] faltas = new long[maximo + 1];

        for (int k = 1; k <= maximo; k++) {
            long comDistanciaGrande = 0;

            for (int d = k; d <= maximo + 1; d++) comDistanciaGrande += histograma[d];

            faltas[k] = obrigatorias + comDistanciaGrande;
        }

        return faltas;
    }

    // ───────────────────── 3. a propriedade de inclusao ─────────────────────

    /**
     * @param politica a politica testada
     * @param traco    o traco
     * @param molduras o k menor do par
     * @param vale     o conjunto de k esteve contido no de k mais um em todo instante
     * @param instante o primeiro instante em que falhou, ou menos um
     */
    public record Inclusao(String politica, String traco, int molduras, boolean vale, int instante) {
    }

    /** O conjunto residente com k molduras esta contido no de k mais uma, em todo instante? */
    public static Inclusao inclusao(Supplier<Politica> politica, Traco traco, int molduras) {
        List<Set<Integer>> menor = Simulador.conjuntosResidentes(politica.get(), traco, molduras);
        List<Set<Integer>> maior = Simulador.conjuntosResidentes(politica.get(), traco, molduras + 1);

        String nome = politica.get().nome();

        for (int t = 0; t < menor.size(); t++)
            if (!maior.get(t).containsAll(menor.get(t)))
                return new Inclusao(nome, traco.nome(), molduras, false, t);

        return new Inclusao(nome, traco.nome(), molduras, true, -1);
    }

    // ───────────────────── a anomalia ─────────────────────

    /**
     * @param politica  a politica testada
     * @param traco     o traco
     * @param molduras  quantas molduras tinha
     * @param faltas    quantas faltas deu
     * @param aMais     quantas faltas deu com uma moldura A MAIS
     * @param anomalia  dar mais memoria PIOROU o resultado
     */
    public record Anomalia(String politica, String traco, int molduras, long faltas, long aMais, boolean anomalia) {
    }

    /** Onde dar mais uma moldura PIORA o numero de faltas. */
    public static List<Anomalia> anomalias(Supplier<Politica> politica, Traco traco, int maximo) {
        List<Anomalia> saida = new ArrayList<>();

        long anterior = Simulador.rodar(politica.get(), traco, 1).faltas();

        for (int k = 1; k < maximo; k++) {
            long depois = Simulador.rodar(politica.get(), traco, k + 1).faltas();

            saida.add(new Anomalia(politica.get().nome(), traco.nome(), k, anterior, depois, depois > anterior));

            anterior = depois;
        }

        return saida;
    }
}
