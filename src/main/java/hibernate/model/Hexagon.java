package hibernate.model;

import java.util.ArrayList;
import java.util.List;

import hibernate.enums.TipusTerreny;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "hexagon")
public class Hexagon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // TODO: validacio: codi únic per partida
    @Column(nullable = false)
    private int codi;

    @Column(nullable = false)
    private int q;

    @Column(nullable = false)
    private int r;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipus_terreny", nullable = false)
    private TipusTerreny tipusTerreny;

    @Column(name = "numero")
    private Integer numero;

    @ManyToOne(optional = false)
    @JoinColumn(name = "partida_id", nullable = false)
    private Partida partida;

    // Validar
    @OneToMany(mappedBy = "hexagon", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HexagonVertex> vertexs = new ArrayList<>();
}