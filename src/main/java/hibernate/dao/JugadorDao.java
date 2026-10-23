package hibernate.dao;


import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.Query;
import hibernate.enums.TipusRecurs;
import hibernate.model.Jugador;
import hibernate.model.RecursJugador;

public class JugadorDao extends GenericDAO<Jugador, Integer> implements IJugadorDao{

    @Override
    public void MostrarRecursos(int idJugador) {
        Session session = sessionFactory.getCurrentSession();
        try{
            session.beginTransaction();
            MostrarRecursos(session, idJugador);
            session.getTransaction().commit();
        }catch(RuntimeException e){
            handleException(session, e);
        }
    }

    private void MostrarRecursos(Session session, int idJugador){
        Jugador jugador = session.get(Jugador.class, idJugador);
        if(jugador == null){
            System.out.println("No s'ha trobat cap jugador amb ID: " + idJugador);
            return;
        }

        String sql = "SELECT r FROM RecursJugador R WHERE r.jugador.id = :idJugador";
        Query<RecursJugador> query = session.createQuery(sql, RecursJugador.class);
        query.setParameter("idJugador", idJugador);

        List<RecursJugador> recursos = query.getResultList();

        System.out.println("    --------------------");
        System.out.println("    Inventari de Recursos - " + jugador.getNom() + "(Color: " + jugador.getColor() + ")");
        System.out.println("    --------------------");

        if (recursos.isEmpty()) {
            System.out.println("El jugador no té cap recurs actualment.");
        }else{
            for(RecursJugador r : recursos){
                System.out.printf("- %-10s: %d\n", r.getTipusRecurs(), r.getQuantitat());
            }
        }
        System.out.println("--------------------");
    }

    @Override
    public boolean ConstruirCarretera(int idJugador, int idAresta) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'ConstruirCarretera'");
    }

    @Override
    public boolean ConstruirPoblat(int idJugador, int idVertex) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'ConstruirPoblat'");
    }

    @Override
    public boolean MillorarACiutat(int idJugador, int idVertex) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'MillorarACiutat'");
    }

    @Override
    public TipusRecurs ComerciarBanca(int idJugador, TipusRecurs dona, TipusRecurs rep) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'ComerciarBanca'");
    }

    @Override
    public int CalcularPunts(int idJugador) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'CalcularPunts'");
    }

    @Override
    public void PassarTorn(int idJugador) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'PassarTorn'");
    }

}
