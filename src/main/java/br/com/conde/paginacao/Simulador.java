package br.com.conde.paginacao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * O SIMULADOR: roda um traco com k molduras sob uma politica e conta as faltas.
 *
 * <p>Ele e deliberadamente burro. Toda a inteligencia esta na politica; aqui so tem
 * um vetor de molduras, uma busca linear para saber se a pagina ja esta la, e a
 * contagem. A busca linear e proposital: trocar por um mapa esconderia o custo de
 * uma coisa que o hardware faz em paralelo e que nao e o assunto.
 *
 * <p>Uma MOLDURA vazia vale -1. Enquanto houver moldura vazia a politica nem e
 * consultada: nao ha o que escolher.
 */
public final class Simulador {

    private Simulador() {
    }

    /**
     * @param politica  quem escolheu as vitimas
     * @param traco     o traco rodado
     * @param molduras  quantas paginas cabem na memoria
     * @param faltas    quantas vezes a pagina pedida nao estava la
     * @param acessos   o tamanho do traco
     * @param operacoes quanto a politica gastou decidindo
     */
    public record Resultado(String politica, String traco, int molduras,
                            long faltas, long acessos, long operacoes) {

        public double taxaDeFalta() {
            return acessos == 0 ? 0 : (double) faltas / acessos;
        }

        public long acertos() {
            return acessos - faltas;
        }
    }

    public static Resultado rodar(Politica politica, Traco traco, int molduras) {
        if (molduras <= 0) throw new IllegalArgumentException("precisa de pelo menos uma moldura");

        int[] residente = new int[molduras];
        Arrays.fill(residente, -1);

        politica.comecar(traco, molduras);

        int ocupadas = 0;
        long faltas = 0;

        for (int t = 0; t < traco.tamanho(); t++) {
            int pagina = traco.em(t);
            int onde = indiceDe(residente, pagina);

            if (onde >= 0) {
                politica.acertou(onde, pagina, t);
                continue;
            }

            faltas++;

            int destino;

            if (ocupadas < molduras) {
                destino = ocupadas++;
            } else {
                destino = politica.vitima(residente, pagina, t);

                if (destino < 0 || destino >= molduras)
                    throw new IllegalStateException(politica.nome() + " escolheu a moldura " + destino);
            }

            residente[destino] = pagina;
            politica.carregou(destino, pagina, t);
        }

        return new Resultado(politica.nome(), traco.nome(), molduras, faltas, traco.tamanho(), politica.operacoes());
    }

    /**
     * O CONJUNTO RESIDENTE depois de cada acesso.
     *
     * <p>E o que a propriedade de inclusao precisa: para uma politica de pilha, o
     * conjunto residente com k molduras tem de estar contido no conjunto com k mais
     * uma, no MESMO instante. E dessa propriedade que sai a garantia de nao ter a
     * anomalia de Belady, e e ela que a FIFO nao tem.
     */
    public static List<Set<Integer>> conjuntosResidentes(Politica politica, Traco traco, int molduras) {
        int[] residente = new int[molduras];
        Arrays.fill(residente, -1);

        politica.comecar(traco, molduras);

        List<Set<Integer>> saida = new ArrayList<>(traco.tamanho());
        int ocupadas = 0;

        for (int t = 0; t < traco.tamanho(); t++) {
            int pagina = traco.em(t);
            int onde = indiceDe(residente, pagina);

            if (onde >= 0) {
                politica.acertou(onde, pagina, t);
            } else {
                int destino;

                if (ocupadas < molduras) destino = ocupadas++;
                else destino = politica.vitima(residente, pagina, t);

                residente[destino] = pagina;
                politica.carregou(destino, pagina, t);
            }

            Set<Integer> agora = new TreeSet<>();

            for (int p : residente) if (p >= 0) agora.add(p);

            saida.add(agora);
        }

        return saida;
    }

    /** Onde a pagina esta, ou -1. Busca linear de proposito: ver o comentario da classe. */
    private static int indiceDe(int[] residente, int pagina) {
        for (int i = 0; i < residente.length; i++) if (residente[i] == pagina) return i;

        return -1;
    }

    /** As faltas de uma politica para cada numero de molduras de 1 ate o maximo. */
    public static long[] faltasPorMolduras(java.util.function.Supplier<Politica> politica, Traco traco, int maximo) {
        long[] saida = new long[maximo + 1];

        for (int k = 1; k <= maximo; k++) saida[k] = rodar(politica.get(), traco, k).faltas();

        return saida;
    }

    /** Quantas paginas distintas o traco tem: acima disso, mais molduras nao mudam nada. */
    public static Set<Integer> paginasDe(Traco traco) {
        Set<Integer> saida = new HashSet<>();

        for (int i = 0; i < traco.tamanho(); i++) saida.add(traco.em(i));

        return saida;
    }
}
