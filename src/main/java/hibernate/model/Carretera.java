package hibernate.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "carretera")
public class Carretera extends Construccio {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aresta_id", nullable = false, unique = true)
    @NotNull
    private Aresta aresta;

    public Carretera() {
    }

    public Carretera(Jugador propietari, Aresta aresta) {
        super(propietari);
        this.aresta = aresta;
    }

    public Aresta getAresta() {
        return aresta;
    }

    public void setAresta(Aresta aresta) {
        this.aresta = aresta;
    }

    @Override
    public String toString() {
        return "Carretera{id=" + getId() + "}";
    }
}