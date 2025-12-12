import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class GerenciadorInscricoes {

    private Map<String, Oficina> oficinas;
    private List<Participante> participantes;

    // Constantes para os nomes dos arquivos
    private final String ARQUIVO_OFICINAS = "oficinas.dat"; // Serialização para oficinas (dados complexos)
    private final String ARQUIVO_PARTICIPANTES_TXT = "participantes.dat"; // Arquivo de texto para participantes

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public GerenciadorInscricoes() {
        this.oficinas = new HashMap<>();
        this.participantes = new ArrayList<>();
        inicializarOficinasPadrao();
    }

    private void inicializarOficinasPadrao() {
        String[] titulosDefault = {
                "jQuery", "Arduino", "Desenvolvimento para Android",
                "Layout Responsivo com HTML5 e CSS3", "C++: Desenvolvimento para iOS", "Google Apps"
        };
        for (String titulo : titulosDefault) {
            oficinas.putIfAbsent(titulo, new Oficina(titulo));
        }
    }

    // --- Métodos Auxiliares de Persistência (Serialização) ---
    
    // Método para carregar a coleção de Oficinas (Mantido por Serialização)
    private void carregarOficinasSerializado() {
        try (ObjectInputStream oisOficinas = new ObjectInputStream(new FileInputStream(ARQUIVO_OFICINAS))) {
            Object obj = oisOficinas.readObject();
            if (obj instanceof Map) {
                this.oficinas = (Map<String, Oficina>) obj;
                System.out.println("Dados de Oficinas carregados (Serialização).");
            }
        } catch (FileNotFoundException e) {
            System.out.println("Arquivo de oficinas não encontrado. Iniciando com padrão.");
            inicializarOficinasPadrao();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Erro ao carregar oficinas: " + e.getMessage());
            inicializarOficinasPadrao();
        }
    }

    // Método para salvar a coleção de Oficinas (Mantido por Serialização)
    private void salvarOficinasSerializado() {
        try (ObjectOutputStream oosOficinas = new ObjectOutputStream(new FileOutputStream(ARQUIVO_OFICINAS))) {
            oosOficinas.writeObject(oficinas);
            System.out.println("Oficinas salvas (Serialização).");
        } catch (IOException e) {
            System.out.println("Erro ao salvar oficinas (Serialização): " + e.getMessage());
        }
    }

    // --- Persistência em TXT e Serialização (Métodos Principais) ---

    public void carregarDados() {
        carregarOficinasSerializado(); // 1. Carrega oficinas (Serialização)
        
        // 2. Carrega participantes (TXT)
        participantes.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(ARQUIVO_PARTICIPANTES_TXT))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                // Formato esperado: Nome;CPF;Sexo;DataNascimento(dd/MM/yyyy);Oficina1,Oficina2,...
                String[] partes = linha.split(";"); 
                
                if (partes.length >= 4) {
                    String nome = partes[0];
                    String cpf = partes[1];
                    String sexo = partes[2];
                    LocalDate dataNascimento = LocalDate.parse(partes[3], DATE_FMT);
                    
                    Participante p = new Participante(nome, cpf, sexo, dataNascimento);
                    
                    if (partes.length == 5 && !partes[4].isEmpty()) {
                        // Carrega as oficinas inscritas (separadas por vírgula)
                        List<String> oficinasInscritas = Arrays.asList(partes[4].split(","));
                        p.setTitulosOficinasInscritas(new ArrayList<>(oficinasInscritas)); // Usar ArrayList para ser mutável
                    }
                    
                    participantes.add(p);
                }
            }
            System.out.println("Dados de Participantes carregados de " + ARQUIVO_PARTICIPANTES_TXT + " (TXT).");
        } catch (FileNotFoundException e) {
            System.out.println("Arquivo " + ARQUIVO_PARTICIPANTES_TXT + " não encontrado. Iniciando lista de participantes vazia.");
        } catch (IOException | DateTimeParseException e) {
            System.out.println("Erro ao carregar participantes de TXT: " + e.getMessage());
        }
    }

    public void salvarDados() {
        salvarOficinasSerializado(); // 1. Salva oficinas (Serialização)

        // 2. Salva participantes (TXT)
        try (PrintWriter writer = new PrintWriter(new FileWriter(ARQUIVO_PARTICIPANTES_TXT))) {
            for (Participante p : participantes) {
                // Formato: Nome;CPF;Sexo;DataNascimento(dd/MM/yyyy);Oficina1,Oficina2,...
                String dataStr = p.getDataNascimento().format(DATE_FMT);
                String oficinasStr = String.join(",", p.getTitulosOficinasInscritas());
                
                writer.println(String.format("%s;%s;%s;%s;%s",
                    p.getNome(), p.getCpf(), p.getSexo(), dataStr, oficinasStr));
            }
            System.out.println("Participantes salvos em " + ARQUIVO_PARTICIPANTES_TXT + " (TXT).");
        } catch (IOException e) {
            System.out.println("Erro ao salvar participantes em TXT: " + e.getMessage());
        }
    }


    // --- Lógica de Registro ---

    public String registrarParticipante(Participante novoParticipante, List<String> titulosOficinas,
            LocalDate dataCorrente) {
        // 1. Validação de CPF existente
        for (Participante p : participantes) {
            if (p.getCpf().equals(novoParticipante.getCpf())) {
                return "ERRO: CPF já inscrito!";
            }
        }

        // 2. Validação quantidade de oficinas (1 a 3)
        if (titulosOficinas == null || titulosOficinas.size() < 1 || titulosOficinas.size() > 3) {
            return "ERRO: Selecione entre 1 e 3 oficinas.";
        }

        // 3. Validação de Vagas (Transacional - Check before Write)
        for (String titulo : titulosOficinas) {
            if (!oficinas.containsKey(titulo)) {
                return "ERRO: Oficina '" + titulo + "' não existe.";
            }
            Oficina oficina = oficinas.get(titulo);
            if (oficina.getVagasOcupadas() >= oficina.getVagasMaximas()) {
                return "ERRO: Oficina '" + titulo + "' está lotada (Max: " + oficina.getVagasMaximas()
                        + "). Inscrição cancelada.";
            }
        }

        // --- Efetivação do Registro ---
        for (String titulo : titulosOficinas) {
            Oficina oficina = oficinas.get(titulo);
            oficina.adicionarInscrito(novoParticipante.getCpf());
            novoParticipante.adicionarOficina(titulo);
        }

        participantes.add(novoParticipante);
        return "SUCESSO: Participante registrado e inscrito nas oficinas selecionadas.";
    }

    // --- Consultas Individuais ---

    public Map<String, Integer> consultarVagasDisponiveis() {
        Map<String, Integer> disponibilidade = new HashMap<>();
        for (Oficina of : oficinas.values()) {
            disponibilidade.put(of.getTitulo(), of.getVagasMaximas() - of.getVagasOcupadas());
        }
        return disponibilidade;
    }

    public String consultarInscricaoPorCPF(String cpf, LocalDate dataReferencia) {
        for (Participante p : participantes) {
            if (p.getCpf().equals(cpf)) {
                return String.format("Nome: %s | Sexo: %s | Faixa Etária: %s | Oficinas: %s",
                        p.getNome(), p.getSexo(), p.getFaixaEtaria(dataReferencia),
                        String.join(", ", p.getTitulosOficinasInscritas()));
            }
        }
        return "Participante com CPF " + cpf + " não encontrado.";
    }

    public List<String> consultarMenoresDeIdadePorOficina(String tituloOficina, LocalDate dataReferencia) {
        List<String> menores = new ArrayList<>();
        Oficina oficina = oficinas.get(tituloOficina);
        if (oficina == null)
            return menores;

        // Itera sobre os CPFs inscritos na oficina
        for (String cpf : oficina.getCpfsInscritos()) {
            // Busca o objeto participante pelo CPF (usando Stream para simplificar a busca)
            participantes.stream()
                         .filter(p -> p.getCpf().equals(cpf))
                         .findFirst()
                         .ifPresent(p -> {
                            if ("Menor de Idade".equals(p.getFaixaEtaria(dataReferencia))) {
                                menores.add(p.getNome());
                            }
                         });
        }
        return menores;
    }

    // --- Consultas Estatísticas ---

    public Map<String, Double> gerarEstatisticasPorSexo() {
        Map<String, Double> stats = new HashMap<>();
        int total = participantes.size();
        if (total == 0)
            return stats;

        // Contagem usando Stream API
        long masculinos = participantes.stream()
                .filter(p -> p.getSexo() != null && p.getSexo().equalsIgnoreCase("Masculino")).count();
        long femininos = participantes.stream()
                .filter(p -> p.getSexo() != null && p.getSexo().equalsIgnoreCase("Feminino")).count();

        stats.put("Masculino", (double) masculinos / total * 100);
        stats.put("Feminino", (double) femininos / total * 100);
        return stats;
    }

    public Map<String, Integer> gerarEstatisticasPorOficina() {
        Map<String, Integer> stats = new HashMap<>();
        for (Oficina of : oficinas.values()) {
            stats.put(of.getTitulo(), of.getVagasOcupadas());
        }
        return stats;
    }

    public Map<String, Map<String, Double>> gerarEstatisticasPorFaixaEtariaEOficina(LocalDate dataReferencia) {
        Map<String, Map<String, Double>> statsGeral = new HashMap<>();

        for (Oficina of : oficinas.values()) {
            List<String> cpfs = of.getCpfsInscritos();
            int totalInscritos = cpfs.size();

            Map<String, Double> officeStats = new HashMap<>();

            if (totalInscritos == 0) {
                officeStats.put("Menor de Idade", 0.0);
                officeStats.put("Maior de Idade", 0.0);
            } else {
                long menores = cpfs.stream()
                    .map(cpf -> participantes.stream().filter(p -> p.getCpf().equals(cpf)).findFirst().orElse(null))
                    .filter(p -> p != null && "Menor de Idade".equals(p.getFaixaEtaria(dataReferencia)))
                    .count();
                    
                long maiores = totalInscritos - menores; // O restante são maiores de idade

                officeStats.put("Menor de Idade", (double) menores / totalInscritos * 100);
                officeStats.put("Maior de Idade", (double) maiores / totalInscritos * 100);
            }
            statsGeral.put(of.getTitulo(), officeStats);
        }
        return statsGeral;
    }
}
