package hibernate.dao;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import hibernate.enums.EstatPartida;
import hibernate.enums.TipusCarta;
import hibernate.enums.TipusRecurs;
import hibernate.enums.TipusTerreny;
import hibernate.model.Aresta;
import hibernate.model.Assentament;
import hibernate.model.Carretera;
import hibernate.model.Ciutat;
import hibernate.model.Hexagon;
import hibernate.model.HexagonVertex;
import hibernate.model.Jugador;
import hibernate.model.Partida;
import hibernate.model.Poblat;
import hibernate.model.RecursJugador;
import hibernate.model.Vertex;

public class JugadorDao extends GenericDAO<Jugador, Integer> implements IJugadorDao {

    @Override
    public void MostrarRecursos(int idJugador) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            Jugador jugador = session.get(Jugador.class, idJugador);
            if (jugador == null) {
                System.out.println("No s'ha trobat cap jugador amb ID: " + idJugador);
                tx.commit();
                return;
            }

            List<RecursJugador> recursos = session
                    .createNamedQuery("RecursJugador.findByJugador", RecursJugador.class)
                    .setParameter("idJugador", idJugador)
                    .getResultList();

            System.out.println("    --------------------");
            System.out.println("    Inventari de Recursos - " + jugador.getNom() + " (Color: " + jugador.getColor() + ")");
            System.out.println("    --------------------");

            if (recursos.isEmpty()) {
                System.out.println("El jugador no té cap recurs actualment.");
            } else {
                for (RecursJugador r : recursos) {
                    System.out.printf("- %-10s: %d\n", r.getTipusRecurs(), r.getQuantitat());
                }
            }
            System.out.println("--------------------");
            tx.commit();
        } catch (RuntimeException e) {
            handleException(session, e);
        }
    }

    @Override
    public boolean ConstruirCarretera(int idJugador, int idAresta) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();

        try {
            Jugador jugador = session.get(Jugador.class, idJugador);
            Aresta aresta = session.get(Aresta.class, idAresta);

            if (jugador == null || aresta == null) {
                tx.rollback();
                return false;
            }

            Partida partida = jugador.getPartida();
            if (partida.getEstat() == EstatPartida.FINALITZADA) {
                tx.rollback();
                return false;
            }

            // Comprova que sigui el torn del jugador
            if (partida.getJugadorActiu() == null || partida.getJugadorActiu().getId() != idJugador) {
                tx.rollback();
                return false;
            }

            // Comprova que l'aresta pertanyi a la mateixa partida
            if (aresta.getPartida() != null && aresta.getPartida().getId() != partida.getId()) {
                tx.rollback();
                return false;
            }

            // Comprova que l'aresta estigui lliure
            Long carreteresAlAresta = session.createQuery(
                    "SELECT COUNT(c) FROM Carretera c WHERE c.aresta.id = :idAresta", Long.class)
                    .setParameter("idAresta", idAresta)
                    .getSingleResult();
            if (carreteresAlAresta > 0) {
                tx.rollback();
                return false;
            }

            boolean preparacio = (partida.getEstat() == EstatPartida.PREPARACIO);
            int vertexAId = aresta.getVertexA().getId();
            int vertexBId = aresta.getVertexB().getId();

            Long totalCarreteresJugador = session.createQuery(
                    "SELECT COUNT(c) FROM Carretera c WHERE c.propietari.id = :idJugador", Long.class)
                    .setParameter("idJugador", idJugador)
                    .getSingleResult();

            Long totalPoblatsJugador = session.createQuery(
                    "SELECT COUNT(p) FROM Poblat p WHERE p.propietari.id = :idJugador", Long.class)
                    .setParameter("idJugador", idJugador)
                    .getSingleResult();

            if (preparacio) {
                // En PREPARACIO el jugador ha de tenir exactament un poblat més que carreteres (el poblat acabat de col·locar)
                if (totalPoblatsJugador != totalCarreteresJugador + 1) {
                    tx.rollback();
                    return false;
                }

                // Ha de tocar un poblat propi que encara no tingui cap carretera pròpia adjacent
                boolean tocaPoblatSenseCarretera = false;
                for (int vId : new int[] { vertexAId, vertexBId }) {
                    Long poblatPropi = session.createQuery(
                            "SELECT COUNT(p) FROM Poblat p WHERE p.vertex.id = :vId AND p.propietari.id = :idJugador",
                            Long.class)
                            .setParameter("vId", vId)
                            .setParameter("idJugador", idJugador)
                            .getSingleResult();

                    if (poblatPropi > 0) {
                        Long carreteresAdjacents = session.createQuery(
                                "SELECT COUNT(c) FROM Carretera c WHERE c.propietari.id = :idJugador AND (c.aresta.vertexA.id = :vId OR c.aresta.vertexB.id = :vId)",
                                Long.class)
                                .setParameter("idJugador", idJugador)
                                .setParameter("vId", vId)
                                .getSingleResult();

                        if (carreteresAdjacents == 0) {
                            tocaPoblatSenseCarretera = true;
                            break;
                        }
                    }
                }

                if (!tocaPoblatSenseCarretera) {
                    tx.rollback();
                    return false;
                }

            } else {
                // EN_CURS: cal haver tirat daus i no tenir lladre pendent
                if (!partida.isDausTirats() || partida.isLladrePendent()) {
                    tx.rollback();
                    return false;
                }

                // Límit de 15 carreteres
                if (totalCarreteresJugador >= 15) {
                    tx.rollback();
                    return false;
                }

                // Connectivitat: ha de tocar un poblat/ciutat del jugador o una altra carretera del jugador
                Long assentamentsPropis = session.createQuery(
                        "SELECT COUNT(a) FROM Assentament a WHERE a.propietari.id = :idJugador AND (a.vertex.id = :vA OR a.vertex.id = :vB)",
                        Long.class)
                        .setParameter("idJugador", idJugador)
                        .setParameter("vA", vertexAId)
                        .setParameter("vB", vertexBId)
                        .getSingleResult();

                Long carreteresConnectades = session.createQuery(
                        "SELECT COUNT(c) FROM Carretera c WHERE c.propietari.id = :idJugador AND c.aresta.id <> :idAresta "
                                + "AND (c.aresta.vertexA.id = :vA OR c.aresta.vertexB.id = :vA OR c.aresta.vertexA.id = :vB OR c.aresta.vertexB.id = :vB)",
                        Long.class)
                        .setParameter("idJugador", idJugador)
                        .setParameter("idAresta", idAresta)
                        .setParameter("vA", vertexAId)
                        .setParameter("vB", vertexBId)
                        .getSingleResult();

                if (assentamentsPropis == 0 && carreteresConnectades == 0) {
                    tx.rollback();
                    return false;
                }

                // Cost: 1 FUSTA + 1 MAÓ
                List<RecursJugador> recursos = session
                        .createNamedQuery("RecursJugador.findByJugador", RecursJugador.class)
                        .setParameter("idJugador", idJugador)
                        .getResultList();

                RecursJugador fusta = trobarRecurs(recursos, TipusRecurs.FUSTA);
                RecursJugador mao = trobarRecurs(recursos, TipusRecurs.MAO);

                if (fusta == null || fusta.getQuantitat() < 1 || mao == null || mao.getQuantitat() < 1) {
                    tx.rollback();
                    return false;
                }

                fusta.setQuantitat(fusta.getQuantitat() - 1);
                mao.setQuantitat(mao.getQuantitat() - 1);
            }

            Carretera carretera = new Carretera(jugador, aresta);
            session.persist(carretera);
            jugador.getConstruccions().add(carretera);

            tx.commit();
            return true;
        } catch (RuntimeException e) {
            handleException(session, e);
            return false;
        }
    }

    @Override
    public boolean ConstruirPoblat(int idJugador, int idVertex) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();

        try {
            Jugador jugador = session.get(Jugador.class, idJugador);
            Vertex vertex = session.get(Vertex.class, idVertex);

            if (jugador == null || vertex == null) {
                tx.rollback();
                return false;
            }

            Partida partida = jugador.getPartida();
            if (partida.getEstat() == EstatPartida.FINALITZADA) {
                tx.rollback();
                return false;
            }

            // Comprova que sigui el torn del jugador
            if (partida.getJugadorActiu() == null || partida.getJugadorActiu().getId() != idJugador) {
                tx.rollback();
                return false;
            }

            // Comprova que el vèrtex pertanyi a la mateixa partida
            if (vertex.getPartida() != null && vertex.getPartida().getId() != partida.getId()) {
                tx.rollback();
                return false;
            }

            // Comprova que el vèrtex estigui lliure
            Long assentamentAlVertex = session.createQuery(
                    "SELECT COUNT(a) FROM Assentament a WHERE a.vertex.id = :idVertex", Long.class)
                    .setParameter("idVertex", idVertex)
                    .getSingleResult();
            if (assentamentAlVertex > 0) {
                tx.rollback();
                return false;
            }

            // Regla de la distància: cap vèrtex adjacent pot tenir cap construcció
            Long assentamentsVeins = session.createQuery(
                    "SELECT COUNT(a) FROM Assentament a WHERE a.vertex.id IN ("
                            + "SELECT e.vertexB.id FROM Aresta e WHERE e.vertexA.id = :idVertex"
                            + ") OR a.vertex.id IN ("
                            + "SELECT e.vertexA.id FROM Aresta e WHERE e.vertexB.id = :idVertex)",
                    Long.class)
                    .setParameter("idVertex", idVertex)
                    .getSingleResult();
            if (assentamentsVeins > 0) {
                tx.rollback();
                return false;
            }

            boolean preparacio = (partida.getEstat() == EstatPartida.PREPARACIO);

            Long poblatsActuals = session.createQuery(
                    "SELECT COUNT(p) FROM Poblat p WHERE p.propietari.id = :idJugador", Long.class)
                    .setParameter("idJugador", idJugador)
                    .getSingleResult();

            Long carreteresActuals = session.createQuery(
                    "SELECT COUNT(c) FROM Carretera c WHERE c.propietari.id = :idJugador", Long.class)
                    .setParameter("idJugador", idJugador)
                    .getSingleResult();

            List<RecursJugador> recursos = session
                    .createNamedQuery("RecursJugador.findByJugador", RecursJugador.class)
                    .setParameter("idJugador", idJugador)
                    .getResultList();

            if (preparacio) {
                // En PREPARACIO el jugador no pot col·locar un altre poblat si encara no ha col·locat la carretera d'aquest torn
                if (poblatsActuals != carreteresActuals || poblatsActuals >= 2) {
                    tx.rollback();
                    return false;
                }
            } else {
                // EN_CURS: cal haver tirat daus i no tenir lladre pendent
                if (!partida.isDausTirats() || partida.isLladrePendent()) {
                    tx.rollback();
                    return false;
                }

                // Límit de 5 poblats
                if (poblatsActuals >= 5) {
                    tx.rollback();
                    return false;
                }

                // Ha d'estar connectat a una carretera pròpia
                Long carreteresPropies = session.createQuery(
                        "SELECT COUNT(c) FROM Carretera c WHERE c.propietari.id = :idJugador "
                                + "AND (c.aresta.vertexA.id = :idVertex OR c.aresta.vertexB.id = :idVertex)",
                        Long.class)
                        .setParameter("idJugador", idJugador)
                        .setParameter("idVertex", idVertex)
                        .getSingleResult();
                if (carreteresPropies == 0) {
                    tx.rollback();
                    return false;
                }

                // Cost: 1 FUSTA + 1 MAÓ + 1 LLANA + 1 BLAT
                TipusRecurs[] cost = { TipusRecurs.FUSTA, TipusRecurs.MAO, TipusRecurs.LLANA, TipusRecurs.BLAT };
                for (TipusRecurs tipus : cost) {
                    RecursJugador r = trobarRecurs(recursos, tipus);
                    if (r == null || r.getQuantitat() < 1) {
                        tx.rollback();
                        return false;
                    }
                }
                for (TipusRecurs tipus : cost) {
                    RecursJugador r = trobarRecurs(recursos, tipus);
                    r.setQuantitat(r.getQuantitat() - 1);
                }
            }

            Poblat poblat = new Poblat(jugador, vertex);
            session.persist(poblat);
            jugador.getConstruccions().add(poblat);

            // Si és el segon poblat de la preparació, rep 1 recurs per cada hexàgon (no desert) que el toca
            if (preparacio && poblatsActuals == 1) {
                List<HexagonVertex> hvList = session.createQuery(
                        "SELECT hv FROM HexagonVertex hv JOIN FETCH hv.hexagon WHERE hv.vertex.id = :idVertex",
                        HexagonVertex.class)
                        .setParameter("idVertex", idVertex)
                        .getResultList();

                for (HexagonVertex hv : hvList) {
                    Hexagon hex = hv.getHexagon();
                    TipusRecurs tipus = recursDelTerreny(hex.getTipusTerreny());
                    if (tipus != null) {
                        afegirRecurs(session, jugador, recursos, tipus);
                    }
                }
            }

            // Comprova si aquesta acció atorga la victòria
            comprovarVictoriaInternal(session, partida);

            tx.commit();
            return true;
        } catch (RuntimeException e) {
            handleException(session, e);
            return false;
        }
    }

    @Override
    public boolean MillorarACiutat(int idJugador, int idVertex) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            Jugador jugador = session.get(Jugador.class, idJugador);
            Vertex vertex = session.get(Vertex.class, idVertex);

            if (jugador == null || vertex == null) {
                tx.rollback();
                return false;
            }

            Partida partida = jugador.getPartida();
            if (partida.getEstat() != EstatPartida.EN_CURS) {
                tx.rollback();
                return false;
            }

            if (partida.getJugadorActiu() == null || partida.getJugadorActiu().getId() != idJugador) {
                tx.rollback();
                return false;
            }

            if (!partida.isDausTirats() || partida.isLladrePendent()) {
                tx.rollback();
                return false;
            }

            if (vertex.getPartida() != null && vertex.getPartida().getId() != partida.getId()) {
                tx.rollback();
                return false;
            }

            // Límit de 4 ciutats
            Long ciutats = session.createQuery(
                    "SELECT COUNT(c) FROM Ciutat c WHERE c.propietari.id = :idJugador", Long.class)
                    .setParameter("idJugador", idJugador)
                    .getSingleResult();
            if (ciutats >= 4) {
                tx.rollback();
                return false;
            }

            // Comprova que al vèrtex hi hagi un poblat propi
            Poblat poblat = session.createQuery(
                    "SELECT p FROM Poblat p WHERE p.vertex.id = :idVertex AND p.propietari.id = :idJugador",
                    Poblat.class)
                    .setParameter("idVertex", idVertex)
                    .setParameter("idJugador", idJugador)
                    .uniqueResultOptional()
                    .orElse(null);

            if (poblat == null) {
                tx.rollback();
                return false;
            }

            // Cost: 2 BLAT + 3 MINERAL
            List<RecursJugador> recursos = session
                    .createNamedQuery("RecursJugador.findByJugador", RecursJugador.class)
                    .setParameter("idJugador", idJugador)
                    .getResultList();

            RecursJugador blat = trobarRecurs(recursos, TipusRecurs.BLAT);
            RecursJugador mineral = trobarRecurs(recursos, TipusRecurs.MINERAL);

            if (blat == null || blat.getQuantitat() < 2 || mineral == null || mineral.getQuantitat() < 3) {
                tx.rollback();
                return false;
            }

            blat.setQuantitat(blat.getQuantitat() - 2);
            mineral.setQuantitat(mineral.getQuantitat() - 3);

            // Elimina el poblat i crea la ciutat
            jugador.getConstruccions().remove(poblat);
            session.remove(poblat);
            session.flush();

            Ciutat ciutat = new Ciutat(jugador, vertex);
            session.persist(ciutat);
            jugador.getConstruccions().add(ciutat);

            comprovarVictoriaInternal(session, partida);

            tx.commit();
            return true;
        } catch (RuntimeException e) {
            handleException(session, e);
            return false;
        }
    }

    @Override
    public TipusRecurs ComerciarBanca(int idJugador, TipusRecurs dona, TipusRecurs rep) {
        if (dona == null || rep == null || dona == rep) {
            return null;
        }

        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            Jugador jugador = session.get(Jugador.class, idJugador);
            if (jugador == null) {
                tx.rollback();
                return null;
            }

            Partida partida = jugador.getPartida();
            if (partida.getEstat() != EstatPartida.EN_CURS) {
                tx.rollback();
                return null;
            }

            if (partida.getJugadorActiu() == null || partida.getJugadorActiu().getId() != idJugador) {
                tx.rollback();
                return null;
            }

            if (!partida.isDausTirats() || partida.isLladrePendent()) {
                tx.rollback();
                return null;
            }

            List<RecursJugador> recursos = session
                    .createNamedQuery("RecursJugador.findByJugador", RecursJugador.class)
                    .setParameter("idJugador", idJugador)
                    .getResultList();

            RecursJugador recursDona = trobarRecurs(recursos, dona);
            RecursJugador recursRep = trobarRecurs(recursos, rep);

            if (recursDona == null || recursDona.getQuantitat() < 4 || recursRep == null) {
                tx.rollback();
                return null;
            }

            recursDona.setQuantitat(recursDona.getQuantitat() - 4);
            recursRep.setQuantitat(recursRep.getQuantitat() + 1);

            tx.commit();
            return rep;
        } catch (RuntimeException e) {
            handleException(session, e);
            return null;
        }
    }

    @Override
    public int CalcularPunts(int idJugador) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            Jugador jugador = session.get(Jugador.class, idJugador);
            if (jugador == null) {
                tx.rollback();
                return 0;
            }

            Integer idExercitMesGran = trobarIdExercitMesGran(session, jugador.getPartida());
            int punts = calcularPuntsInternal(session, idJugador, idExercitMesGran);

            tx.commit();
            return punts;
        } catch (RuntimeException e) {
            handleException(session, e);
            return 0;
        }
    }

    @Override
    public void PassarTorn(int idJugador) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            Jugador jugador = session.get(Jugador.class, idJugador);
            if (jugador == null) {
                throw new IllegalStateException("El jugador no existeix.");
            }

            Partida partida = jugador.getPartida();
            if (partida.getEstat() == EstatPartida.FINALITZADA) {
                throw new IllegalStateException("La partida està finalitzada.");
            }

            if (partida.getJugadorActiu() == null || partida.getJugadorActiu().getId() != idJugador) {
                throw new IllegalStateException("No és el teu torn.");
            }

            if (partida.isLladrePendent()) {
                throw new IllegalStateException("No es pot passar torn amb el lladre pendent.");
            }

            boolean preparacio = (partida.getEstat() == EstatPartida.PREPARACIO);

            if (preparacio) {
                // En PREPARACIO comprova que el jugador hagi col·locat el poblat i la carretera d'aquest torn
                int ronda = partida.getRonda();
                Long poblats = session.createQuery(
                        "SELECT COUNT(p) FROM Poblat p WHERE p.propietari.id = :idJugador", Long.class)
                        .setParameter("idJugador", idJugador)
                        .getSingleResult();

                Long carreteres = session.createQuery(
                        "SELECT COUNT(c) FROM Carretera c WHERE c.propietari.id = :idJugador", Long.class)
                        .setParameter("idJugador", idJugador)
                        .getSingleResult();

                if (poblats < ronda || carreteres < ronda) {
                    throw new IllegalStateException("En fase de preparació cal haver col·locat el poblat i la carretera de la ronda actual.");
                }
            } else {
                // EN_CURS: cal haver tirat els daus
                if (!partida.isDausTirats()) {
                    throw new IllegalStateException("Cal tirar els daus abans de passar torn.");
                }
            }

            // Ordena jugadors per ordre de torn
            List<Jugador> jugadors = session.createQuery(
                    "SELECT j FROM Jugador j WHERE j.partida.id = :idPartida ORDER BY j.ordreTorn ASC", Jugador.class)
                    .setParameter("idPartida", partida.getId())
                    .getResultList();

            int n = jugadors.size();
            int currentOrdre = jugador.getOrdreTorn();

            if (preparacio) {
                // Ordre de serp (ronda 1: 1..n; ronda 2: n..1)
                if (partida.getRonda() == 1) {
                    if (currentOrdre < n) {
                        partida.setJugadorActiu(buscarJugadorPerOrdre(jugadors, currentOrdre + 1));
                    } else {
                        // En canviar a la ronda 2, el jugador n repeteix torn
                        partida.setRonda(2);
                        partida.setJugadorActiu(buscarJugadorPerOrdre(jugadors, n));
                    }
                } else if (partida.getRonda() == 2) {
                    if (currentOrdre > 1) {
                        partida.setJugadorActiu(buscarJugadorPerOrdre(jugadors, currentOrdre - 1));
                    } else {
                        // Tots els jugadors han completat 2 poblats i 2 carreteres -> passa a EN_CURS, ronda 1, actiu ordre 1
                        partida.setEstat(EstatPartida.EN_CURS);
                        partida.setRonda(1);
                        partida.setJugadorActiu(buscarJugadorPerOrdre(jugadors, 1));
                    }
                }
            } else {
                // EN_CURS: rotatori segons ordre de torn (1..n -> 1)
                if (currentOrdre < n) {
                    partida.setJugadorActiu(buscarJugadorPerOrdre(jugadors, currentOrdre + 1));
                } else {
                    partida.setJugadorActiu(buscarJugadorPerOrdre(jugadors, 1));
                    partida.setRonda(partida.getRonda() + 1);
                }
            }

            partida.setDausTirats(false);
            tx.commit();
        } catch (RuntimeException e) {
            handleException(session, e);
            throw e;
        }
    }

    private Jugador buscarJugadorPerOrdre(List<Jugador> jugadors, int ordre) {
        for (Jugador j : jugadors) {
            if (j.getOrdreTorn() == ordre) {
                return j;
            }
        }
        return jugadors.get(0);
    }

    private int calcularPuntsInternal(Session session, int idJugador, Integer idExercitMesGran) {
        Long poblats = session.createQuery(
                "SELECT COUNT(p) FROM Poblat p WHERE p.propietari.id = :idJugador", Long.class)
                .setParameter("idJugador", idJugador)
                .getSingleResult();

        Long ciutats = session.createQuery(
                "SELECT COUNT(c) FROM Ciutat c WHERE c.propietari.id = :idJugador", Long.class)
                .setParameter("idJugador", idJugador)
                .getSingleResult();

        Long puntsCartes = session.createQuery(
                "SELECT COUNT(c) FROM CartaDesenvolupament c WHERE c.jugador.id = :idJugador AND c.tipus = :tipusCarta",
                Long.class)
                .setParameter("idJugador", idJugador)
                .setParameter("tipusCarta", TipusCarta.PUNT_VICTORIA)
                .getSingleResult();

        int punts = poblats.intValue() + (ciutats.intValue() * 2) + puntsCartes.intValue();
        if (idExercitMesGran != null && idExercitMesGran == idJugador) {
            punts += 2;
        }
        return punts;
    }

    private void comprovarVictoriaInternal(Session session, Partida partida) {
        if (partida.getEstat() == EstatPartida.FINALITZADA) {
            return;
        }
        Integer idExercit = trobarIdExercitMesGran(session, partida);
        for (Jugador j : partida.getJugadors()) {
            int punts = calcularPuntsInternal(session, j.getId(), idExercit);
            if (punts >= partida.getPuntsPerGuanyar()) {
                partida.setGuanyador(j);
                partida.setEstat(EstatPartida.FINALITZADA);
                System.out.println("Partida guanyada per " + j.getNom() + " amb " + punts + " punts!");
                break;
            }
        }
    }

    private Integer trobarIdExercitMesGran(Session session, Partida partida) {
        List<Jugador> jugadors = partida.getJugadors();
        if (jugadors == null || jugadors.isEmpty()) return null;

        Jugador maxJugador = null;
        int maxCavallers = 2; // cal almenys 3 cavallers
        boolean empat = false;

        for (Jugador j : jugadors) {
            int c = j.getCavallersJugats();
            if (c > maxCavallers) {
                maxCavallers = c;
                maxJugador = j;
                empat = false;
            } else if (c == maxCavallers && maxCavallers >= 3) {
                empat = true;
            }
        }

        if (maxJugador != null && !empat) {
            return maxJugador.getId();
        }
        return null;
    }

    private RecursJugador trobarRecurs(List<RecursJugador> recursos, TipusRecurs tipus) {
        return recursos.stream()
                .filter(recurs -> recurs.getTipusRecurs() == tipus)
                .findFirst()
                .orElse(null);
    }

    private TipusRecurs recursDelTerreny(TipusTerreny terreny) {
        if (terreny == null) return null;
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
            default:
                return null;
        }
    }

    private void afegirRecurs(Session session, Jugador jugador, List<RecursJugador> recursos, TipusRecurs tipus) {
        RecursJugador recurs = trobarRecurs(recursos, tipus);
        if (recurs == null) {
            recurs = new RecursJugador(0, jugador, tipus, 1);
            session.persist(recurs);
            recursos.add(recurs);
        } else {
            recurs.setQuantitat(recurs.getQuantitat() + 1);
        }
    }
}
