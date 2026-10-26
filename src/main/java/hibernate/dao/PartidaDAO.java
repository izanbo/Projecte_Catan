package hibernate.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;

import org.hibernate.Session;
import org.hibernate.Transaction;

import hibernate.enums.ColorJugador;
import hibernate.model.Jugador;
import hibernate.model.Assentament;
import hibernate.model.Hexagon;
import hibernate.model.HexagonVertex;
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
        Session session = sessionFactory.getCurrentSession();
        try{
            session.beginTransaction();
            MostrarTauler(session, idPartida);
            session.getTransaction();
        }catch(RuntimeException e){
            handleException(session, e);
        }
    }

    private void MostrarTauler(Session session, int idPartida){
        String sql = "SELECT DISTINCT h FROM Hexagon h " +
                     "LEFT JOIN FETCH h.vertexs hv " +
                     "LEFT JOIN FETCH hv.vertex v " +
                     "WHERE h.partida.id = :idPartida " +
                     "ORDER BY h.codi ASC";
        
        Query<Hexagon> query = session.createQuery(sql, Hexagon.class);
        query.setParameter("idPartida", idPartida);

        List<Hexagon> hexagons = query.getResultList();

        if(hexagons.isEmpty()){
            System.out.println("No s'ha trobat hexàgons per a la partida amb ID: " + idPartida);
            return ;
        }

        System.out.println("    --------------------");
        System.out.println("    Tauler de la Partida (" + idPartida + ")");
        System.out.println("    --------------------");

        for(Hexagon hex : hexagons){
            boolean hiHaLladre = hex.getPartida().isLladre();
            System.out.printf("HExàgon [Codi: %d] - Terreny: %10s | Número: %-2s | Lladre: %s\n", hex.getCodi(), hex.getTipusTerreny(), (hex.getNumero() != null ? hex.getNumero(): "N/A"), (hiHaLladre ? "SÍ" : "NO"));

            List<HexagonVertex> hexVertexList = hex.getVertexs();
            if(hexVertexList == null || hexVertexList.isEmpty()){
                System.out.println("(Sense vèrtexs associats)");
            }else{
                for(HexagonVertex hv : hexVertexList){
                    int posicio = hv.getPosicio();
                    int vertexCodi = hv.getVertex().getCodi();
                    int vertexId = hv.getVertex().getId();

                    String sqlAssentament = "SELECT a FROM Assentament a Where a.vertex.id = :vertexId";
                    Assentament assentament = session.createQuery(sqlAssentament, Assentament.class).setParameter("vertexId", vertexId).uniqueResultOptional().orElse(null);

                    if (assentament != null) {
                        String tipusConstruccio = assentament.getClass().getSimpleName();
                        String propietariNom = assentament.getPropietari().getNom();

                        System.out.printf("Vèrtex pos %d (Codi %d): %s de %s\n", posicio, vertexCodi, tipusConstruccio, propietariNom);
                    }else{
                        System.out.printf("Vèrtex pos %d (Codi %d): [BUIT]\n", posicio, vertexCodi);
                    }
                }
            }
            System.out.println("--------------------");
        }
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
