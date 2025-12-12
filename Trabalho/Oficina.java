
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Oficina implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String titulo;
    private final int vagasMaximas = 30; // Conforme especificado: final int = 30
    private int vagasOcupadas;
    private List<String> cpfsInscritos;

    public Oficina(String titulo) {
        this.titulo = titulo;
        this.vagasOcupadas = 0;
        this.cpfsInscritos = new ArrayList<>();
    }

    // Getters e Setters
    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public int getVagasMaximas() {
        return vagasMaximas;
    }

    public int getVagasOcupadas() {
        return vagasOcupadas;
    }

    // O setVagasOcupadas geralmente não é exposto publicamente para evitar inconsistência,
    // mas se necessário para serialização ou outro fim:
    public void setVagasOcupadas(int vagasOcupadas) {
        this.vagasOcupadas = vagasOcupadas;
    }

    public List<String> getCpfsInscritos() {
        return cpfsInscritos;
    }

    public void setCpfsInscritos(List<String> cpfsInscritos) {
        this.cpfsInscritos = cpfsInscritos;
    }

    // --- Métodos de Lógica Interna ---

    /**
     * Tenta adicionar um CPF à oficina.
     * Incrementa vagasOcupadas se houver espaço e o CPF não estiver inscrito.
     * @param cpf CPF do participante
     * @return true se adicionado com sucesso, false caso contrário (lotado ou já inscrito)
     */
    public boolean adicionarInscrito(String cpf) {
        if (vagasOcupadas < vagasMaximas && !cpfsInscritos.contains(cpf)) {
            cpfsInscritos.add(cpf);
            vagasOcupadas++;
            return true;
        }
        return false;
    }

    /**
     * Remove um CPF da oficina.
     * Decrementa vagasOcupadas.
     * @param cpf CPF do participante
     * @return true se removido com sucesso, false se não encontrado
     */
    public boolean removerInscrito(String cpf) {
        if (cpfsInscritos.remove(cpf)) {
            vagasOcupadas--;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Oficina{" +
                "titulo='" + titulo + '\'' +
                ", vagas=" + vagasOcupadas + "/" + vagasMaximas +
                '}';
    }
}
