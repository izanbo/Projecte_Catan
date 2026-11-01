package hibernate.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "assentament")
@NamedQueries({
    @NamedQuery(
        name = "Assentament.findByVertex",
        query = "SELECT a FROM Assentament a WHERE a.vertex.id = :idVertex"
    )
})
public abstract class Assentament extends Construccio {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vertex_id", nullable = false, unique = true)
    private Vertex vertex;

    public Assentament() {
    }

    public Assentament(Jugador propietari, Vertex vertex) {
        super(propietari);
        this.vertex = vertex;
    }

    public Vertex getVertex() {
        return vertex;
    }

    public void setVertex(Vertex vertex) {
        this.vertex = vertex;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + getId() + "}";
    }
}
