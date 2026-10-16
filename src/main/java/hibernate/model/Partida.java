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

    @Column(name = "lladre_pendent", nullable = false)
    private boolean lladrePendent;

    @ManyToOne
    @JoinColumn(name = "guanyador_id")
    private Jugador guanyador;

    @ManyToOne
    @JoinColumn(name = "jugador_actiu_id")
    private Jugador jugadorActiu;

    @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Jugador> jugadors = new ArrayList<>();

    @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Hexagon> hexagons = new ArrayList<>();

    @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartaDesenvolupament> cartes = new ArrayList<>();
}