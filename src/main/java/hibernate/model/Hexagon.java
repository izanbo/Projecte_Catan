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

    public Hexagon(int id, int codi, int q, int r, TipusTerreny tipusTerreny, Integer numero, Partida partida,
            List<HexagonVertex> vertexs) {
        this.id = id;
        this.codi = codi;
        this.q = q;
        this.r = r;
        this.tipusTerreny = tipusTerreny;
        this.numero = numero;
        this.partida = partida;
        this.vertexs = vertexs;
    }

    public Hexagon() {
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

    public int getQ() {
        return q;
    }

    public void setQ(int q) {
        this.q = q;
    }

    public int getR() {
        return r;
    }

    public void setR(int r) {
        this.r = r;
    }

    public TipusTerreny getTipusTerreny() {
        return tipusTerreny;
    }

    public void setTipusTerreny(TipusTerreny tipusTerreny) {
        this.tipusTerreny = tipusTerreny;
    }

    public Integer getNumero() {
        return numero;
    }

    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public Partida getPartida() {
        return partida;
    }

    public void setPartida(Partida partida) {
        this.partida = partida;
    }

    public List<HexagonVertex> getVertexs() {
        return vertexs;
    }

    public void setVertexs(List<HexagonVertex> vertexs) {
        this.vertexs = vertexs;
    }

}