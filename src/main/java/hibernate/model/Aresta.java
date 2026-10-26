package hibernate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;

@Entity
@Table(name = "aresta")
public class Aresta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Min(0)
    @Column(nullable = false)
    private int codi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vertex_a_id", nullable = false)
    private Vertex vertexA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vertex_b_id", nullable = false)
    private Vertex vertexB;

    @ManyToOne
    @JoinColumn(name = "partida_id")
    private Partida partida;

    public Aresta(int id, int codi, Vertex vertexA, Vertex vertexB) {
        this.id = id;
        this.codi = codi;
        this.vertexA = vertexA;
        this.vertexB = vertexB;
    }

    public Aresta() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCodi() {
        return codi;
    }

    public void setCodi(int codi) {
        this.codi = codi;
    }

    public Vertex getVertexA() {
        return vertexA;
    }

    public void setVertexA(Vertex vertexA) {
        this.vertexA = vertexA;
    }

    public Vertex getVertexB() {
        return vertexB;
    }

    public void setVertexB(Vertex vertexB) {
        this.vertexB = vertexB;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (!(object instanceof Aresta altra))
            return false;
        return getId() != 0 && getId() == altra.getId();
    }

    @Override
    public int hashCode() {
        return Aresta.class.hashCode();
    }

    @Override
    public String toString() {
        return "Aresta{id=" + id + ", codi=" + codi + "}";
    }

}