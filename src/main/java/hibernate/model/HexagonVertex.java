package hibernate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "hexagon_vertex", uniqueConstraints = @UniqueConstraint(name = "uk_hexagon_vertex_posicio", columnNames = { "hexagon_id", "posicio" }))
public class HexagonVertex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "hexagon_id", nullable = false)
    private Hexagon hexagon;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vertex_id", nullable = false)
    private Vertex vertex;

    @Column(nullable = false)
    private int posicio;

    public HexagonVertex(int id, Hexagon hexagon, Vertex vertex, int posicio) {
        this.id = id;
        this.hexagon = hexagon;
        this.vertex = vertex;
        this.posicio = posicio;
    }

    public HexagonVertex() {
        //TODO Auto-generated constructor stub
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Hexagon getHexagon() {
        return hexagon;
    }

    public void setHexagon(Hexagon hexagon) {
        this.hexagon = hexagon;
    }

    public Vertex getVertex() {
        return vertex;
    }

    public void setVertex(Vertex vertex) {
        this.vertex = vertex;
    }

    public int getPosicio() {
        return posicio;
    }

    public void setPosicio(int posicio) {
        this.posicio = posicio;
    }

    
}