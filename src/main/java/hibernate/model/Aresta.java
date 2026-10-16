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
}