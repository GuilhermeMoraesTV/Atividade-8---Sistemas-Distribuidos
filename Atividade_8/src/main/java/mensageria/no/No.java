package mensageria.no;

import mensageria.comum.Mural;
import mensageria.comum.Pacote;
import mensageria.comum.Mensagem;

// Imports necessários para lidar com ficheiros
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class No {
    private final int id;
    private final int porta;
    private final Map<Integer, Integer> peers;

    // O mural agora não é final para que possa ser substituído pelo mural carregado do disco.
    private Mural muralLocal;
    private final Map<String, String> usuarios = new HashMap<>();

    private volatile boolean executando = true;
    private ServerSocket serverSocket;

    // O nome do ficheiro de persistência será único para cada nó.
    private final String NOME_ARQUIVO_MURAL;

    public No(int id, int porta, Map<Integer, Integer> peers) {
        this.id = id;
        this.porta = porta;
        this.peers = new HashMap<>(peers);
        this.peers.remove(this.id);

        // Define o nome do ficheiro e tenta carregar o mural do disco.
        this.NOME_ARQUIVO_MURAL = "mural_no_" + id + ".dat";
        carregarMuralDoDisco(); // Carrega o estado anterior, se existir.

        usuarios.put("anderson", "123");
        usuarios.put("carlos", "456");
        usuarios.put("guest", "789");
    }

    // Metodo para carregar o mural de um ficheiro.
    private void carregarMuralDoDisco() {
        File arquivoMural = new File(NOME_ARQUIVO_MURAL);
        if (arquivoMural.exists()) {
            // Usa try-with-resources para garantir que os streams sejam fechados.
            try (FileInputStream fis = new FileInputStream(arquivoMural);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                // Lê o objeto Mural completo do ficheiro.
                this.muralLocal = (Mural) ois.readObject();
                System.out.printf("[Nó %d] Mural carregado do disco com sucesso.%n", id);
            } catch (IOException | ClassNotFoundException e) {
                System.err.printf("[Nó %d] Erro ao carregar mural do disco. Iniciando com um novo. Erro: %s%n", id, e.getMessage());
                this.muralLocal = new Mural();
            }
        } else {
            // Se o ficheiro não existe, simplesmente inicia com um mural novo e vazio.
            System.out.printf("[Nó %d] Nenhum mural salvo encontrado. Iniciando com um novo.%n", id);
            this.muralLocal = new Mural();
        }
    }

    // Metodo para salvar o mural atual em um ficheiro.
    public synchronized void salvarMuralNoDisco() {
        // Usa try-with-resources.
        try (FileOutputStream fos = new FileOutputStream(NOME_ARQUIVO_MURAL);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            // Escreve o objeto Mural completo no ficheiro.
            oos.writeObject(this.muralLocal);
        } catch (IOException e) {
            System.err.printf("[Nó %d] Erro ao salvar mural no disco: %s%n", id, e.getMessage());
        }
    }

    // Ponto de entrada para a lógica do nó.
    public void iniciar() {
        try {
            // Adiciona um atraso escalonado para evitar a "corrida" na inicialização.
            Thread.sleep(id * 500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.printf("[Nó %d] Thread interrompida durante o atraso inicial.%n", id);
            return;
        }

        sincronizarComPeers();
        iniciarServidor();
    }

    // Metodo para parar o nó de forma explícita e segura, chamado pelo Simulador.
    public void parar() {
        this.executando = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.printf("[Nó %d] Erro ao fechar o socket do servidor: %s%n", id, e.getMessage());
        }
    }

    // Inicia o componente servidor do nó.
    private void iniciarServidor() {
        try {
            serverSocket = new ServerSocket(porta);
            System.out.printf("[Nó %d] Servidor iniciado na porta %d. Aguardando conexões...%n", id, porta);
            while (executando) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new TratadorDeConexao(clientSocket, this)).start();
            }
        } catch (SocketException e) {
            System.out.printf("[Nó %d] Servidor encerrado (socket fechado).%n", id);
        } catch (IOException e) {
            if (executando) {
                System.err.printf("[Nó %d] Erro crítico no servidor: %s%n", id, e.getMessage());
            }
        } finally {
            System.out.printf("[Nó %d] Thread do servidor finalizada.%n", id);
        }
    }

    // Envia uma nova mensagem para todos os outros nós ("peers") na rede.
    public void replicarParaPeers(Mensagem mensagem) {
        if (!executando) return;
        System.out.printf("[Nó %d] Replicando mensagem para %d peer(s)...%n", id, peers.size());
        Pacote pacote = new Pacote(Pacote.Tipo.REPLICAR_MSG, mensagem);
        for (Integer peerPorta : peers.values()) {
            try (Socket socket = new Socket("localhost", peerPorta);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                out.writeObject(pacote);
            } catch (IOException e) {
                System.err.printf("[Nó %d] Falha ao replicar para a porta %d (nó pode estar offline).%n", id, peerPorta);
            }
        }
    }

    // Mecanismo de reconciliação.
    @SuppressWarnings("unchecked")
    private void sincronizarComPeers() {
        System.out.printf("[Nó %d] Tentando sincronizar com a rede...%n", id);
        Pacote pacoteDePedido = new Pacote(Pacote.Tipo.PEDIDO_SYNC, null);
        for (Integer peerPorta : peers.values()) {
            try (Socket socket = new Socket("localhost", peerPorta);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                out.writeObject(pacoteDePedido);
                Pacote pacoteDeResposta = (Pacote) in.readObject();

                if (pacoteDeResposta.getTipo() == Pacote.Tipo.RESPOSTA_SYNC) {
                    List<Mensagem> mensagensRecebidas = (List<Mensagem>) pacoteDeResposta.getConteudo();
                    int adicionadas = muralLocal.adicionarTodas(mensagensRecebidas);
                    // Se alguma mensagem nova foi adicionada durante a sincronização, salva o estado.
                    if (adicionadas > 0) {
                        salvarMuralNoDisco();
                    }
                    System.out.printf("[Nó %d] Sincronização bem-sucedida com a porta %d. %d novas mensagens adicionadas.%n", id, peerPorta, adicionadas);
                    return;
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.printf("[Nó %d] Falha ao sincronizar com a porta %d (nó pode estar offline).%n", id, peerPorta);
            }
        }
        System.out.printf("[Nó %d] Não foi possível sincronizar com nenhum peer. Iniciando com mural local.%n", id);
    }

    // Getters
    public int getId() { return id; }
    public Mural getMuralLocal() { return muralLocal; }
    public Map<String, String> getUsuarios() { return usuarios; }
    public List<Mensagem> getTodasAsMensagens() { return muralLocal.getTodasAsMensagens(); }
}