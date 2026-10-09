# paginacao

Políticas de substituição de página do zero em Java 21: dada a sequência de páginas
que um programa pede e quantas cabem na memória, qual página jogar fora quando
chega uma que não está lá.

O juiz é o **ótimo de Belady**, de 1966: joga fora a página cujo próximo uso está
mais longe no futuro. É impossível de implementar num sistema de verdade, porque
exige ler o futuro, e é exatamente por isso que ele serve de juiz. Nenhuma política
pode falhar menos que ele, e isso é teorema.

E há um **segundo juiz** que não sabe o que é moldura nem o que é vítima: a
distância de pilha, de Mattson e companhia, 1970. Para cada acesso, conte quantas
páginas diferentes foram pedidas desde a última vez que aquela apareceu. Com k
molduras, a LRU falha naquele acesso se e somente se essa distância for maior ou
igual a k. Uma passada, todos os tamanhos de memória, zero simulação.

## O ótimo é piso de todas

```
$ mvn exec:java -Dexec.args="definicao"

  traco                       acessos  paginas  molduras    OTIMO       FIFO        LRU      CLOCK        LFU        MRU  ALEATORIA   piso   juiz
  Belady                           12        5         4        6         10          8         10          7          6          7    sim    sim
  sequencial de 50                 50       50         8       50         50         50         50         50         50         50    sim    sim
  laco de 10                      200       10         8       52        200        200        200         67         52         87    sim    sim
  laco de 11                      220       11         8       73        220        220        220         87         73        113    sim    sim
  laco de 10 com ruido            233       20         8       88        230        230        230        126        126        164    sim    sim
  uniforme de 20                 2000       20         8      659       1199       1207       1200       1155       1154       1223    sim    sim
  uniforme de 50                 2000       50         8     1119       1670       1667       1670       1683       1680       1675    sim    sim
  zipf 1.0 de 50                 2000       50         8      703       1253       1145       1207        873       1534       1219    sim    sim
  zipf 1.5 de 50                 2000       49         8      341        693        550        600        402       1279        661    sim    sim
  conjunto de trabalho de 6      2000       38         8       90        106        104        105       1117       1256        173    sim    sim
```

A coluna `piso` confere o teorema linha a linha. A coluna `juiz` confere a coluna
da LRU contra a distância de pilha, que chega ao mesmo número sem simular nada.

Duas linhas já contam o resto do repositório. No **laço**, FIFO, LRU e CLOCK falham
em **todos os 200 acessos**, e a MRU empata com o ótimo. No **conjunto de
trabalho**, a LFU gasta 1.117 faltas contra 104 da LRU.

## O laço derruba a política mais sensata do mundo

```
$ mvn exec:java -Dexec.args="laco"

  paginas do laco  molduras  acessos    OTIMO       FIFO        LRU      CLOCK        LFU        MRU  ALEATORIA
               3         4       60        3          3          3          3          3          3          3
               4         4       80        4          4          4          4          4          4          4
               5         4      100       28        100        100        100         43         28         39
               6         4      120       51        120        120        120         63         51         78
               8         4      160       94        160        160        160        103         94        133
```

Com uma página a mais do que cabe na memória, a LRU joga fora exatamente a página
que vai ser pedida no próximo acesso, toda vez. **Cem por cento de faltas.**

Não é um caso construído para humilhar a LRU: percorrer um vetor maior que o cache
em círculo é a coisa mais comum que um programa faz.

A MRU, que joga fora a página usada há **menos** tempo e soa absurda, empata com o
ótimo em toda linha. E o sorteio puro, que não acredita em nada, ganha das três.

## A anomalia de Belady

```
$ mvn exec:java -Dexec.args="anomalia"

  politica        molduras   faltas faltas com uma a mais     piorou
  FIFO                   1       12                 12        nao
  FIFO                   2       12                  9        nao
  FIFO                   3        9                 10        SIM
  FIFO                   4       10                  5        nao
```

Doze acessos, cinco páginas: `1,2,3,4,1,2,5,1,2,3,4,5`. Com três molduras a FIFO
falha nove vezes; com quatro, dez. **Dar mais memória ao programa piorou o
resultado.**

A CLOCK sofre no mesmo ponto. A LRU, a MRU e o ótimo com desempate não sofrem
nunca, e a seção seguinte diz por quê.

## A propriedade que explica a anomalia

```
$ mvn exec:java -Dexec.args="inclusao"

  politica          pares  pares que valem primeira falha
  OTIMO                67               31 laco de 10 em 5 no acesso 195
  OTIMO+LRU            67               67 nenhuma
  FIFO                 67               35 Belady em 3 no acesso 6
  LRU                  67               67 nenhuma
  CLOCK                67               35 Belady em 3 no acesso 6
  LFU                  67               51 uniforme de 20 em 2 no acesso 585
  MRU                  67               67 nenhuma
  ALEATORIA            67               13 sequencial de 50 em 2 no acesso 6
```

Uma política é **de pilha** quando o conjunto residente com k molduras está contido
no conjunto com k mais uma, no mesmo instante, sempre. Quem tem essa propriedade
não pode sofrer a anomalia: um conjunto contido no outro não pode ter menos
acertos.

A FIFO e a CLOCK quebram no mesmo acesso do mesmo traço, que é onde a anomalia
aparece.

## Como rodar

```
mvn test
mvn exec:java -Dexec.args="tudo"
```

Os comandos do medidor são `definicao`, `anomalia`, `inclusao`, `laco`, `juiz`,
`custo` e `tudo`.

## O que as medidas me corrigiram

**O ótimo que eu escrevi não era política de pilha, e os livros dizem que ele é.**
Ele falhou a inclusão em 36 dos 67 pares. O teorema está certo e a minha leitura
dele é que estava errada.

Quando duas páginas residentes **nunca mais** vão ser pedidas, tanto faz qual sai:
nenhuma das duas causa falta depois. A contagem de faltas não distingue as duas
escolhas, e a minha implementação desempatava pelo índice da moldura, que muda
quando se muda o número de molduras. Com o desempate pela usada há mais tempo, a
inclusão passa a valer em todos os 67 pares, e o número de faltas é exatamente o
mesmo nas duas versões.

A propriedade é sobre a política, e não sobre a contagem de faltas. Duas políticas
igualmente ótimas, uma de pilha e a outra não.

**Quem mais sofre a anomalia de Belady não é a FIFO, é o sorteio puro.** Eu pus a
FIFO no repositório como "a política da anomalia", porque é assim que todo livro
conta. Numa varredura de 500 traços sorteados de 60 acessos a 8 páginas, a FIFO
sofre em 4 e a política aleatória sofre em 59, com um salto de 10 faltas contra 1.

Faz sentido depois de medido: o sorteio também não é política de pilha, e as
trajetórias dele com k e com k mais uma moldura não têm nenhuma razão para se
parecer.

**E a anomalia some quando o traço fica longo.** Nos mesmos 500 traços com 2.000
acessos a 12 páginas, nenhuma política sofre nenhuma vez. Ela precisa de traço
curto em relação ao número de páginas; num traço longo as faltas obrigatórias e o
regime permanente dominam a conta e as duas curvas voltam a se comportar.

Eu tinha escrito que a anomalia "não é uma curiosidade de um traço escolhido a
dedo". Ela é mais comum do que uma curiosidade e muito mais rara do que eu dava a
entender, e a fronteira entre os dois regimes é o que a tabela mede.

**A CLOCK quase não economiza operação.**

```
$ mvn exec:java -Dexec.args="custo"

  politica           faltas    operacoes  operacoes por falta operacoes por acesso
  OTIMO                5121        25958                 5.07                 2.42
  FIFO                 7326        14612                 1.99                 1.36
  LRU                  7172        32111                 4.48                 3.00
  CLOCK                7276        26229                 3.60                 2.45
  ALEATORIA            7359         7319                 0.99                 0.68
```

Eu escrevi na documentação da classe que ela existe para não pagar a varredura da
LRU. Ela gasta 82% das operações da LRU, e não uma fração pequena. A vantagem real
dela não está no número de operações: está em um bit por moldura ser uma coisa que
o hardware liga de graça, e disso a contagem de operações não sabe nada.

A política mais barata da tabela é o sorteio, com menos de uma operação por falta,
e ela não é a pior em faltas.

**O juiz ingênuo fica caro por um motivo que não é o que eu esperava.**

```
$ mvn exec:java -Dexec.args="juiz"

   paginas   acessos   faltas       rapido      ingenuo      razao  mesmas faltas
        20      2000     1119         5345        55951       10.5            sim
        50      2000     1452         6344       142803       22.5            sim
       100      2000     1626         6866       277974       40.5            sim
       200      2000     1751         7241       511899       70.7            sim
       400      2000     1825         7463       880669      118.0            sim
```

O ingênuo varre o traço à frente até achar a próxima ocorrência de cada página
residente. Eu esperava que o custo dele crescesse com o **tamanho** do traço, e ele
é o mesmo nas cinco linhas. O que faz o preço subir é o traço ficar **espalhado**:
com mais páginas, a próxima ocorrência está mais longe e a varredura anda mais.

O rápido vai de 5.345 a 7.463 operações no mesmo intervalo, porque ele calcula
todas as próximas ocorrências numa passada de trás para a frente e depois só olha
as molduras.

## Licença

MIT.
