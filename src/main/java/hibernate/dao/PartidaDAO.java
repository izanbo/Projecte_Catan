package hibernate.dao;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;

import hibernate.enums.ColorJugador;
import hibernate.model.Jugador;
import hibernate.model.Partida;
import hibernate.util.HibernateUtil;

public class PartidaDAO extends GenericDAO<Partida, Integer> implements IPartidaDao {

    @Override
    public int IniciarPartida(List<String> nomsJugadors) {

        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();

        try {
            Partida partida = new Partida();
            partida.IniciarPartida();

            session.persist(partida);

            ColorJugador[] colors = ColorJugador.values();
            for (int i = 0; i < nomsJugadors.size(); i++) {
                Jugador jugador = new Jugador(nomsJugadors.get(i), colors[i], i + 1, partida);
                partida.addJugador(jugador);
            }

            partida.setJugadorActiu(partida.getJugadors().get(0));
            HibernateUtil.carregarDades(session, partida);

            int idPartida = partida.getId();
            tx.commit();
            return idPartida;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    @Override
    public void MostrarTauler(int idPartida) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'MostrarTauler'");
    }

    @Override
    public void MostrarRecursos(int idJugador) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'MostrarRecursos'");
    }

    @Override
    public void MostrarPartida(int idPartida) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'MostrarPartida'");
    }

    @Override
    public void TirarDaus(int idPartida) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'TirarDaus'");
    }

    @Override
    public void ComprovarVictoria(int idPartida) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'ComprovarVictoria'");
    }

}
