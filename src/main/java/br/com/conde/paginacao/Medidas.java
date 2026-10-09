package br.com.conde.paginacao;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * As MEDIDAS, em faltas de pagina e em operacoes, nunca em segundos.
 *
 * <p>Segundo nao e medida: muda com a maquina, com o que mais esta rodando e com o
 * humor do coletor de lixo. A integracao continua roda em tres sistemas, e tres
 * numeros de segundos nao se comparam. Falta de pagina e a mesma conta em qualquer
 * lugar, e e o que o sistema operacional realmente paga.
 */
public final class Medidas {

    private Medidas() {
    }

    // ───────────────────────── 1. contra o otimo ─────────────────────────

    public record LinhaDefinicao(String traco, int acessos, int paginas, int molduras,
                                 long otimo, Map<String, Long> politicas,
                                 boolean otimoEhPiso, boolean lruBateComOJuiz) {
    }

    /**
     * Cada politica contra o OTIMO, traco por traco.
     *
     * <p>Duas colunas de conferencia em cada linha: o otimo e piso de todas, e a
     * coluna da LRU bate com a conta da distancia de pilha, que nao simula nada.
     */
    public static List<LinhaDefinicao> contraOOtimo() {
        List<LinhaDefinicao> linhas = new ArrayList<>();

        for (Traco traco : Traco.todos().values()) {
            int molduras = Math.max(2, Math.min(8, traco.distintas() - 1));

            long otimo = Simulador.rodar(new Otimo(), traco, molduras).faltas();

            Map<String, Long> politicas = new LinkedHashMap<>();
            boolean piso = true;

            for (Supplier<Politica> p : Politicas.todas()) {
                long faltas = Simulador.rodar(p.get(), traco, molduras).faltas();

                politicas.put(p.get().nome(), faltas);

                if (faltas < otimo) piso = false;
            }

            long[] peloJuiz = Juiz.faltasDaLruPorDistancia(traco, molduras);

            linhas.add(new LinhaDefinicao(
                    traco.nome(), traco.tamanho(), traco.distintas(), molduras,
                    otimo, politicas, piso, politicas.get("LRU") == peloJuiz[molduras]));
        }

        return linhas;
    }

    // ───────────────────────── 2. a anomalia ─────────────────────────

    public record LinhaAnomalia(String politica, String traco, int molduras,
                                long faltas, long aMais, boolean anomalia) {
    }

    /**
     * A ANOMALIA DE BELADY no traco de 1969, moldura por moldura.
     *
     * <p>Doze acessos, cinco paginas. Com tres molduras a FIFO falha nove vezes; com
     * quatro, dez.
     */
    public static List<LinhaAnomalia> anomaliaNoTracoDeBelady() {
        List<LinhaAnomalia> linhas = new ArrayList<>();
        Traco traco = Traco.deBelady();

        for (Supplier<Politica> p : Politicas.comOtimo())
            for (Juiz.Anomalia a : Juiz.anomalias(p, traco, 5))
                linhas.add(new LinhaAnomalia(a.politica(), a.traco(), a.molduras(), a.faltas(), a.aMais(), a.anomalia()));

        return linhas;
    }

    public record LinhaCaca(String politica, int tracos, int comAnomalia, int paresPiorados, long piorSalto) {
    }

    /**
     * A CACA A ANOMALIA: em tracos sorteados, quantos a sofrem e com qual politica.
     *
     * <p>A anomalia nao e uma curiosidade de um traco escolhido a dedo. Mas ela
     * tambem nao aparece em qualquer traco: ela precisa de traco CURTO em relacao ao
     * numero de paginas. Num traco longo as faltas obrigatorias e o regime permanente
     * dominam a conta e as duas curvas voltam a se comportar.
     *
     * <p>Por isso a tabela varre dois regimes, e a diferenca entre eles e o achado.
     */
    public static List<LinhaCaca> cacarAnomalias(int quantos, int paginas, int acessos, int maximoDeMolduras) {
        List<LinhaCaca> linhas = new ArrayList<>();

        for (Supplier<Politica> p : Politicas.comOtimo()) {
            int comAnomalia = 0;
            int paresPiorados = 0;
            long piorSalto = 0;

            for (long semente = 1; semente <= quantos; semente++) {
                Traco traco = Traco.uniforme(acessos, paginas, semente);
                boolean achou = false;

                for (Juiz.Anomalia a : Juiz.anomalias(p, traco, maximoDeMolduras))
                    if (a.anomalia()) {
                        achou = true;
                        paresPiorados++;
                        piorSalto = Math.max(piorSalto, a.aMais() - a.faltas());
                    }

                if (achou) comAnomalia++;
            }

            linhas.add(new LinhaCaca(p.get().nome(), quantos, comAnomalia, paresPiorados, piorSalto));
        }

        return linhas;
    }

    // ───────────────────────── 3. a inclusao ─────────────────────────

    public record LinhaInclusao(String politica, int pares, int paresQueValem, String primeiraFalha) {
    }

    /**
     * A PROPRIEDADE DE INCLUSAO, politica por politica, em todo traco e todo par de
     * molduras vizinhas.
     *
     * <p>E a explicacao da tabela anterior: quem tem a propriedade nao pode sofrer a
     * anomalia, e quem nao tem, pode.
     */
    public static List<LinhaInclusao> inclusaoPorPolitica(int maximoDeMolduras) {
        List<LinhaInclusao> linhas = new ArrayList<>();

        for (Supplier<Politica> p : Politicas.comOtimo()) {
            int pares = 0;
            int valem = 0;
            String primeiraFalha = "";

            for (Traco traco : Traco.todos().values())
                for (int k = 1; k < Math.min(maximoDeMolduras, traco.distintas()); k++) {
                    Juiz.Inclusao i = Juiz.inclusao(p, traco, k);

                    pares++;

                    if (i.vale()) valem++;
                    else if (primeiraFalha.isEmpty())
                        primeiraFalha = traco.nome() + " em " + k + " no acesso " + i.instante();
                }

            linhas.add(new LinhaInclusao(p.get().nome(), pares, valem, primeiraFalha));
        }

        return linhas;
    }

    // ───────────────────────── 4. o laco ─────────────────────────

    public record LinhaLaco(int paginasDoLaco, int molduras, long otimo, Map<String, Long> politicas, long acessos) {
    }

    /**
     * O LACO, que e onde a politica mais sensata do mundo falha em cem por cento dos
     * acessos.
     *
     * <p>Percorrer um vetor maior que o cache em circulo e o que todo programa faz. A
     * LRU joga fora exatamente a pagina que vai ser pedida no proximo acesso, toda
     * vez. A MRU, que soa absurda, acerta quase tudo.
     */
    public static List<LinhaLaco> oLaco(int molduras) {
        List<LinhaLaco> linhas = new ArrayList<>();

        for (int paginas : new int[] {molduras - 1, molduras, molduras + 1, molduras + 2, molduras * 2}) {
            if (paginas < 1) continue;

            Traco traco = Traco.laco(paginas, 20);

            Map<String, Long> politicas = new LinkedHashMap<>();

            for (Supplier<Politica> p : Politicas.todas())
                politicas.put(p.get().nome(), Simulador.rodar(p.get(), traco, molduras).faltas());

            linhas.add(new LinhaLaco(paginas, molduras,
                    Simulador.rodar(new Otimo(), traco, molduras).faltas(),
                    politicas, traco.tamanho()));
        }

        return linhas;
    }

    // ───────────────────────── 5. o custo do juiz ─────────────────────────

    public record LinhaJuiz(int paginas, int acessos, long faltas, long rapido, long ingenuo,
                            double razao, boolean mesmasFaltas) {
    }

    /**
     * O CUSTO DO PROPRIO JUIZ: o otimo rapido contra o otimo ingenuo.
     *
     * <p>Os dois dao exatamente as mesmas faltas, e e essa coluna que faz deles uma
     * conferencia. O que muda e o preco, e ele muda de um jeito que eu nao tinha
     * previsto: o ingenuo varre o traco ate achar a proxima ocorrencia de cada pagina
     * residente, entao quanto mais ESPALHADO o traco, mais longe fica essa ocorrencia
     * e mais caro ele custa.
     *
     * <p>O rapido quase nao se mexe, porque ele calcula todas as proximas ocorrencias
     * numa passada de tras para a frente e depois so varre as molduras.
     */
    public static List<LinhaJuiz> oCustoDoJuiz() {
        List<LinhaJuiz> linhas = new ArrayList<>();

        for (int paginas : new int[] {20, 50, 100, 200, 400}) {
            Traco traco = Traco.uniforme(2_000, paginas, 1);

            Simulador.Resultado rapido = Simulador.rodar(new Otimo(), traco, 4);
            Simulador.Resultado ingenuo = Simulador.rodar(new Otimo.Ingenuo(), traco, 4);

            linhas.add(new LinhaJuiz(paginas, traco.tamanho(), rapido.faltas(),
                    rapido.operacoes(), ingenuo.operacoes(),
                    (double) ingenuo.operacoes() / rapido.operacoes(),
                    rapido.faltas() == ingenuo.faltas()));
        }

        return linhas;
    }

    // ───────────────────────── 6. o custo ─────────────────────────

    public record LinhaCusto(String politica, long faltas, long operacoes,
                             double operacoesPorFalta, double operacoesPorAcesso) {
    }

    /**
     * O CUSTO de decidir, somado em todos os tracos.
     *
     * <p>A CLOCK existe porque a LRU exige uma ordem completa de uso, e manter ordem
     * completa em hardware nao da. A pergunta que a tabela responde e quanto a CLOCK
     * paga em faltas a mais para pagar tao pouco em operacoes.
     */
    public static List<LinhaCusto> oCusto(int molduras) {
        List<LinhaCusto> linhas = new ArrayList<>();

        for (Supplier<Politica> fabrica : Politicas.comOtimo()) {
            long faltas = 0;
            long operacoes = 0;
            long acessos = 0;

            for (Traco traco : Traco.todos().values()) {
                Simulador.Resultado r = Simulador.rodar(fabrica.get(), traco, molduras);

                faltas += r.faltas();
                operacoes += r.operacoes();
                acessos += r.acessos();
            }

            linhas.add(new LinhaCusto(fabrica.get().nome(), faltas, operacoes,
                    faltas == 0 ? 0 : (double) operacoes / faltas,
                    acessos == 0 ? 0 : (double) operacoes / acessos));
        }

        return linhas;
    }
}
