package hibernate.model;

import java.util.ArrayList;
import java.util.List;

import hibernate.enums.ColorJugador;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "jugador", uniqueConstraints = @UniqueConstraint(name = "uk_jugador_partida_color", columnNames = { "partida_id", "color" }))
public class Jugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, length = 50)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ColorJugador color;

    @Column(name = "ordre_torn", nullable = false)
    private int ordreTorn;

    @Column(name = "cavallers_jugats", nullable = false)
    private int cavallersJugats;

    @ManyToOne(optional = false)
    @JoinColumn(name = "partida_id", nullable = false)
    private Partida partida;

    @OneToMany(mappedBy = "jugador", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecursJugador> recursos = new ArrayList<>();

    @OneToMany(mappedBy = "propietari")
    private List<Construccio> construccions = new ArrayList<>();
}