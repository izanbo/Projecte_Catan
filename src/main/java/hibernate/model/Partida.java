package hibernate.model;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import hibernate.enums.EstatPartida;
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
@Table(name = "partida")
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstatPartida estat;

    @Column(name = "data_inici", nullable = false)
    private Date dataInici;

    @Column(nullable = false)
    private int ronda;

    @Column(name = "ultima_tirada")
    private int ultimaTirada;

    @Column(name = "punts_per_guanyar", nullable = false)
    private int puntsPerGuanyar;

    @Column(name = "daus_tirats", nullable = false)
    private boolean dausTirats;

    @Column(name = "lladre", nullable = true)
    private boolean lladre;

    @ManyToOne
    @JoinColumn(name = "guanyador_id", nullable = true)
    private Jugador guanyador;

    @ManyToOne
    @JoinColumn(name = "jugador_actiu_id", nullable = true)
    private Jugador jugadorActiu;

    @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Jugador> jugadors = new ArrayList<>();

    @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Hexagon> hexagons = new ArrayList<>();

    @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartaDesenvolupament> cartes = new ArrayList<>();

    public Partida(int id, EstatPartida estat, Date dataInici, int ronda, int ultimaTirada, int puntsPerGuanyar,
            boolean dausTirats, boolean lladre, Jugador guanyador, Jugador jugadorActiu, List<Jugador> jugadors,
            List<Hexagon> hexagons, List<CartaDesenvolupament> cartes) {
        this.id = id;
        this.estat = estat;
        this.dataInici = dataInici;
        this.ronda = ronda;
        this.ultimaTirada = ultimaTirada;
        this.puntsPerGuanyar = puntsPerGuanyar;
        this.dausTirats = dausTirats;
        this.lladre = lladre;
        this.guanyador = guanyador;
        this.jugadorActiu = jugadorActiu;
        this.jugadors = jugadors;
        this.hexagons = hexagons;
        this.cartes = cartes;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public EstatPartida getEstat() {
        return estat;
    }

    public void setEstat(EstatPartida estat) {
        this.estat = estat;
    }

    public Date getDataInici() {
        return dataInici;
    }

    public void setDataInici(Date dataInici) {
        this.dataInici = dataInici;
    }

    public int getRonda() {
        return ronda;
    }

    public void setRonda(int ronda) {
        this.ronda = ronda;
    }

    public int getUltimaTirada() {
        return ultimaTirada;
    }

    public void setUltimaTirada(int ultimaTirada) {
        this.ultimaTirada = ultimaTirada;
    }

    public int getPuntsPerGuanyar() {
        return puntsPerGuanyar;
    }

    public void setPuntsPerGuanyar(int puntsPerGuanyar) {
        this.puntsPerGuanyar = puntsPerGuanyar;
    }

    public boolean isDausTirats() {
        return dausTirats;
    }

    public void setDausTirats(boolean dausTirats) {
        this.dausTirats = dausTirats;
    }

    public boolean isLladre() {
        return lladre;
    }

    public void setLladre(boolean lladre) {
        this.lladre = lladre;
    }

    public Jugador getGuanyador() {
        return guanyador;
    }

    public void setGuanyador(Jugador guanyador) {
        this.guanyador = guanyador;
    }

    public Jugador getJugadorActiu() {
        return jugadorActiu;
    }

    public void setJugadorActiu(Jugador jugadorActiu) {
        this.jugadorActiu = jugadorActiu;
    }

    public List<Jugador> getJugadors() {
        return jugadors;
    }

    public void setJugadors(List<Jugador> jugadors) {
        this.jugadors = jugadors;
    }

    public List<Hexagon> getHexagons() {
        return hexagons;
    }

    public void setHexagons(List<Hexagon> hexagons) {
        this.hexagons = hexagons;
    }

    public List<CartaDesenvolupament> getCartes() {
        return cartes;
    }

    public void setCartes(List<CartaDesenvolupament> cartes) {
        this.cartes = cartes;
    }

    public void IniciarPartida() {
        this.estat = EstatPartida.PREPARACIO;
        this.dataInici = new Date(System.currentTimeMillis());
        this.ronda = 1;
        this.ultimaTirada = 0;
        this.puntsPerGuanyar = 10; // Valor per defecte, es pot canviar segons les regles del joc
        this.dausTirats = false;
        this.lladre = false;
    }
}