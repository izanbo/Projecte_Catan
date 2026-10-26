package hibernate;

import java.util.List;

import hibernate.dao.PartidaDAO;
import hibernate.util.HibernateUtil;

public class Main {
    public static void main(String[] args) {
        int idPartida = new PartidaDAO().IniciarPartida(List.of("Jugador 1", "Jugador 2"));
        System.out.println("Partida creada amb ID: " + idPartida);

        HibernateUtil.shutdown();
    }
}