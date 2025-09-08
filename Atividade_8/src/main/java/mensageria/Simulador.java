package mensageria;

import mensageria.no.No;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ponto de entrada principal para a aplicação.
 * Esta classe é responsável por iniciar a rede de nós do sistema de mensagens,
 * orquestrar a simulação de falha e fornecer feedback ao usuário.
 */
public class Simulador {

    // --- Configurações da Simulação ---
    // Define quantos nós serão criados na rede.
    private static final int NUMERO_DE_NOS = 3;
    // A porta inicial para os nós. O Nó 1 usará 8001, o Nó 2 usará 8002, e assim por diante.
    private static final int PORTA_BASE = 8000;

    // O método main é onde a execução do programa começa.
    public static void main(String[] args) throws InterruptedException {
        System.out.println("--- INICIANDO A REDE DE NÓS ---");

        // 1. Mapeia as portas para cada nó da rede.
        // Cria um mapa que associa o ID de cada nó (ex: 1) à sua porta correspondente (ex: 8001).
        Map<Integer, Integer> peers = new HashMap<>();
        for (int i = 1; i <= NUMERO_DE_NOS; i++) {
            peers.put(i, PORTA_BASE + i);
        }

        // Guarda as instâncias dos objetos No para podermos interagir com eles mais tarde (ex: para pará-los).
        List<No> nos = new ArrayList<>();

        // 2. Cria e inicia cada nó em uma thread separada.
        for (int i = 1; i <= NUMERO_DE_NOS; i++) {
            // Cria uma cópia do mapa de peers para cada nó.
            Map<Integer, Integer> peersParaEsteNo = new HashMap<>(peers);
            // Remove o próprio nó da sua lista de peers.
            peersParaEsteNo.remove(i);

            // Cria a instância do nó.
            No no = new No(i, PORTA_BASE + i, peersParaEsteNo);
            nos.add(no); // Adiciona a instância à nossa lista de controle.

            // Cria uma nova thread para executar a lógica do nó (o método no::iniciar).
            // Isso permite que todos os nós executem em paralelo.
            Thread thread = new Thread(no::iniciar);
            thread.start();
        }

        System.out.println("\n>>> REDE DE NÓS INICIADA. OS SERVIDORES ESTÃO ATIVOS. <<<");
        System.out.println(">>> Use o script EXECUTAR_CLIENTE.bat para interagir com a rede. <<<");

        // 3. Agenda e executa a simulação de falha.
        // Define qual nó será "derrubado".
        int noParaDerrubar = 3;
        // Define quanto tempo esperar antes de simular a falha (em milissegundos).
        int tempoAteFalha = 60 * 1000; // 60 segundos
        System.out.printf("%n>>> Simulação de falha agendada: Nó %d irá falhar em %d segundos.%n", noParaDerrubar, tempoAteFalha / 1000);

        // A thread principal do Simulador "dorme" por 60 segundos.
        Thread.sleep(tempoAteFalha);

        // Após a espera, a falha é acionada.
        // Obtém a instância do nó que deve falhar da nossa lista.
        No noQueVaiFalhar = nos.get(noParaDerrubar - 1); // (-1 porque a lista começa em 0)
        System.out.printf("%n>>> SIMULANDO FALHA DO NÓ %d <<<%n", noParaDerrubar);
        // Chama o metodo 'parar()' do nó, que o encerra de forma controlada e segura.
        noQueVaiFalhar.parar();

        System.out.println(">>> Para testar a reconciliação, reinicie a simulação. O Nó 3 irá se sincronizar com os outros. <<<");
        // O método main termina aqui, mas o programa Java continua a executar
        // porque as threads dos nós restantes (Nó 1 e Nó 2) ainda estão ativas.
    }
}