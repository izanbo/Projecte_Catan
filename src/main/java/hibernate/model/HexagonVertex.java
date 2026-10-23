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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "hexagon_vertex", uniqueConstraints = @UniqueConstraint(name = "uk_hexagon_vertex_posicio", columnNames = { "hexagon_id", "posicio" }))
public class HexagonVertex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hexagon_id", nullable = false)
    @NotNull
    private Hexagon hexagon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vertex_id", nullable = false)
    @NotNull
    private Vertex vertex;

    @Min(0)
    @Max(5)
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

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof HexagonVertex altre)) return false;
        return getId() != 0 && getId() == altre.getId();
    }

    @Override
    public int hashCode() {
        return HexagonVertex.class.hashCode();
    }

    @Override
    public String toString() {
        return "HexagonVertex{id=" + id + ", posicio=" + posicio + "}";
    }

    
}