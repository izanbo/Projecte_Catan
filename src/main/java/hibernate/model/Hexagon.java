package hibernate.model;

import hibernate.enums.TipusTerreny;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "hexagon")
public class Hexagon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private int codi;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipus_terreny", nullable = false)
    private TipusTerreny tipusTerreny;

    @Column(name = "numero")
    private Integer numero;
}