package mensageria.no;

import mensageria.comum.Mural;
import mensageria.comum.Pacote;
import mensageria.comum.Mensagem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class TratadorDeConexao implements Runnable {

    private final Socket socket;
    private final No noPai;
    // A flag 'autenticado' não é mais necessária aqui, pois cada conexão é para uma única ação.
    // A validação será feita dentro de cada chamada.

    public TratadorDeConexao(Socket socket, No noPai) {
        this.socket = socket;
        this.noPai = noPai;
    }

    @Override
    public void run() {
        // Modelo de uma única requisição por conexão.
        // O try-with-resources garante que tudo é fechado corretamente.
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // Lê um único pacote da conexão.
            Pacote pacoteRecebido = (Pacote) in.readObject();

            // Processa esse pacote, passando o stream de saída para a resposta.
            processarPacote(pacoteRecebido, out);

        } catch (IOException | ClassNotFoundException e) {
            // Silencioso, pois uma desconexão é esperada e normal.
        }
    }

    // O método agora recebe 'out' para poder enviar a resposta na mesma conexão.
    private void processarPacote(Pacote pacote, ObjectOutputStream out) throws IOException {
        System.out.printf("[Nó %d] Pacote recebido: %s%n", noPai.getId(), pacote.getTipo());
        Mural mural = noPai.getMuralLocal();

        switch (pacote.getTipo()) {
            case LOGIN:
                String[] credenciais = ((String) pacote.getConteudo()).split(";");
                if (credenciais.length == 2) {
                    String usuario = credenciais[0];
                    String senha = credenciais[1];
                    if (noPai.getUsuarios().getOrDefault(usuario, "").equals(senha)) {
                        // Responde diretamente, não há estado de sessão para guardar.
                        out.writeObject(new Pacote(Pacote.Tipo.LOGIN_OK, "Login bem-sucedido!"));
                    } else {
                        out.writeObject(new Pacote(Pacote.Tipo.LOGIN_FALHA, "Usuário ou senha inválidos."));
                    }
                }
                break;

            case LER_MURAL:
                out.writeObject(new Pacote(Pacote.Tipo.MURAL_ATUALIZADO, mural.getTodasAsMensagens()));
                break;

            case POSTAR_MENSAGEM:
                // A validação de autenticação agora está implícita no cliente, que só envia
                // este pacote se estiver logado. O pacote em si contém o autor.
                Mensagem novaMensagem = (Mensagem) pacote.getConteudo();
                if (mural.adicionarMensagem(novaMensagem)) {
                    noPai.replicarParaPeers(novaMensagem);
                    noPai.salvarMuralNoDisco();
                }
                // Não há resposta para o cliente neste caso.
                break;

            case REPLICAR_MSG:
                Mensagem msgReplicada = (Mensagem) pacote.getConteudo();
                if (mural.adicionarMensagem(msgReplicada)) {
                    System.out.printf("[Nó %d] Mensagem replicada de outro nó foi adicionada ao mural.%n", noPai.getId());
                    noPai.salvarMuralNoDisco();
                }
                break;

            case PEDIDO_SYNC:
                out.writeObject(new Pacote(Pacote.Tipo.RESPOSTA_SYNC, mural.getTodasAsMensagens()));
                break;
        }
        out.flush();
    }
}