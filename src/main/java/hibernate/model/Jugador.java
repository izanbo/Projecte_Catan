package hibernate.model;

import java.util.ArrayList;
import java.util.List;

import hibernate.enums.ColorJugador;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "jugador", uniqueConstraints = @UniqueConstraint(name = "uk_jugador_partida_color", columnNames = {
        "partida_id", "color" }))
public class Jugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String nom;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ColorJugador color;

    @Column(name = "ordre_torn", nullable = false)
    @Min(0)
    private int ordreTorn;

    @Column(name = "cavallers_jugats", nullable = false)
    @Min(0)
    private int cavallersJugats;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partida_id", nullable = false)
    @NotNull
    private Partida partida;

    @OneToMany(mappedBy = "jugador", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecursJugador> recursos = new ArrayList<>();

    // Valorar cascade y orphanRemoval
    @OneToMany(mappedBy = "propietari", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Construccio> construccions = new ArrayList<>();

    @OneToMany(mappedBy = "jugador", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartaDesenvolupament> cartas = new ArrayList<>();

    public Jugador() {
    }

    public Jugador(String nom, ColorJugador color, int ordreTorn, Partida partida) {
        this.nom = nom;
        this.color = color;
        this.ordreTorn = ordreTorn;
        this.partida = partida;
        this.cavallersJugats = 0;
        this.recursos = new ArrayList<>();
        this.construccions = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public ColorJugador getColor() {
        return color;
    }

    public void setColor(ColorJugador color) {
        this.color = color;
    }

    public int getOrdreTorn() {
        return ordreTorn;
    }

    public void setOrdreTorn(int ordreTorn) {
        this.ordreTorn = ordreTorn;
    }

    public int getCavallersJugats() {
        return cavallersJugats;
    }

    public void setCavallersJugats(int cavallersJugats) {
        this.cavallersJugats = cavallersJugats;
    }

    public Partida getPartida() {
        return partida;
    }

    public void setPartida(Partida partida) {
        this.partida = partida;
    }

    public List<RecursJugador> getRecursos() {
        return recursos;
    }

    public void setRecursos(List<RecursJugador> recursos) {
        this.recursos = recursos;
    }

    public List<Construccio> getConstruccions() {
        return construccions;
    }

    public void setConstruccions(List<Construccio> construccions) {
        this.construccions = construccions;
    }

    public List<CartaDesenvolupament> getCartas() {
        return cartas;
    }

    public void setCartas(List<CartaDesenvolupament> cartas) {
        this.cartas = cartas;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Jugador altre)) return false;
        return getId() != 0 && getId() == altre.getId();
    }

    @Override
    public int hashCode() {
        return Jugador.class.hashCode();
    }

    @Override
    public String toString() {
        return "Jugador{id=" + id + ", nom='" + nom + "', color=" + color + ", ordreTorn=" + ordreTorn
                + ", cavallersJugats=" + cavallersJugats + "}";
    }

}