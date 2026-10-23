package hibernate.model;

import hibernate.enums.TipusCarta;
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
import jakarta.validation.constraints.Min;

@Entity
@Table(name = "carta_desenvolupament")
public class CartaDesenvolupament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipusCarta tipus;

    @Column(nullable = false)
    private boolean usada;
    
    @Min(0)
    @Column(name = "ronda_compra", nullable = false)
    private Integer rondaCompra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jugador_id")
    private Jugador jugador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partida_id", nullable = false)
    private Partida partida;

    public CartaDesenvolupament(int id, TipusCarta tipus, boolean usada, Integer rondaCompra, Jugador jugador,
            Partida partida) {
        this.id = id;
        this.tipus = tipus;
        this.usada = usada;
        this.rondaCompra = rondaCompra;
        this.jugador = jugador;
        this.partida = partida;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public TipusCarta getTipus() {
        return tipus;
    }

    public void setTipus(TipusCarta tipus) {
        this.tipus = tipus;
    }

    public boolean isUsada() {
        return usada;
    }

    public void setUsada(boolean usada) {
        this.usada = usada;
    }

    public Integer getRondaCompra() {
        return rondaCompra;
    }

    public void setRondaCompra(Integer rondaCompra) {
        this.rondaCompra = rondaCompra;
    }

    public Jugador getJugador() {
        return jugador;
    }

    public void setJugador(Jugador jugador) {
        this.jugador = jugador;
    }

    public Partida getPartida() {
        return partida;
    }

    public void setPartida(Partida partida) {
        this.partida = partida;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof CartaDesenvolupament altra)) return false;
        return getId() != 0 && getId() == altra.getId();
    }

    @Override
    public int hashCode() {
        return CartaDesenvolupament.class.hashCode();
    }

    @Override
    public String toString() {
        return "CartaDesenvolupament{id=" + id + ", tipus=" + tipus + ", usada=" + usada + ", rondaCompra="
                + rondaCompra + "}";
    }

    
}