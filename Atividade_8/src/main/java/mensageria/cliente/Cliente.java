package mensageria.cliente;

import mensageria.comum.Mensagem;
import mensageria.comum.Pacote;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;

public class Cliente {
    private final String host;
    private final int porta;

    private boolean autenticado = false;
    private String usuarioAutenticado = null;

    public Cliente(String host, int porta) {
        this.host = host;
        this.porta = porta;
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Uso: java -jar <caminho_do_jar> <porta_do_no>");
            System.err.println("Exemplo: java -jar NOME_DO_ARQUIVO.jar 8001");
            return;
        }
        try {
            int porta = Integer.parseInt(args[0]);
            Cliente cliente = new Cliente("localhost", porta);
            cliente.iniciar();
        } catch (NumberFormatException e) {
            System.err.println("A porta deve ser um número.");
        }
    }

    // NOVO: Método que faz uma verificação rápida para ver se o nó está online.
    private boolean verificarNoAtivo() {
        // Tenta abrir e fechar um socket. Se conseguir, o nó está ativo.
        try (Socket socket = new Socket(host, porta)) {
            return true;
        } catch (IOException e) {
            // Se falhar, o nó está offline. A mensagem de erro é mostrada aqui.
            System.err.printf("Erro: Não foi possível conectar ao nó na porta %d. O nó está offline?%n", porta);
            return false;
        }
    }

    public void iniciar() {
        System.out.printf("--- Iniciando Cliente para a porta %d ---\n", porta);

        // ALTERAÇÃO: Verificação inicial antes de mostrar o menu.
        if (!verificarNoAtivo()) {
            System.out.println("Encerrando cliente.");
            return; // Encerra se o nó alvo estiver inativo.
        }

        System.out.printf("Conectado com sucesso ao nó na porta %d.%n", porta);

        Scanner scanner = new Scanner(System.in);
        while (true) {
            exibirMenu();
            System.out.print("Escolha uma opção: ");
            String opcao = scanner.nextLine();

            switch (opcao) {
                case "1":
                    fazerLogin(scanner);
                    break;
                case "2":
                    lerMural();
                    break;
                case "3":
                    postarMensagem(scanner);
                    break;
                case "4":
                    System.out.println("Encerrando cliente...");
                    return;
                default:
                    System.out.println("Opção inválida. Tente novamente.");
            }
        }
    }

    private void exibirMenu() {
        System.out.println("\n--- MENU DO CLIENTE ---");
        System.out.println("1. Fazer Login");
        System.out.println("2. Ler Mural de Mensagens");
        System.out.println("3. Postar Nova Mensagem");
        System.out.println("4. Sair");
        System.out.println("-----------------------");
        if (autenticado) {
            System.out.printf("Status: Logado como '%s'%n", usuarioAutenticado);
        } else {
            System.out.println("Status: Não logado");
        }
    }

    // Os métodos fazerLogin, lerMural e postarMensagem permanecem os mesmos
    private void fazerLogin(Scanner scanner) {
        System.out.print("Digite o usuário: ");
        String usuario = scanner.nextLine();
        System.out.print("Digite a senha: ");
        String senha = scanner.nextLine();
        String credenciais = usuario + ";" + senha;

        try (Socket socket = new Socket(host, porta);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(new Pacote(Pacote.Tipo.LOGIN, credenciais));
            Pacote resposta = (Pacote) in.readObject();

            if (resposta.getTipo() == Pacote.Tipo.LOGIN_OK) {
                this.autenticado = true;
                this.usuarioAutenticado = usuario;
                System.out.println(">>> " + resposta.getConteudo());
            } else {
                this.autenticado = false;
                this.usuarioAutenticado = null;
                System.err.println(">>> " + resposta.getConteudo());
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro de comunicação ao tentar fazer login. O nó pode estar offline.");
        }
    }

    @SuppressWarnings("unchecked")
    private void lerMural() {
        try (Socket socket = new Socket(host, porta);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(new Pacote(Pacote.Tipo.LER_MURAL, null));
            Pacote resposta = (Pacote) in.readObject();

            if (resposta.getTipo() == Pacote.Tipo.MURAL_ATUALIZADO) {
                List<Mensagem> mensagens = (List<Mensagem>) resposta.getConteudo();
                System.out.println("\n--- MURAL DE MENSAGENS ---");
                if (mensagens.isEmpty()) {
                    System.out.println("   (Mural vazio)");
                } else {
                    for (Mensagem msg : mensagens) {
                        System.out.println(msg);
                    }
                }
                System.out.println("--------------------------");
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro de comunicação ao tentar ler o mural. O nó pode estar offline.");
        }
    }

    private void postarMensagem(Scanner scanner) {
        if (!autenticado) {
            System.err.println("Erro: Você precisa estar logado para postar uma mensagem.");
            return;
        }
        System.out.print("Digite sua mensagem: ");
        String conteudo = scanner.nextLine();
        Mensagem novaMensagem = new Mensagem(this.usuarioAutenticado, conteudo);

        try (Socket socket = new Socket(host, porta);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(new Pacote(Pacote.Tipo.POSTAR_MENSAGEM, novaMensagem));
            System.out.println("Mensagem enviada para o mural!");
        } catch (IOException e) {
            System.err.println("Erro de comunicação ao tentar postar a mensagem. O nó pode estar offline.");
        }
    }
}