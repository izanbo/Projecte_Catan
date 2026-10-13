package hibernate.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "carretera")
public class Carretera extends Construccio {

    @OneToOne(optional = false)
    @JoinColumn(name = "aresta_id", nullable = false, unique = true)
    private Aresta aresta;
}