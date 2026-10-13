package hibernate.model;

import hibernate.enums.ColorJugador;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "jugador",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_jugador_partida_color",
           columnNames = {"partida_id", "color"}))
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
}