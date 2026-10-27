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
import org.hibernate.Session;

import hibernate.enums.EstatPartida;
import hibernate.enums.TipusRecurs;
import hibernate.enums.TipusTerreny;
import hibernate.model.Assentament;
import hibernate.model.Ciutat;
import hibernate.model.Hexagon;
import hibernate.model.HexagonVertex;
import hibernate.model.Jugador;
import hibernate.model.Partida;
import hibernate.util.HibernateUtil;
import hibernate.model.Poblat;
import hibernate.model.RecursJugador;
import hibernate.model.Vertex;

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
        Session session = sessionFactory.getCurrentSession();
        Partida partida = session.get(Partida.class, idPartida);
        if (partida == null) {
            throw new RuntimeException("La partida no exsisteix.");
        }
        if (partida.getEstat() != EstatPartida.EN_CURS) {
            throw new RuntimeException("La partida no esta en curs.");
        }
        if (partida.isDausTirats()) {
            throw new RuntimeException("Ja s'han tirat els daus en aquest torn.");
        }

        int dau1 = (int) (Math.random() * 6) + 1;
        int dau2 = (int) (Math.random() * 6) + 1;
        int tirada = dau1 + dau2;
        partida.setUltimaTirada(tirada);
        partida.setDausTirats(true);
        if(tirada == 7){
            // HACER MAS ADELANTE
            return;
        }

        List<Hexagon> hexagons = session.createQuery(
            "FROM Hexagon h "
          + "WHERE h.partida.id = :idPartida "
          + "AND h.numero = :numero "
          + "AND h.id <> :idLladre",Hexagon.class)
        .setParameter("idPartida", idPartida)
        .setParameter("numero", tirada)
        .setParameter("idLladre", partida.getLladre().getId())
        .getResultList();

        for (Hexagon hexagon : hexagons) {
            for (HexagonVertex hv : hexagon.getVertexs()) {
                Vertex vertex = hv.getVertex();
                Assentament assentament = session.createQuery(
                    "FROM Assentament a WHERE a.vertex.id = :vertexId",
                    Assentament.class)
                .setParameter("vertexId", vertex.getId())
                .uniqueResult();
                if (assentament == null) {
                    continue;
                }

                int quantitat;

                if (assentament instanceof Ciutat) {
                    quantitat = 2;
                } else if (assentament instanceof Poblat) {
                    quantitat = 1;
                } else {
                    continue;
                }

                Jugador propietari = assentament.getPropietari();
                TipusTerreny terreny = hexagon.getTipusTerreny();
                TipusRecurs tipus = obtenirRecurs(terreny);
                if (tipus == null) {
                    continue;
                }
                RecursJugador recursJugador = session.createQuery(
                    "FROM RecursJugador r "
                    + "WHERE r.jugador.id = :idJugador "
                    + "AND r.tipus = :tipus",
                    RecursJugador.class)
                    .setParameter("idJugador", propietari.getId())
                    .setParameter("tipus", tipus)
                    .uniqueResult();

                if (recursJugador == null) {
                    throw new RuntimeException("No exsisteix l'inventari del recurs " + 
                    tipus + " per el jugador " + propietari.getNom());
                }
                recursJugador.setQuantitat(recursJugador.getQuantitat() + quantitat);
                System.out.println(propietari.getNom() + " rep " + quantitat + " de " + tipus);
            }
        }
        System.out.println("Tirada: " + dau1 + " + " + dau2
                + " = " + tirada);
    }
    @Override
    public void ComprovarVictoria(int idPartida) {
        throw new UnsupportedOperationException("Unimplemented method 'ComprovarVictoria'");
    }

    private TipusRecurs obtenirRecurs(TipusTerreny terreny) {
        switch (terreny) {
            case BOSC:
                return TipusRecurs.FUSTA;
            case TURONS:
                return TipusRecurs.MAO;
            case PASTURES:
                return TipusRecurs.LLANA;
            case CAMPS:
                return TipusRecurs.BLAT;
            case MUNTANYES:
                return TipusRecurs.MINERAL;
            case DESERT:
                return null;
            default:
                throw new IllegalArgumentException("Terreny desconegut");
        }
    }
}
