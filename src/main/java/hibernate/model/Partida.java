package hibernate.model;

import java.sql.Date;

import hibernate.enums.EstatPartida;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
}