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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "partida")
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstatPartida estat;

    @NotNull
    @Column(name = "data_inici", nullable = false)
    private Date dataInici;

    @Column(nullable = false)
    @Min(0)
    private int ronda;

    @Min(0)
    @Max(12)
    @Column(name = "ultima_tirada")
    private int ultimaTirada;

    @Min(1)
    @Column(name = "punts_per_guanyar", nullable = false)
    private int puntsPerGuanyar;

    @Column(name = "daus_tirats", nullable = false)
    private boolean dausTirats;

    @Column(name = "lladrePendent", nullable = true)
    private boolean lladrePendent;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hexagon_lladre_id")
    private Hexagon hexagonLladre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guanyador_id", nullable = true)
    private Jugador guanyador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jugador_actiu_id", nullable = true)
    private Jugador jugadorActiu;

    @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Jugador> jugadors = new ArrayList<>();

    @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Hexagon> hexagons = new ArrayList<>();

    @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartaDesenvolupament> cartes = new ArrayList<>();

    @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vertex> vertexs = new ArrayList<>();

    @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Aresta> arestes = new ArrayList<>();

    public Partida(int id, EstatPartida estat, Date dataInici, int ronda, int ultimaTirada, int puntsPerGuanyar,
            boolean dausTirats, boolean lladre, Jugador guanyador, Jugador jugadorActiu, List<Jugador> jugadors,
            List<Hexagon> hexagons, List<CartaDesenvolupament> cartes, List<Vertex> vertexs, List<Aresta> arestes) {
        this.id = id;
        this.estat = estat;
        this.dataInici = dataInici;
        this.ronda = ronda;
        this.ultimaTirada = ultimaTirada;
        this.puntsPerGuanyar = puntsPerGuanyar;
        this.dausTirats = dausTirats;
        this.lladrePendent = lladre;
        this.guanyador = guanyador;
        this.jugadorActiu = jugadorActiu;
        this.jugadors = jugadors;
        this.hexagons = hexagons;
        this.cartes = cartes;
        this.vertexs = vertexs;
        this.arestes = arestes;
    }

    public Partida() {
        
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
        return lladrePendent;
    }

    public void setLladre(boolean lladre) {
        this.lladrePendent = lladre;
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

    public Hexagon getHexagonLladre() {
        return hexagonLladre;
    }

    public void setHexagonLladre(Hexagon hexagonLladre) {
        this.hexagonLladre = hexagonLladre;
    }

    public void addJugador(Jugador j) {
        this.jugadors.add(j);
        j.setPartida(this);
    }

    public void addHexagon(Hexagon hexagon) {
        this.hexagons.add(hexagon);
        hexagon.setPartida(this);
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

    public List<Vertex> getVertexs() {
        return vertexs;
    }

    public void setVertexs(List<Vertex> vertexs) {
        this.vertexs = vertexs;
    }

    public List<Aresta> getArestes() {
        return arestes;
    }

    public void setArestes(List<Aresta> arestes) {
        this.arestes = arestes;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Partida altra)) return false;
        return getId() != 0 && getId() == altra.getId();
    }

    @Override
    public int hashCode() {
        return Partida.class.hashCode();
    }

    @Override
    public String toString() {
        return "Partida{id=" + id + ", estat=" + estat + ", dataInici=" + dataInici + ", ronda=" + ronda
                + ", ultimaTirada=" + ultimaTirada + ", puntsPerGuanyar=" + puntsPerGuanyar + ", dausTirats="
                + dausTirats + ", lladre=" + lladrePendent + "}";
    }

    public void IniciarPartida() {
        inicialitzarValorsInicials();
    }

    private void inicialitzarValorsInicials() {
        this.estat = EstatPartida.PREPARACIO;
        this.dataInici = new Date(System.currentTimeMillis());
        this.ronda = 1;
        this.ultimaTirada = 0;
        this.puntsPerGuanyar = 10;
        this.dausTirats = false;
        this.lladrePendent = false;
    }
}