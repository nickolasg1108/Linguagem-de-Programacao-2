
import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Participante implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nome;
    private String cpf;
    private String sexo; // Poderia ser Enum, mas String atende "Masculino"/"Feminino"
    private LocalDate dataNascimento;
    private List<String> titulosOficinasInscritas;

    public Participante(String nome, String cpf, String sexo, LocalDate dataNascimento) {
        this.nome = nome;
        this.cpf = cpf;
        this.sexo = sexo;
        this.dataNascimento = dataNascimento;
        this.titulosOficinasInscritas = new ArrayList<>();
    }

    // Getters e Setters
    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getSexo() {
        return sexo;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public List<String> getTitulosOficinasInscritas() {
        return titulosOficinasInscritas;
    }

    public void setTitulosOficinasInscritas(List<String> titulosOficinasInscritas) {
        this.titulosOficinasInscritas = titulosOficinasInscritas;
    }

    // --- Métodos de Lógica ---

    public int calcularIdade(LocalDate dataReferencia) {
        if (dataNascimento == null || dataReferencia == null) {
            return 0; 
        }
        return Period.between(dataNascimento, dataReferencia).getYears();
    }

    public String getFaixaEtaria(LocalDate dataReferencia) {
        int idade = calcularIdade(dataReferencia);
        if (idade < 18) {
            return "Menor de Idade";
        } else {
            return "Maior de Idade";
        }
    }
    
    public void adicionarOficina(String tituloOficina) {
        if (!titulosOficinasInscritas.contains(tituloOficina)) {
             titulosOficinasInscritas.add(tituloOficina);
        }
    }

    @Override
    public String toString() {
        return "Participante{" +
                "nome='" + nome + '\'' +
                ", cpf='" + cpf + '\'' +
                ", sexo='" + sexo + '\'' +
                '}';
    }
}
