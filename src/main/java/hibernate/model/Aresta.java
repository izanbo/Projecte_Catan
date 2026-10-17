package hibernate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "aresta")
public class Aresta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private int codi;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vertex_a_id", nullable = false)
    private Vertex vertexA;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vertex_b_id", nullable = false)
    private Vertex vertexB;

    public Aresta(int id, int codi, Vertex vertexA, Vertex vertexB) {
        this.id = id;
        this.codi = codi;
        this.vertexA = vertexA;
        this.vertexB = vertexB;
    }

    public Aresta() {
        //TODO Auto-generated constructor stub
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

}