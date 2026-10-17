package hibernate.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "ciutat")
public class Ciutat extends Assentament {

    public Ciutat(Jugador jugador, Vertex vertex) {
        super(jugador, vertex);
    }
}