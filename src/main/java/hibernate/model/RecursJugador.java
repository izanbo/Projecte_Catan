package hibernate.model;

import hibernate.enums.TipusRecurs;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "recurs_jugador", uniqueConstraints = @UniqueConstraint(name = "uk_recurs_jugador_tipus", columnNames = { "jugador_id", "tipus_recurs" }))
public class RecursJugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "jugador_id", nullable = false)
    private Jugador jugador;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipus_recurs", nullable = false)
    private TipusRecurs tipusRecurs;

    @Column(nullable = false)
    private int quantitat;

    public RecursJugador(int id, Jugador jugador, TipusRecurs tipusRecurs, int quantitat) {
        this.id = id;
        this.jugador = jugador;
        this.tipusRecurs = tipusRecurs;
        this.quantitat = quantitat;
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

    
}