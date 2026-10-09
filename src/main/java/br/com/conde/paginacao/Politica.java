package br.com.conde.paginacao;

/**
 * Uma POLITICA DE SUBSTITUICAO: quando a memoria esta cheia e chega uma pagina que
 * nao esta la, qual moldura esvaziar.
 *
 * <p>E so isso. Toda a diferenca entre as politicas cabe no metodo {@link #vitima},
 * e a diferenca de resultado entre elas e enorme.
 *
 * <p>O contrato tem quatro avisos e nao tres de proposito. A politica precisa saber
 * do ACERTO, e nao so da falta: e exatamente no acerto que a LRU atualiza a ordem e
 * que a CLOCK liga o bit de referencia. Uma politica que so e avisada quando falha
 * nao consegue ser LRU, e esse foi o primeiro desenho que eu escrevi aqui.
 */
public interface Politica {

    String nome();

    /** Avisado uma vez antes do traco. O otimo precisa disto: ele le o futuro. */
    default void comecar(Traco traco, int molduras) {
    }

    /** A pagina ja estava na memoria, nesta moldura. */
    default void acertou(int moldura, int pagina, int instante) {
    }

    /** A pagina entrou nesta moldura, que estava vazia ou acabou de ser esvaziada. */
    default void carregou(int moldura, int pagina, int instante) {
    }

    /**
     * A memoria esta cheia e a pagina precisa entrar: qual moldura sai.
     *
     * @param residente moldura para pagina, no estado atual
     * @param pagina    a pagina que esta chegando
     * @param instante  o indice do acesso no traco
     * @return o indice da moldura a esvaziar
     */
    int vitima(int[] residente, int pagina, int instante);

    /**
     * Quantas operacoes a politica gastou para decidir.
     *
     * <p>Operacoes, e nao segundos. Segundo nao e medida: muda com a maquina, com o
     * que mais esta rodando e com o humor do coletor de lixo. A integracao continua
     * roda em tres sistemas, e tres numeros de segundos nao se comparam.
     */
    long operacoes();
}
