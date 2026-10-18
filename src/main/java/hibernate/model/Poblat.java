package hibernate.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "poblat")
public class Poblat extends Assentament {

    public Poblat() {
    }

    public Poblat(Jugador propietari, Vertex vertex) {
        super(propietari, vertex);
    }
}