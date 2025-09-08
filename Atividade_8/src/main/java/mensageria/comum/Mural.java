package mensageria.comum;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Representa o mural de mensagens compartilhado, que é a base de dados do sistema.
 * A palavra 'synchronized' em todos os métodos garante que as operações sejam "thread-safe",
 * ou seja, apenas uma thread pode modificar ou ler o mural por vez, evitando inconsistências.
 */
public class Mural implements Serializable {
    private static final long serialVersionUID = 1L;

    // A estrutura de dados escolhida foi um 'LinkedHashSet'.
    // - 'Set': Garante que não haverá mensagens duplicadas (baseado nos métodos equals/hashCode da classe Mensagem).
    // - 'Linked': Mantém a ordem em que as mensagens foram inseridas, o que é ideal para um mural.
    private final Set<mensageria.comum.Mensagem> mensagens = new LinkedHashSet<>();

    /**
     * Adiciona uma nova mensagem ao mural de forma segura.
     * @param mensagem A mensagem a ser adicionada.
     * @return true se a mensagem era nova e foi adicionada, false se já existia.
     */
    public synchronized boolean adicionarMensagem(mensageria.comum.Mensagem mensagem) {
        // O método 'add' do Set já lida com a verificação de duplicatas.
        return this.mensagens.add(mensagem);
    }

    /**
     * Adiciona uma coleção de mensagens ao mural.
     * Este método é crucial para a reconciliação, quando um nó que esteve offline
     * recebe uma lista de mensagens para se atualizar.
     * @param novasMensagens A lista de mensagens a serem adicionadas.
     * @return O número de mensagens que foram efetivamente adicionadas (ou seja, que não eram duplicatas).
     */
    public synchronized int adicionarTodas(List<mensageria.comum.Mensagem> novasMensagens) {
        int adicionadas = 0;
        // Itera sobre a lista de novas mensagens.
        for (mensageria.comum.Mensagem msg : novasMensagens) {
            // Tenta adicionar cada mensagem. Se 'add' retornar true, a mensagem era nova.
            if (this.mensagens.add(msg)) {
                adicionadas++;
            }
        }
        return adicionadas;
    }

    /**
     * Retorna uma CÓPIA da lista de todas as mensagens do mural.
     * É importante retornar uma cópia (new ArrayList) para que o código externo
     * não possa modificar a lista original de mensagens do mural diretamente.
     * @return Uma nova lista contendo todas as mensagens.
     */
    public synchronized List<mensageria.comum.Mensagem> getTodasAsMensagens() {
        return new ArrayList<>(this.mensagens);
    }

    /**
     * Retorna o conteúdo do mural como uma única string formatada para exibição.
     * @return A representação textual do mural.
     */
    @Override
    public synchronized String toString() {
        // Verifica se o mural está vazio para retornar uma mensagem amigável.
        if (mensagens.isEmpty()) {
            return "--- Mural vazio ---";
        }
        // StringBuilder é mais eficiente para construir strings em loops.
        StringBuilder sb = new StringBuilder();
        sb.append("--- MURAL DE MENSAGENS ---\n");
        // Itera sobre as mensagens e anexa a representação textual de cada uma.
        for (mensageria.comum.Mensagem msg : mensagens) {
            sb.append(msg.toString()).append("\n");
        }
        sb.append("--------------------------");
        return sb.toString();
    }
}