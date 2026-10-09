package br.com.conde.paginacao;

import java.util.List;
import java.util.Locale;

/**
 * O MEDIDOR: imprime as tabelas que o README mostra e que os testes conferem.
 *
 * <p>Toda tabela daqui tem um teste do lado que afirma os mesmos numeros. Texto nao
 * quebra quando deixa de ser verdade; teste quebra.
 */
public final class Medidor {

    private Medidor() {
    }

    public static void main(String[] args) {
        String comando = args.length > 0 ? args[0] : "tudo";

        if (comando.equals("definicao") || comando.equals("tudo")) definicao();
        if (comando.equals("anomalia") || comando.equals("tudo")) anomalia();
        if (comando.equals("inclusao") || comando.equals("tudo")) inclusao();
        if (comando.equals("laco") || comando.equals("tudo")) laco();
        if (comando.equals("juiz") || comando.equals("tudo")) juiz();
        if (comando.equals("custo") || comando.equals("tudo")) custo();
    }

    private static void definicao() {
        System.out.println();
        System.out.println("CADA POLITICA CONTRA O OTIMO: o otimo e piso, e a coluna da LRU bate com a distancia de pilha.");
        System.out.println();

        List<Medidas.LinhaDefinicao> linhas = Medidas.contraOOtimo();
        String[] nomes = linhas.get(0).politicas().keySet().toArray(new String[0]);

        StringBuilder cabecalho = new StringBuilder(String.format(
                "  %-26s %8s %8s %9s %8s", "traco", "acessos", "paginas", "molduras", "OTIMO"));

        for (String n : nomes) cabecalho.append(String.format(" %10s", n));

        cabecalho.append(String.format(" %6s %6s", "piso", "juiz"));
        System.out.println(cabecalho);

        for (Medidas.LinhaDefinicao l : linhas) {
            StringBuilder linha = new StringBuilder(String.format(
                    "  %-26s %8d %8d %9d %8d", l.traco(), l.acessos(), l.paginas(), l.molduras(), l.otimo()));

            for (String n : nomes) linha.append(String.format(" %10d", l.politicas().get(n)));

            linha.append(String.format(" %6s %6s", sim(l.otimoEhPiso()), sim(l.lruBateComOJuiz())));
            System.out.println(linha);
        }

        System.out.println();
    }

    private static void anomalia() {
        System.out.println();
        System.out.println("A ANOMALIA DE BELADY no traco de 1969: 1,2,3,4,1,2,5,1,2,3,4,5.");
        System.out.println();
        System.out.printf("  %-14s %9s %8s %18s %10s%n", "politica", "molduras", "faltas", "faltas com uma a mais", "piorou");

        for (Medidas.LinhaAnomalia l : Medidas.anomaliaNoTracoDeBelady())
            System.out.printf("  %-14s %9d %8d %18d %10s%n",
                    l.politica(), l.molduras(), l.faltas(), l.aMais(), l.anomalia() ? "SIM" : "nao");

        System.out.println();
        for (int[] regime : new int[][] {{30, 6}, {60, 8}, {200, 10}, {2_000, 12}}) {
            System.out.println("A CACA: 500 tracos uniformes de " + regime[0] + " acessos a "
                    + regime[1] + " paginas, molduras de 1 a 6.");
            System.out.println();
            System.out.printf("  %-14s %8s %14s %18s %12s%n",
                    "politica", "tracos", "com anomalia", "pares que pioraram", "pior salto");

            for (Medidas.LinhaCaca l : Medidas.cacarAnomalias(500, regime[1], regime[0], 6))
                System.out.printf("  %-14s %8d %14d %18d %12d%n",
                        l.politica(), l.tracos(), l.comAnomalia(), l.paresPiorados(), l.piorSalto());

            System.out.println();
        }

        System.out.println();
    }

    private static void inclusao() {
        System.out.println();
        System.out.println("A PROPRIEDADE DE INCLUSAO: o conjunto de k dentro do de k mais um, em todo instante.");
        System.out.println();
        System.out.printf("  %-14s %8s %16s %-44s%n", "politica", "pares", "pares que valem", "primeira falha");

        for (Medidas.LinhaInclusao l : Medidas.inclusaoPorPolitica(8))
            System.out.printf("  %-14s %8d %16d %-44s%n",
                    l.politica(), l.pares(), l.paresQueValem(),
                    l.primeiraFalha().isEmpty() ? "nenhuma" : l.primeiraFalha());

        System.out.println();
    }

    private static void laco() {
        System.out.println();
        System.out.println("O LACO com 4 molduras: a LRU falha em cem por cento dos acessos.");
        System.out.println();

        List<Medidas.LinhaLaco> linhas = Medidas.oLaco(4);
        String[] nomes = linhas.get(0).politicas().keySet().toArray(new String[0]);

        StringBuilder cabecalho = new StringBuilder(String.format(
                "  %14s %9s %8s %8s", "paginas do laco", "molduras", "acessos", "OTIMO"));

        for (String n : nomes) cabecalho.append(String.format(" %10s", n));

        System.out.println(cabecalho);

        for (Medidas.LinhaLaco l : linhas) {
            StringBuilder linha = new StringBuilder(String.format(
                    "  %14d %9d %8d %8d", l.paginasDoLaco(), l.molduras(), l.acessos(), l.otimo()));

            for (String n : nomes) linha.append(String.format(" %10d", l.politicas().get(n)));

            System.out.println(linha);
        }

        System.out.println();
    }

    private static void juiz() {
        System.out.println();
        System.out.println("O CUSTO DO PROPRIO JUIZ: o otimo rapido contra o ingenuo, nas mesmas faltas.");
        System.out.println();
        System.out.printf("  %8s %9s %8s %12s %12s %10s %14s%n",
                "paginas", "acessos", "faltas", "rapido", "ingenuo", "razao", "mesmas faltas");

        for (Medidas.LinhaJuiz l : Medidas.oCustoDoJuiz())
            System.out.printf(Locale.ROOT, "  %8d %9d %8d %12d %12d %10.1f %14s%n",
                    l.paginas(), l.acessos(), l.faltas(), l.rapido(), l.ingenuo(), l.razao(), sim(l.mesmasFaltas()));

        System.out.println();
    }

    private static void custo() {
        System.out.println();
        System.out.println("O CUSTO DE DECIDIR, somado em todos os tracos, com 4 molduras.");
        System.out.println();
        System.out.printf("  %-14s %10s %12s %20s %20s%n",
                "politica", "faltas", "operacoes", "operacoes por falta", "operacoes por acesso");

        for (Medidas.LinhaCusto l : Medidas.oCusto(4))
            System.out.printf(Locale.ROOT, "  %-14s %10d %12d %20.2f %20.2f%n",
                    l.politica(), l.faltas(), l.operacoes(), l.operacoesPorFalta(), l.operacoesPorAcesso());

        System.out.println();
    }

    private static String sim(boolean valor) {
        return valor ? "sim" : "NAO";
    }
}
