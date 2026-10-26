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
@Table(name = "vertex")
public class Vertex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Min(0)
    @Column(nullable = false)
    private int codi;

    @Column(nullable = false)
    private double x;

    @Column(nullable = false)
    private double y;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partida_id")
    private Partida partida;

    public Vertex(int id, int codi, double x, double y) {
        this.id = id;
        this.codi = codi;
        this.x = x;
        this.y = y;
    }

    public Vertex() {
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

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (!(object instanceof Vertex altre))
            return false;
        return getId() != 0 && getId() == altre.getId();
    }

    @Override
    public int hashCode() {
        return Vertex.class.hashCode();
    }

    @Override
    public String toString() {
        return "Vertex{id=" + id + ", codi=" + codi + ", x=" + x + ", y=" + y + "}";
    }

}