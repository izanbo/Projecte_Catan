package hibernate.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "assentament")
public abstract class Assentament extends Construccio {

    public Assentament(int id, Jugador propietari) {
        super(id, propietari);
    }

    @OneToOne(optional = false)
    @JoinColumn(name = "vertex_id", nullable = false, unique = true)
    private Vertex vertex;

    public Vertex getVertex() {
        return vertex;
    }

    public void setVertex(Vertex vertex) {
        this.vertex = vertex;
    }
}