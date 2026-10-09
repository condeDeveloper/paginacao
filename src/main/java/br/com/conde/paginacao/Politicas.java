package br.com.conde.paginacao;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

/**
 * As POLITICAS, e o que cada uma acredita sobre o futuro.
 *
 * <p>Nenhuma delas ve o futuro, e por isso todas erram. O que muda e no que elas
 * apostam:
 *
 * <ul>
 *   <li>A FIFO aposta que a pagina mais ANTIGA e a menos util. Ela nao olha uso
 *       nenhum, so idade.
 *   <li>A LRU aposta que o passado recente se repete: a pagina usada ha mais tempo
 *       e a proxima a ser dispensada.
 *   <li>A CLOCK aposta o mesmo que a LRU, mas com um bit por moldura em vez de uma
 *       ordem completa. E a LRU que cabe em hardware.
 *   <li>A LFU aposta na frequencia: a menos pedida sai.
 *   <li>A ALEATORIA nao aposta em nada, e esta aqui porque e o piso. Uma politica
 *       que perde para ela esta errada, nao so imperfeita.
 *   <li>A MRU joga fora a mais recente, que soa absurdo, e ganha no laco.
 * </ul>
 */
public final class Politicas {

    private Politicas() {
    }

    /** As politicas que nao veem o futuro, na ordem das tabelas. */
    public static List<Supplier<Politica>> todas() {
        return List.of(Fifo::new, Lru::new, Clock::new, Lfu::new, Mru::new, () -> new Aleatoria(7));
    }

    public static List<Supplier<Politica>> comOtimo() {
        return List.of(Otimo::new, () -> new Otimo(true), Fifo::new, Lru::new, Clock::new, Lfu::new, Mru::new, () -> new Aleatoria(7));
    }

    // ───────────────────────────── FIFO ─────────────────────────────

    /**
     * FIFO: sai a que entrou primeiro.
     *
     * <p>Uma fila de molduras, e nada mais. Nao olha se a pagina foi usada mil vezes
     * desde que entrou. E justamente por isso ela nao tem a propriedade de inclusao,
     * e e nela que a anomalia de Belady aparece.
     */
    public static final class Fifo implements Politica {

        private final Deque<Integer> fila = new ArrayDeque<>();
        private long operacoes;

        @Override
        public String nome() {
            return "FIFO";
        }

        @Override
        public void carregou(int moldura, int pagina, int instante) {
            operacoes++;
            fila.addLast(moldura);
        }

        @Override
        public int vitima(int[] residente, int pagina, int instante) {
            operacoes++;
            return fila.removeFirst();
        }

        @Override
        public long operacoes() {
            return operacoes;
        }
    }

    // ───────────────────────────── LRU ─────────────────────────────

    /**
     * LRU: sai a usada ha mais tempo.
     *
     * <p>Guarda o instante do ultimo uso de cada moldura e varre as molduras para
     * achar o menor. A varredura custa o numero de molduras por falta, e e essa a
     * conta que a CLOCK tenta nao pagar.
     */
    public static final class Lru implements Politica {

        private int[] ultimoUso = new int[0];
        private long operacoes;

        @Override
        public String nome() {
            return "LRU";
        }

        @Override
        public void comecar(Traco traco, int molduras) {
            ultimoUso = new int[molduras];
            Arrays.fill(ultimoUso, -1);
        }

        @Override
        public void acertou(int moldura, int pagina, int instante) {
            operacoes++;
            ultimoUso[moldura] = instante;
        }

        @Override
        public void carregou(int moldura, int pagina, int instante) {
            operacoes++;
            ultimoUso[moldura] = instante;
        }

        @Override
        public int vitima(int[] residente, int pagina, int instante) {
            int escolhida = 0;

            for (int i = 1; i < residente.length; i++) {
                operacoes++;

                if (ultimoUso[i] < ultimoUso[escolhida]) escolhida = i;
            }

            return escolhida;
        }

        @Override
        public long operacoes() {
            return operacoes;
        }
    }

    // ───────────────────────────── CLOCK ─────────────────────────────

    /**
     * CLOCK, ou segunda chance: a LRU que cabe em hardware.
     *
     * <p>Um bit por moldura e um ponteiro que gira. Quando precisa de vitima, o
     * ponteiro anda: moldura com bit ligado perde o bit e ganha mais uma volta,
     * moldura com bit desligado sai.
     *
     * <p>Ela nao e a LRU. Entre duas molduras com o bit ligado, a CLOCK escolhe pela
     * posicao no circulo e nao pela ordem de uso. A tabela mede o tamanho dessa
     * diferenca, e ele e menor do que eu esperava.
     */
    public static final class Clock implements Politica {

        private boolean[] referencia = new boolean[0];
        private int ponteiro;
        private long operacoes;

        @Override
        public String nome() {
            return "CLOCK";
        }

        @Override
        public void comecar(Traco traco, int molduras) {
            referencia = new boolean[molduras];
            ponteiro = 0;
        }

        @Override
        public void acertou(int moldura, int pagina, int instante) {
            operacoes++;
            referencia[moldura] = true;
        }

        @Override
        public void carregou(int moldura, int pagina, int instante) {
            operacoes++;
            referencia[moldura] = true;
        }

        @Override
        public int vitima(int[] residente, int pagina, int instante) {
            while (true) {
                operacoes++;

                if (!referencia[ponteiro]) {
                    int escolhida = ponteiro;

                    ponteiro = (ponteiro + 1) % referencia.length;

                    return escolhida;
                }

                referencia[ponteiro] = false;
                ponteiro = (ponteiro + 1) % referencia.length;
            }
        }

        @Override
        public long operacoes() {
            return operacoes;
        }
    }

    // ───────────────────────────── LFU ─────────────────────────────

    /**
     * LFU: sai a menos pedida.
     *
     * <p>A contagem e por PAGINA e nao por moldura, e ela nao e zerada quando a
     * pagina sai. E assim que a LFU classica funciona, e e tambem a razao do defeito
     * dela: uma pagina muito pedida numa fase antiga fica na memoria para sempre,
     * mesmo que ninguem a peca de novo. O nome disso e poluicao por cache.
     */
    public static final class Lfu implements Politica {

        private int[] contagem = new int[0];
        private long operacoes;

        @Override
        public String nome() {
            return "LFU";
        }

        @Override
        public void comecar(Traco traco, int molduras) {
            int maior = 0;

            for (int i = 0; i < traco.tamanho(); i++) maior = Math.max(maior, traco.em(i));

            contagem = new int[maior + 1];
        }

        @Override
        public void acertou(int moldura, int pagina, int instante) {
            operacoes++;
            contagem[pagina]++;
        }

        @Override
        public void carregou(int moldura, int pagina, int instante) {
            operacoes++;
            contagem[pagina]++;
        }

        @Override
        public int vitima(int[] residente, int pagina, int instante) {
            int escolhida = 0;

            for (int i = 1; i < residente.length; i++) {
                operacoes++;

                if (contagem[residente[i]] < contagem[residente[escolhida]]) escolhida = i;
            }

            return escolhida;
        }

        @Override
        public long operacoes() {
            return operacoes;
        }
    }

    // ───────────────────────────── MRU ─────────────────────────────

    /**
     * MRU: sai a usada ha MENOS tempo.
     *
     * <p>Soa absurdo, e e exatamente o que o laco pede. Num laco maior que a memoria,
     * a pagina que acabou de ser usada e a que vai demorar mais a voltar, e a usada
     * ha mais tempo e a proxima. A MRU acerta o laco inteiro menos uma pagina; a LRU
     * erra todas.
     */
    public static final class Mru implements Politica {

        private int[] ultimoUso = new int[0];
        private long operacoes;

        @Override
        public String nome() {
            return "MRU";
        }

        @Override
        public void comecar(Traco traco, int molduras) {
            ultimoUso = new int[molduras];
            Arrays.fill(ultimoUso, -1);
        }

        @Override
        public void acertou(int moldura, int pagina, int instante) {
            operacoes++;
            ultimoUso[moldura] = instante;
        }

        @Override
        public void carregou(int moldura, int pagina, int instante) {
            operacoes++;
            ultimoUso[moldura] = instante;
        }

        @Override
        public int vitima(int[] residente, int pagina, int instante) {
            int escolhida = 0;

            for (int i = 1; i < residente.length; i++) {
                operacoes++;

                if (ultimoUso[i] > ultimoUso[escolhida]) escolhida = i;
            }

            return escolhida;
        }

        @Override
        public long operacoes() {
            return operacoes;
        }
    }

    // ───────────────────────────── ALEATORIA ─────────────────────────────

    /**
     * ALEATORIA: sorteia a vitima.
     *
     * <p>E o piso do problema e esta aqui para isso. Uma politica que perde para o
     * sorteio num traco nao e so imperfeita: ela esta apostando na direcao errada
     * naquele traco. Acontece com a LRU no laco.
     */
    public static final class Aleatoria implements Politica {

        private final long semente;
        private Sorteio sorteio;
        private long operacoes;

        public Aleatoria(long semente) {
            this.semente = semente;
            this.sorteio = new Sorteio(semente);
        }

        @Override
        public String nome() {
            return "ALEATORIA";
        }

        @Override
        public void comecar(Traco traco, int molduras) {
            sorteio = new Sorteio(semente);
        }

        @Override
        public int vitima(int[] residente, int pagina, int instante) {
            operacoes++;
            return sorteio.ate(residente.length);
        }

        @Override
        public long operacoes() {
            return operacoes;
        }
    }
}
