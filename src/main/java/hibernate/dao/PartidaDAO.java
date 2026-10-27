package hibernate.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;

import org.hibernate.Transaction;

import hibernate.enums.ColorJugador;
import hibernate.enums.EstatPartida;
import hibernate.model.Jugador;
import hibernate.enums.TipusCarta;
import hibernate.enums.TipusRecurs;
import hibernate.enums.TipusTerreny;
import hibernate.model.Assentament;
import hibernate.model.CartaDesenvolupament;
import hibernate.model.Ciutat;
import hibernate.model.Construccio;
import hibernate.model.Hexagon;
import hibernate.model.HexagonVertex;
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
        try {
            session.beginTransaction();
            MostrarTauler(session, idPartida);
            session.getTransaction();
        } catch (RuntimeException e) {
            handleException(session, e);
        }
    }

    private void MostrarTauler(Session session, int idPartida) {
        String sql = "SELECT DISTINCT h FROM Hexagon h " +
                "LEFT JOIN FETCH h.vertexs hv " +
                "LEFT JOIN FETCH hv.vertex v " +
                "WHERE h.partida.id = :idPartida " +
                "ORDER BY h.codi ASC";

        Query<Hexagon> query = session.createQuery(sql, Hexagon.class);
        query.setParameter("idPartida", idPartida);

        List<Hexagon> hexagons = query.getResultList();

        if (hexagons.isEmpty()) {
            System.out.println("No s'ha trobat hexàgons per a la partida amb ID: " + idPartida);
            return;
        }

        System.out.println("    --------------------");
        System.out.println("    Tauler de la Partida (" + idPartida + ")");
        System.out.println("    --------------------");

        for (Hexagon hex : hexagons) {
            boolean hiHaLladre = hex.getPartida().isLladre();
            System.out.printf("HExàgon [Codi: %d] - Terreny: %10s | Número: %-2s | Lladre: %s\n", hex.getCodi(),
                    hex.getTipusTerreny(), (hex.getNumero() != null ? hex.getNumero() : "N/A"),
                    (hiHaLladre ? "SÍ" : "NO"));

            List<HexagonVertex> hexVertexList = hex.getVertexs();
            if (hexVertexList == null || hexVertexList.isEmpty()) {
                System.out.println("(Sense vèrtexs associats)");
            } else {
                for (HexagonVertex hv : hexVertexList) {
                    int posicio = hv.getPosicio();
                    int vertexCodi = hv.getVertex().getCodi();
                    int vertexId = hv.getVertex().getId();

                    String sqlAssentament = "SELECT a FROM Assentament a Where a.vertex.id = :vertexId";
                    Assentament assentament = session.createQuery(sqlAssentament, Assentament.class)
                            .setParameter("vertexId", vertexId).uniqueResultOptional().orElse(null);

                    if (assentament != null) {
                        String tipusConstruccio = assentament.getClass().getSimpleName();
                        String propietariNom = assentament.getPropietari().getNom();

                        System.out.printf("Vèrtex pos %d (Codi %d): %s de %s\n", posicio, vertexCodi, tipusConstruccio,
                                propietariNom);
                    } else {
                        System.out.printf("Vèrtex pos %d (Codi %d): [BUIT]\n", posicio, vertexCodi);
                    }
                }
            }
            System.out.println("--------------------");
        }
    }

    @Override
    public void MostrarPartida(int idPartida) {
        Session session = sessionFactory.getCurrentSession();
        try {
            session.beginTransaction();
            MostrarPartida(session, idPartida);
            session.getTransaction().commit();
        } catch (RuntimeException e) {
            handleException(session, e);
        }
    }

    private void MostrarPartida(Session session, int idPartida) {
        String sqlPartida = "SELECT DISTINCT p FROM Partida p " +
                "LEFT JOIN FETCH p.jugadors j " +
                "LEFT JOIN FETCH p.jugadorActiu " +
                "WHERE p.id = :idPartida";

        Partida partida = session.createQuery(sqlPartida, Partida.class).setParameter("idPartida", idPartida)
                .uniqueResultOptional().orElse(null);

        if (partida == null) {
            System.out.println("No s'ha trobat cap partida amb ID: " + idPartida);
            return;
        }

        System.out.println("    --------------------");
        System.out.println("    Estat de la Partida (" + partida.getId() + ")");
        System.out.println("    --------------------");

        System.out.println("Estat: " + partida.getEstat());
        System.out.println("Ronda: " + partida.getRonda());
        System.out.println(
                "Jugador actiu: " + (partida.getJugadorActiu() != null ? partida.getJugadorActiu().getNom() : "cap"));
        System.out.println("Ultima tirada: " + partida.getUltimaTirada());
        System.out.println("------------------");

        List<Jugador> jugadors = partida.getJugadors();
        if (jugadors == null || jugadors.isEmpty()) {
            System.out.println("Aquesta partida no té jugadors registrats.");
        } else {
            for (Jugador j : jugadors) {
                String sqlConstruccions = "SELECT c FROM Construccio c WHERE c.propietari.id = :idJugador";
                List<Construccio> construccios = session.createQuery(sqlConstruccions, Construccio.class)
                        .setParameter("idJugador", j.getId()).getResultList();

                int numPoblats = 0;
                int numCiutats = 0;
                int numCarreteres = 0;

                for (Construccio c : construccios) {
                    if (c instanceof Poblat) {
                        numPoblats++;
                    } else if (c instanceof Ciutat) {
                        numCiutats++;
                    } else {
                        numCarreteres++;
                    }
                }

                String sqlCartes = "SELECT c FROM CartaDesenvolupament c WHERE c.jugador.id = :idJugador AND c.tipus = :tipusCarta";
                List<CartaDesenvolupament> cartesPunt = session.createQuery(sqlCartes, CartaDesenvolupament.class)
                        .setParameter("idJugador", j.getId()).setParameter("tipusCarta", TipusCarta.PUNT_VICTORIA)
                        .getResultList();

                int puntsCartes = cartesPunt.size();
                int puntsVictoria = (numPoblats * 1) + (numCiutats * 2) + puntsCartes;

                String sqlRecursos = "SELECT SUM(r.quantitat) FROM RecursJugador r WHERE r.jugador.id = :idJugador";
                Long totalRecursosInt = session.createQuery(sqlRecursos, Long.class)
                        .setParameter("idJugador", j.getId()).uniqueResult();
                Long totalRecursos = (totalRecursosInt != null) ? totalRecursosInt : 0;

                System.out.printf("Jugador: %s (Color: %s)\n", j.getNom(), j.getColor());
                System.out.printf(" └─ Punts de Victòria: %d\n", puntsVictoria);
                System.out.printf(" └─ Construccions: %d Poblat(s), %d Ciutat(s), %d Carretera(s)\n", numPoblats,
                        numCiutats, numCarreteres);
                System.out.printf(" └─ Total de recursos: %d\n", totalRecursos);
                System.out.println("--------------------------------------------------");

            }
        }
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
        if (tirada == 7) {
            // HACER MAS ADELANTE
            return;
        }

        List<Hexagon> hexagons = session.createQuery(
                "FROM Hexagon h "
                        + "WHERE h.partida.id = :idPartida "
                        + "AND h.numero = :numero "
                        + "AND h.id <> :idLladre",
                Hexagon.class)
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

    @Override
    public void ComprovarVictoria(int idPartida) {
        Session session = sessionFactory.getCurrentSession();
        // Inicia la transacció per llegir la partida i desar el possible resultat.
        Transaction tx = session.beginTransaction();
        try {
            // Carrega la partida; si no existeix, no es pot comprovar la victòria.
            Partida partida = session.get(Partida.class, idPartida);
            if (partida == null) {
                throw new RuntimeException("La partida no existeix.");
            }

            // No cal tornar a calcular el resultat d'una partida ja finalitzada.
            if (partida.getEstat() == EstatPartida.FINALITZADA) {
                tx.commit();
                return;
            }

            // Comprova els punts de cada jugador fins a trobar-ne un que arribi al llindar.
            for (Jugador jugador : partida.getJugadors()) {
                // Cada poblat suma un punt.
                Long poblats = session.createQuery(
                        "SELECT COUNT(p) FROM Poblat p WHERE p.propietari.id = :idJugador", Long.class)
                        .setParameter("idJugador", jugador.getId())
                        .getSingleResult();
                // Cada ciutat suma dos punts.
                Long ciutats = session.createQuery(
                        "SELECT COUNT(c) FROM Ciutat c WHERE c.propietari.id = :idJugador", Long.class)
                        .setParameter("idJugador", jugador.getId())
                        .getSingleResult();
                // Cada carta de punt de victòria del jugador suma un punt.
                Long cartesVictoria = session.createQuery(
                        "SELECT COUNT(c) FROM CartaDesenvolupament c " +
                                "WHERE c.jugador.id = :idJugador " +
                                "AND c.partida.id = :idPartida " +
                                "AND c.tipus = :tipusCarta",
                        Long.class)
                        .setParameter("idJugador", jugador.getId())
                        .setParameter("idPartida", idPartida)
                        .setParameter("tipusCarta", TipusCarta.PUNT_VICTORIA)
                        .getSingleResult();

                // Suma els punts i els compara amb els necessaris per guanyar la partida.
                int puntsVictoria = poblats.intValue() + (2 * ciutats.intValue()) + cartesVictoria.intValue();
                if (puntsVictoria >= partida.getPuntsPerGuanyar()) {
                    // Desa el guanyador i marca la partida com a finalitzada.
                    partida.setGuanyador(jugador);
                    partida.setEstat(EstatPartida.FINALITZADA);
                    System.out.println("La partida " + idPartida + " ha estat guanyada per " + jugador.getNom()
                            + " amb " + puntsVictoria + " punts de victòria.");
                    break;
                }
            }

            // Confirma els canvis a la base de dades.
            tx.commit();
        } catch (RuntimeException e) {
            // Desfà els canvis si l'operació falla i propaga l'error.
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

}
