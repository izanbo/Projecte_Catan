package hibernate.dao;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import hibernate.enums.EstatPartida;
import hibernate.enums.TipusRecurs;
import hibernate.enums.TipusTerreny;
import hibernate.model.Jugador;
import hibernate.model.Poblat;
import hibernate.model.RecursJugador;
import hibernate.model.Vertex;

public class JugadorDao extends GenericDAO<Jugador, Integer> implements IJugadorDao {

    @Override
    public void MostrarRecursos(int idJugador) {
        Session session = sessionFactory.getCurrentSession();
        try {
            session.beginTransaction();
            MostrarRecursos(session, idJugador);
            session.getTransaction().commit();
        } catch (RuntimeException e) {
            handleException(session, e);
        }
    }

    private void MostrarRecursos(Session session, int idJugador) {
        Jugador jugador = session.get(Jugador.class, idJugador);
        if (jugador == null) {
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
        } else {
            for (RecursJugador r : recursos) {
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
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();

        try {
            // Comprova que existeixin el jugador i el vèrtex, i que sigui el torn del jugador.
            Jugador jugador = session.get(Jugador.class, idJugador);
            Vertex vertex = session.get(Vertex.class, idVertex);
            if (jugador == null || vertex == null || jugador.getPartida().getEstat() == EstatPartida.FINALITZADA || jugador.getPartida().getJugadorActiu() == null || jugador.getPartida().getJugadorActiu().getId() != idJugador) {
                tx.rollback();
                return false;
            }

            // Comprova el límit de poblats i que el vèrtex estigui lliure i compleixi la distància.
            boolean preparacio = jugador.getPartida().getEstat() == EstatPartida.PREPARACIO;
            Long poblats = session.createQuery(
                    "select count(p) from Poblat p where p.propietari.id = :idJugador",
                    Long.class)
                    .setParameter("idJugador", idJugador)
                    .getSingleResult();
            Long assentamentsAlVertexOVeins = session.createQuery(
                    "select count(a) from Assentament a where a.vertex.id = :idVertex "
                        + "or a.vertex.id in (select e.vertexB.id from Aresta e where e.vertexA.id = :idVertex) "
                        + "or a.vertex.id in (select e.vertexA.id from Aresta e where e.vertexB.id = :idVertex)",
                    Long.class)
                    .setParameter("idVertex", idVertex)
                    .getSingleResult();
            if (poblats >= 5 || assentamentsAlVertexOVeins > 0) {
                tx.rollback();
                return false;
            }

            // Fora de la preparació, el poblat ha d'estar connectat a una carretera pròpia.
            if (!preparacio) {
                Long carreteresPropies = session.createQuery(
                        "select count(c) from Carretera c "
                                + "where c.propietari.id = :idJugador "
                                + "and (c.aresta.vertexA.id = :idVertex or c.aresta.vertexB.id = :idVertex)",
                        Long.class)
                        .setParameter("idJugador", idJugador)
                        .setParameter("idVertex", idVertex)
                        .getSingleResult();
                if (carreteresPropies == 0) {
                    tx.rollback();
                    return false;
                }
            }

            List<RecursJugador> recursos = session.createQuery(
                    "select r from RecursJugador r where r.jugador.id = :idJugador",
                    RecursJugador.class)
                    .setParameter("idJugador", idJugador)
                    .getResultList();

            // Comprova que tingui els recursos necessaris i, després, els descompta.
            if (!preparacio) {
                TipusRecurs[] cost = { TipusRecurs.FUSTA, TipusRecurs.MAO, TipusRecurs.LLANA, TipusRecurs.BLAT };
                for (TipusRecurs tipus : cost) {
                    RecursJugador recurs = trobarRecurs(recursos, tipus);
                    if (recurs == null || recurs.getQuantitat() < 1) {
                        tx.rollback();
                        return false;
                    }
                }
                for (TipusRecurs tipus : cost) {
                    RecursJugador recurs = trobarRecurs(recursos, tipus);
                    recurs.setQuantitat(recurs.getQuantitat() - 1);
                }
            }

            // Crea el poblat i, si és el segon de la preparació, reparteix recursos dels hexàgons veïns.
            session.persist(new Poblat(jugador, vertex));
            if (preparacio && poblats == 1) {
                List<TipusTerreny> terrenysVeins = session.createQuery(
                        "select hv.hexagon.tipusTerreny from HexagonVertex hv where hv.vertex.id = :idVertex",
                        TipusTerreny.class)
                        .setParameter("idVertex", idVertex)
                        .getResultList();
                for (TipusTerreny terreny : terrenysVeins) {
                    TipusRecurs tipus = recursDelTerreny(terreny);
                    if (tipus != null) {
                        afegirRecurs(session, jugador, recursos, tipus);
                    }
                }
            }

            tx.commit();
            return true;
        } catch (RuntimeException e) {
            handleException(session, e);
            return false;
        }
    }

    private RecursJugador trobarRecurs(List<RecursJugador> recursos, TipusRecurs tipus) {
        // Busca al jugador l'entrada de l'inventari del tipus indicat.
        return recursos.stream()
                .filter(recurs -> recurs.getTipusRecurs() == tipus)
                .findFirst()
                .orElse(null);
    }

    private TipusRecurs recursDelTerreny(TipusTerreny terreny) {
        // Converteix el tipus de terreny en el recurs que produeix.
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
                return null;
        }
    }

    private void afegirRecurs(Session session, Jugador jugador, List<RecursJugador> recursos, TipusRecurs tipus) {
        // Crea l'entrada de recurs si no existeix; si ja existeix, n'augmenta la quantitat.
        RecursJugador recurs = trobarRecurs(recursos, tipus);
        if (recurs == null) {
            recurs = new RecursJugador(0, jugador, tipus, 1);
            session.persist(recurs);
            recursos.add(recurs);
        } else {
            recurs.setQuantitat(recurs.getQuantitat() + 1);
        }
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
