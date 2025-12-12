
import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class GerenciadorInscricoes {

    private Map<String, Oficina> oficinas;
    private List<Participante> participantes;

    private final String ARQUIVO_OFICINAS = "oficinas.ser";
    private final String ARQUIVO_PARTICIPANTES = "participantes.ser";

    public GerenciadorInscricoes() {
        this.oficinas = new HashMap<>();
        this.participantes = new ArrayList<>();
        // Inicializa com as oficinas padrão caso não carregue do arquivo depois
        inicializarOficinasPadrao();
    }

    private void inicializarOficinasPadrao() {
        String[] titulosDefault = {
                "jQuery", "Arduino", "Desenvolvimento para Android",
                "Layout Responsivo com HTML5 e CSS3", "C++: Desenvolvimento para iOS", "Google Apps"
        };
        for (String titulo : titulosDefault) {
            // Put only if not exists (preserves existing data upon load if called oddly,
            // but usually we load first. If load is empty, we populate).
            oficinas.putIfAbsent(titulo, new Oficina(titulo));
        }
    }

    // --- Persistência ---

    public void carregarDados() {
        try (ObjectInputStream oisOficinas = new ObjectInputStream(new FileInputStream(ARQUIVO_OFICINAS))) {
            Object obj = oisOficinas.readObject();
            if (obj instanceof Map) {
                this.oficinas = (Map<String, Oficina>) obj;
                System.out.println("Dados de Oficinas carregados com sucesso.");
            }
        } catch (FileNotFoundException e) {
            System.out.println("Arquivo de oficinas não encontrado. Iniciando com padrão.");
            inicializarOficinasPadrao();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Erro ao carregar oficinas: " + e.getMessage());
            inicializarOficinasPadrao();
        }

        try (ObjectInputStream oisParticipantes = new ObjectInputStream(new FileInputStream(ARQUIVO_PARTICIPANTES))) {
            Object obj = oisParticipantes.readObject();
            if (obj instanceof List) {
                this.participantes = (List<Participante>) obj;
                System.out.println("Dados de Participantes carregados com sucesso.");
            }
        } catch (FileNotFoundException e) {
            System.out.println("Arquivo de participantes não encontrado. Iniciando lista vazia.");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Erro ao carregar participantes: " + e.getMessage());
        }
    }

    public void salvarDados() {
        try (ObjectOutputStream oosOficinas = new ObjectOutputStream(new FileOutputStream(ARQUIVO_OFICINAS))) {
            oosOficinas.writeObject(oficinas);
            System.out.println("Oficinas salvas.");
        } catch (IOException e) {
            System.out.println("Erro ao salvar oficinas: " + e.getMessage());
        }

        try (ObjectOutputStream oosParticipantes = new ObjectOutputStream(
                new FileOutputStream(ARQUIVO_PARTICIPANTES))) {
            oosParticipantes.writeObject(participantes);
            System.out.println("Participantes salvos.");
        } catch (IOException e) {
            System.out.println("Erro ao salvar participantes: " + e.getMessage());
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

        // 2. Validação quantidade de oficinas
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

        for (String cpf : oficina.getCpfsInscritos()) {
            // Buscando o objeto participante pelo CPF (Ineficiente O(N), mas OK para escopo
            // simples)
            for (Participante p : participantes) {
                if (p.getCpf().equals(cpf)) {
                    if ("Menor de Idade".equals(p.getFaixaEtaria(dataReferencia))) {
                        menores.add(p.getNome());
                    }
                    break;
                }
            }
        }
        return menores;
    }

    // --- Consultas Estatísticas ---

    public Map<String, Double> gerarEstatisticasPorSexo() {
        Map<String, Double> stats = new HashMap<>();
        int total = participantes.size();
        if (total == 0)
            return stats;

        long masculinos = participantes.stream().filter(p -> p.getSexo().equalsIgnoreCase("Masculino")).count();
        long femininos = participantes.stream().filter(p -> p.getSexo().equalsIgnoreCase("Feminino")).count();

        // Assumindo apenas esses dois para simplificação, ou calculando %
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

        for (Oficina rx : oficinas.values()) {
            List<String> cpfs = rx.getCpfsInscritos();
            int totalInscritos = cpfs.size();

            if (totalInscritos == 0) {
                Map<String, Double> zeroStats = new HashMap<>();
                zeroStats.put("Menor de Idade", 0.0);
                zeroStats.put("Maior de Idade", 0.0);
                statsGeral.put(rx.getTitulo(), zeroStats);
                continue;
            }

            int menores = 0;
            int maiores = 0;

            for (String cpf : cpfs) {
                for (Participante p : participantes) {
                    if (p.getCpf().equals(cpf)) {
                        String faixa = p.getFaixaEtaria(dataReferencia);
                        if ("Menor de Idade".equals(faixa))
                            menores++;
                        else
                            maiores++;
                        break;
                    }
                }
            }

            Map<String, Double> officeStats = new HashMap<>();
            officeStats.put("Menor de Idade", (double) menores / totalInscritos * 100);
            officeStats.put("Maior de Idade", (double) maiores / totalInscritos * 100);
            statsGeral.put(rx.getTitulo(), officeStats);
        }
        return statsGeral;
    }
}
