import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class App {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static void main(String[] args) {
        GerenciadorInscricoes manager = new GerenciadorInscricoes();
        
        // Chama o método que agora executa a persistência TXT e Serialização
        manager.carregarDados(); 

        Scanner scanner = new Scanner(System.in);
        int opcao = -1;

        while (opcao != 0) {
            exibirMenu();
            try {
                String input = scanner.nextLine();
                opcao = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Opção inválida. Digite um número.");
                continue;
            }

            switch (opcao) {
                case 1:
                    realizarInscricao(manager, scanner);
                    break;
                case 2:
                    Map<String, Integer> vagas = manager.consultarVagasDisponiveis();
                    System.out.println("\n--- Vagas Disponíveis ---");
                    vagas.forEach((k, v) -> System.out.println(k + ": " + v));
                    break;
                case 3:
                    System.out.print("Digite o CPF para consulta: ");
                    String cpfCons = scanner.nextLine();
                    System.out.println(manager.consultarInscricaoPorCPF(cpfCons, LocalDate.now()));
                    break;
                case 4:
                    System.out.println("\n--- Oficinas ---"); // Listar nomes pode ajudar
                    Map<String, Integer> ofs = manager.consultarVagasDisponiveis(); // Hack to get keys
                    ofs.keySet().forEach(System.out::println);

                    System.out.print("Digite o nome exato da oficina: ");
                    String tituloMenor = scanner.nextLine();
                    List<String> menores = manager.consultarMenoresDeIdadePorOficina(tituloMenor, LocalDate.now());
                    
                    System.out.println("\n--- Consulta de Menores de Idade ---");
                    if (menores.isEmpty()) {
                         // Adicionando verificação para dar feedback ao usuário
                        if (ofs.containsKey(tituloMenor)) {
                            System.out.println("Nenhum participante menor de idade encontrado em '" + tituloMenor + "'.");
                        } else {
                            System.out.println("ERRO: Oficina '" + tituloMenor + "' não encontrada.");
                        }
                    } else {
                         System.out.println("Participantes menores de idade em '" + tituloMenor + "': " + menores);
                    }
                    break;
                case 5:
                    Map<String, Double> stSexo = manager.gerarEstatisticasPorSexo();
                    System.out.println("\n--- Estatísticas por Sexo ---");
                    stSexo.forEach((k, v) -> System.out.printf("%s: %.2f%%\n", k, v));
                    break;
                case 6:
                    Map<String, Integer> stOf = manager.gerarEstatisticasPorOficina();
                    System.out.println("\n--- Inscrições por Oficina ---");
                    stOf.forEach((k, v) -> System.out.println(k + ": " + v + " inscritos"));
                    break;
                case 7:
                    Map<String, Map<String, Double>> stFaixa = manager
                            .gerarEstatisticasPorFaixaEtariaEOficina(LocalDate.now());
                    System.out.println("\n--- Estatísticas Faixa Etária por Oficina ---");
                    stFaixa.forEach((k, v) -> {
                        System.out.println("Oficina: " + k);
                        v.forEach((subK, subV) -> System.out.printf("  %s: %.2f%%\n", subK, subV));
                    });
                    break;
                case 0:
                    System.out.println("Salvando dados e saindo...");
                    // Chama o método que agora executa a persistência TXT e Serialização
                    manager.salvarDados(); 
                    break;
                default:
                    System.out.println("Opção inválida.");
            }
            if (opcao != 0) {
                System.out.println("\nPressione Enter para continuar...");
                scanner.nextLine();
            }
        }
    }

    private static void exibirMenu() {
        System.out.println("\n=== SIO - Sistema de Inscrições em Oficinas ===");
        System.out.println("1. Registrar Participante e Inscrições");
        System.out.println("2. Consultar Vagas Disponíveis");
        System.out.println("3. Consultar Inscrição por CPF");
        System.out.println("4. Consultar Menores de Idade por Oficina");
        System.out.println("5. Estatísticas: Por Sexo");
        System.out.println("6. Estatísticas: Total Inscritos por Oficina");
        System.out.println("7. Estatísticas: Faixa Etária por Oficina");
        System.out.println("0. Sair");
        System.out.print("Escolha uma opção: ");
    }

    private static void realizarInscricao(GerenciadorInscricoes manager, Scanner scanner) {
        System.out.println("\n--- Nova Inscrição ---");
        System.out.print("Nome: ");
        String nome = scanner.nextLine();
        System.out.print("CPF: ");
        String cpf = scanner.nextLine();
        System.out.print("Sexo (Masculino/Feminino): ");
        String sexo = scanner.nextLine();

        LocalDate nasc = null;
        while (nasc == null) {
            System.out.print("Data de Nascimento (dd/MM/yyyy): ");
            String dataStr = scanner.nextLine();
            try {
                // Adicionando um pequeno tratamento de erro no ano de nascimento
                nasc = LocalDate.parse(dataStr, DATE_FMT);
                if (nasc.isAfter(LocalDate.now())) {
                    System.out.println("Data de nascimento não pode ser futura.");
                    nasc = null;
                }
            } catch (DateTimeParseException e) {
                System.out.println("Data inválida. Use o formato dd/MM/yyyy.");
            }
        }

        Participante p = new Participante(nome, cpf, sexo, nasc);

        // Seleção de Oficinas
        Map<String, Integer> mapVagas = manager.consultarVagasDisponiveis();
        List<String> titulosDisponiveis = new ArrayList<>(mapVagas.keySet());

        List<String> selecionadas = new ArrayList<>();
        System.out.println("Selecione entre 1 e 3 oficinas (Digite o número da oficina):");
        for (int i = 0; i < titulosDisponiveis.size(); i++) {
            System.out.println((i + 1) + ". " + titulosDisponiveis.get(i) + " (Vagas: " + mapVagas.get(titulosDisponiveis.get(i)) + ")");
        }

        System.out.println("Digite os números separados por vírgula (ex: 1,3): ");
        String selecaoStr = scanner.nextLine();
        String[] parts = selecaoStr.split(",");

        for (String part : parts) {
            try {
                int idx = Integer.parseInt(part.trim()) - 1;
                if (idx >= 0 && idx < titulosDisponiveis.size()) {
                    String t = titulosDisponiveis.get(idx);
                    if (!selecionadas.contains(t)) {
                        selecionadas.add(t);
                    }
                }
            } catch (NumberFormatException e) {
                // ignorar
            }
        }

        String resultado = manager.registrarParticipante(p, selecionadas, LocalDate.now());
        System.out.println(resultado);
    }
}
