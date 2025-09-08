package mensageria.comum;

import java.io.Serializable;

/**
 * Representa um "envelope" para toda a comunicação na rede.
 * Padroniza as requisições, permitindo que o servidor saiba qual
 * ação tomar com base no Tipo do pacote. É como o assunto de um e-mail.
 */
public class Pacote implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Enum 'Tipo' define todos os possíveis comandos ou tipos de mensagem
     * que podem ser trocados entre cliente e servidor, ou entre servidores.
     */
    public enum Tipo {
        // Ações iniciadas pelo Cliente
        LOGIN,            // Cliente tentando se autenticar (envia "usuario;senha")
        POSTAR_MENSAGEM,  // Cliente enviando uma nova mensagem para o mural
        LER_MURAL,        // Cliente pedindo a versão atual do mural

        // Respostas do Servidor (Nó) para o Cliente
        LOGIN_OK,         // Resposta do nó se o login for bem-sucedido
        LOGIN_FALHA,      // Resposta do nó se o login falhar
        MURAL_ATUALIZADO, // Resposta do nó com a lista de mensagens

        // Comunicação entre Nós (Servidores)
        REPLICAR_MSG,     // Um nó enviando uma nova mensagem para outro nó replicar
        PEDIDO_SYNC,      // Um nó que voltou de falha pedindo as mensagens que perdeu
        RESPOSTA_SYNC     // Um nó enviando sua lista de mensagens para sincronização
    }

    // O tipo do pacote, que define a intenção da comunicação.
    private final Tipo tipo;
    // O conteúdo/carga do pacote. É do tipo 'Object' para ser genérico e poder
    // carregar diferentes tipos de dados (String, Mensagem, List<Mensagem>, etc.).
    private final Object conteudo;

    // Construtor para criar um novo pacote com um tipo e um conteúdo.
    public Pacote(Tipo tipo, Object conteudo) {
        this.tipo = tipo;
        this.conteudo = conteudo;
    }

    // Getter para obter o tipo do pacote.
    public Tipo getTipo() {
        return tipo;
    }

    // Getter para obter o conteúdo do pacote.
    public Object getConteudo() {
        return conteudo;
    }
}