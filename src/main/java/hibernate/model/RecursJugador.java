package hibernate.model;

import hibernate.enums.TipusRecurs;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "recurs_jugador", uniqueConstraints = @UniqueConstraint(name = "uk_recurs_jugador_tipus", columnNames = { "jugador_id", "tipus_recurs" }))
public class RecursJugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jugador_id", nullable = false)
    @NotNull
    private Jugador jugador;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipus_recurs", nullable = false)
    @NotNull
    private TipusRecurs tipusRecurs;

    @Min(0)
    @Max(19)
    @Column(nullable = false)
    @Min(0)
    private int quantitat;

    public RecursJugador(int id, Jugador jugador, TipusRecurs tipusRecurs, int quantitat) {
        this.id = id;
        this.jugador = jugador;
        this.tipusRecurs = tipusRecurs;
        this.quantitat = quantitat;
    }

    public RecursJugador() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Jugador getJugador() {
        return jugador;
    }

    public void setJugador(Jugador jugador) {
        this.jugador = jugador;
    }

    public TipusRecurs getTipusRecurs() {
        return tipusRecurs;
    }

    public void setTipusRecurs(TipusRecurs tipusRecurs) {
        this.tipusRecurs = tipusRecurs;
    }

    public int getQuantitat() {
        return quantitat;
    }

    public void setQuantitat(int quantitat) {
        this.quantitat = quantitat;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof RecursJugador altre)) return false;
        return getId() != 0 && getId() == altre.getId();
    }

    @Override
    public int hashCode() {
        return RecursJugador.class.hashCode();
    }

    @Override
    public String toString() {
        return "RecursJugador{id=" + id + ", tipusRecurs=" + tipusRecurs + ", quantitat=" + quantitat + "}";
    }

    
}