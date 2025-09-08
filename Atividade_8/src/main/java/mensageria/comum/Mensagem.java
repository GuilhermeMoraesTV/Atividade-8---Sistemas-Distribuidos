package mensageria.comum;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Representa uma única mensagem no mural.
 * A classe implementa 'Serializable' para que seus objetos possam ser convertidos
 * em bytes e enviados através da rede (sockets).
 */
public class Mensagem implements Serializable {
    // serialVersionUID é um controle de versão para a serialização.
    // Garante que o objeto enviado e o recebido sejam da mesma versão da classe.
    private static final long serialVersionUID = 1L;

    // Atributos finais (final) garantem que, uma vez que a mensagem é criada, ela não pode ser alterada.
    private final UUID id;         // Um Identificador Único Universal para cada mensagem.
    private final String autor;
    private final String conteudo;
    private final long timestamp;  // A hora exata em que a mensagem foi criada, em milissegundos.

    // Construtor para criar uma nova mensagem.
    public Mensagem(String autor, String conteudo) {
        // Gera um ID aleatório e único para a mensagem. Isso evita colisões e garante
        // que cada mensagem, mesmo com conteúdo idêntico, seja distinta.
        this.id = UUID.randomUUID();
        this.autor = autor;
        this.conteudo = conteudo;
        // Captura o momento exato da criação da mensagem.
        this.timestamp = new Date().getTime();
    }

    // Métodos "getter" para permitir o acesso aos atributos privados da mensagem.
    public UUID getId() {
        return id;
    }

    public String getAutor() {
        return autor;
    }

    public String getConteudo() {
        return conteudo;
    }

    // Sobrescreve o método toString() para fornecer uma representação textual bonita da mensagem.
    @Override
    public String toString() {
        // Formata o timestamp (que é um número longo) para uma data legível no formato "dd/MM/yyyy HH:mm:ss".
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        // Retorna a string final formatada.
        return String.format("[%s] %s: %s", sdf.format(new Date(timestamp)), autor, conteudo);
    }

    // Os métodos equals() e hashCode() são essenciais para que coleções como HashSet
    // funcionem corretamente, evitando a adição de mensagens duplicadas.

    // Sobrescreve o método equals() para definir o que faz duas mensagens serem "iguais".
    @Override
    public boolean equals(Object o) {
        // Duas mensagens são consideradas iguais se e somente se seus IDs únicos forem iguais.
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mensagem mensagem = (Mensagem) o;
        return id.equals(mensagem.id);
    }

    // Sobrescreve o método hashCode() para ser consistente com o método equals().
    @Override
    public int hashCode() {
        // O hashCode é baseado apenas no ID, pois é o campo usado para a comparação no equals().
        return id.hashCode();
    }
}