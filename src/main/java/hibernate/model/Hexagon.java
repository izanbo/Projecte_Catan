package hibernate.model;

import java.util.ArrayList;
import java.util.List;

import hibernate.enums.TipusTerreny;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "hexagon", uniqueConstraints = @UniqueConstraint(columnNames = { "codi", "partida_id" }))
public class Hexagon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Min(0)
    @Column(nullable = false)
    private int codi;

    @Column(nullable = false)
    private int q;

    @Column(nullable = false)
    private int r;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipus_terreny", nullable = false)
    private TipusTerreny tipusTerreny;

    @Min(2)
    @Max(12)
    @Column(name = "numero")
    private Integer numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partida_id", nullable = false)
    @NotNull
    private Partida partida;

    @NotNull
    @Size(min = 6, max = 6)
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

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Hexagon altre)) return false;
        return getId() != 0 && getId() == altre.getId();
    }

    @Override
    public int hashCode() {
        return Hexagon.class.hashCode();
    }

    @Override
    public String toString() {
        return "Hexagon{id=" + id + ", codi=" + codi + ", q=" + q + ", r=" + r + ", tipusTerreny="
                + tipusTerreny + ", numero=" + numero + "}";
    }

}