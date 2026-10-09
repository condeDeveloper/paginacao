package br.com.conde.paginacao;

/**
 * O SORTEIO, escrito aqui em vez de vir da biblioteca.
 *
 * <p>A documentacao do {@code java.util.Random} promete a mesma sequencia entre
 * versoes, mas o {@code ThreadLocalRandom} e o {@code RandomGenerator} novo nao
 * prometem nada, e a tentacao de trocar um pelo outro e grande. Pior: a integracao
 * continua deste repositorio roda em tres sistemas, e "a FIFO sofreu a anomalia em
 * 37 de 1.000 tracos sorteados" com tracos diferentes em cada sistema nao e uma
 * medida: sao tres medidas que ninguem pode comparar.
 *
 * <p>O gerador e um congruente linear de 64 bits, e o deslocamento de 32 bits no
 * fim nao e enfeite. Os bits de baixo de um congruente linear tem periodo curto: o
 * ultimo bit alterna par e impar a cada passo, e um traco de paginas sorteado com
 * ele viraria um ziguezague entre duas paginas. Quem devolve a palavra inteira
 * entrega esse defeito junto.
 */
public final class Sorteio {

    private long estado;

    public Sorteio(long semente) {
        this.estado = semente == 0 ? 1 : semente;
    }

    /** O proximo numero de 32 bits, tirado da METADE DE CIMA do estado. */
    public int proximo() {
        estado = estado * 6364136223846793005L + 1442695040888963407L;
        return (int) (estado >>> 32);
    }

    /** Um inteiro de 0 ate o limite, exclusive. */
    public int ate(int limite) {
        if (limite <= 0) throw new IllegalArgumentException("limite precisa ser positivo: " + limite);

        return (int) (Integer.toUnsignedLong(proximo()) % limite);
    }

    /** Uma fracao entre 0 e 1. */
    public double fracao() {
        return Integer.toUnsignedLong(proximo()) / 4294967296.0;
    }
}
