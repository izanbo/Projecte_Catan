package hibernate.model;

import hibernate.enums.TipusCarta;
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
    // TODO: afegir validacio
    @Column(name = "ronda_compra")
    private Integer rondaCompra;

    @ManyToOne
    @JoinColumn(name = "jugador_id")
    private Jugador jugador;

    @ManyToOne(optional = false)
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

    
}