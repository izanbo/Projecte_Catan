package hibernate.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import hibernate.enums.ColorJugador;
import hibernate.enums.EstatPartida;
import hibernate.enums.TipusCarta;
import hibernate.enums.TipusRecurs;
import hibernate.enums.TipusTerreny;
import hibernate.model.Assentament;
import hibernate.model.CartaDesenvolupament;
import hibernate.model.Ciutat;
import hibernate.model.Construccio;
import hibernate.model.Hexagon;
import hibernate.model.HexagonVertex;
import hibernate.model.Jugador;
import hibernate.model.Partida;
import hibernate.model.Poblat;
import hibernate.model.RecursJugador;
import hibernate.model.Vertex;
import hibernate.util.HibernateUtil;

public class PartidaDAO extends GenericDAO<Partida, Integer> implements IPartidaDao {

    @Override
    public int IniciarPartida(List<String> nomsJugadors) {
        if (nomsJugadors == null || nomsJugadors.size() < 2 || nomsJugadors.size() > 4) {
            throw new IllegalArgumentException("Hi ha d'haver entre 2 i 4 jugadors.");
        }

        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();

        try {
            Partida partida = new Partida();
            partida.IniciarPartida();

            session.persist(partida);

            List<String> nomsBarrejats = new ArrayList<>(nomsJugadors);
            Collections.shuffle(nomsBarrejats);

            ColorJugador[] colors = ColorJugador.values();
            for (int i = 0; i < nomsBarrejats.size(); i++) {
                Jugador jugador = new Jugador(nomsBarrejats.get(i), colors[i], i + 1, partida);
                for (TipusRecurs tipus : TipusRecurs.values()) {
                    RecursJugador recurs = new RecursJugador(0, jugador, tipus, 0);
                    jugador.getRecursos().add(recurs);
                }
                partida.addJugador(jugador);
            }

            // El jugador actiu inicial és el d'ordre de torn 1
            Jugador primerJugador = partida.getJugadors().stream()
                    .filter(j -> j.getOrdreTorn() == 1)
                    .findFirst()
                    .orElse(partida.getJugadors().get(0));
            partida.setJugadorActiu(primerJugador);

            // Carrega la topologia del tauler des dels CSV i assigna terrenys, números i el desert
            HibernateUtil.carregarDades(session, partida);

            // Crea la pila de cartes de desenvolupament (14 CAVALLER, 5 PUNT_VICTORIA, 2 MONOPOLI)
            crearPilaCartes(session, partida);

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

    private void crearPilaCartes(Session session, Partida partida) {
        List<TipusCarta> tipusCartes = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            tipusCartes.add(TipusCarta.CAVALLER);
        }
        for (int i = 0; i < 5; i++) {
            tipusCartes.add(TipusCarta.PUNT_VICTORIA);
        }
        for (int i = 0; i < 2; i++) {
            tipusCartes.add(TipusCarta.MONOPOLI);
        }
        Collections.shuffle(tipusCartes);

        for (TipusCarta tipus : tipusCartes) {
            CartaDesenvolupament carta = new CartaDesenvolupament(tipus, partida);
            partida.getCartes().add(carta);
            session.persist(carta);
        }
    }

    @Override
    public void MostrarTauler(int idPartida) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            MostrarTauler(session, idPartida);
            tx.commit();
        } catch (RuntimeException e) {
            handleException(session, e);
        }
    }

    private void MostrarTauler(Session session, int idPartida) {
        Partida partida = session.get(Partida.class, idPartida);
        if (partida == null) {
            System.out.println("No s'ha trobat cap partida amb ID: " + idPartida);
            return;
        }

        String sql = "SELECT DISTINCT h FROM Hexagon h " +
                "LEFT JOIN FETCH h.vertexs hv " +
                "LEFT JOIN FETCH hv.vertex v " +
                "WHERE h.partida.id = :idPartida " +
                "ORDER BY h.codi ASC";

        Query<Hexagon> query = session.createQuery(sql, Hexagon.class);
        query.setParameter("idPartida", idPartida);

        List<Hexagon> hexagons = query.getResultList();

        if (hexagons.isEmpty()) {
            System.out.println("No s'han trobat hexàgons per a la partida amb ID: " + idPartida);
            return;
        }

        System.out.println("    --------------------");
        System.out.println("    Tauler de la Partida (" + idPartida + ")");
        System.out.println("    --------------------");

        Hexagon hexLladre = partida.getHexagonLladre();

        for (Hexagon hex : hexagons) {
            boolean hiHaLladre = (hexLladre != null && hexLladre.getId() == hex.getId());
            System.out.printf("Hexàgon [Codi: %d] - Terreny: %10s | Número: %-2s | Lladre: %s\n", hex.getCodi(),
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

                    Assentament assentament = session
                            .createNamedQuery("Assentament.findByVertex", Assentament.class)
                            .setParameter("idVertex", vertexId)
                            .uniqueResultOptional()
                            .orElse(null);

                    if (assentament != null) {
                        String tipusConstruccio = assentament.getClass().getSimpleName();
                        String propietariNom = assentament.getPropietari().getNom();
                        System.out.printf("  └─ Vèrtex pos %d (Codi %d): %s de %s\n", posicio, vertexCodi,
                                tipusConstruccio, propietariNom);
                    } else {
                        System.out.printf("  └─ Vèrtex pos %d (Codi %d): [BUIT]\n", posicio, vertexCodi);
                    }
                }
            }
            System.out.println("--------------------");
        }
    }

    @Override
    public void MostrarPartida(int idPartida) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            MostrarPartida(session, idPartida);
            tx.commit();
        } catch (RuntimeException e) {
            handleException(session, e);
        }
    }

    private void MostrarPartida(Session session, int idPartida) {
        String sqlPartida = "SELECT DISTINCT p FROM Partida p " +
                "LEFT JOIN FETCH p.jugadors j " +
                "LEFT JOIN FETCH p.jugadorActiu " +
                "WHERE p.id = :idPartida";

        Partida partida = session.createQuery(sqlPartida, Partida.class)
                .setParameter("idPartida", idPartida)
                .uniqueResultOptional()
                .orElse(null);

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
        System.out.println("Última tirada: " + partida.getUltimaTirada());
        System.out.println("------------------");

        List<Jugador> jugadors = partida.getJugadors();
        if (jugadors == null || jugadors.isEmpty()) {
            System.out.println("Aquesta partida no té jugadors registrats.");
        } else {
            // Comprova qui té l'exèrcit més gran si escau
            Integer idExercitMesGran = trobarIdExercitMesGran(session, partida);

            for (Jugador j : jugadors) {
                Long poblats = session.createQuery(
                        "SELECT COUNT(p) FROM Poblat p WHERE p.propietari.id = :idJugador", Long.class)
                        .setParameter("idJugador", j.getId()).getSingleResult();

                Long ciutats = session.createQuery(
                        "SELECT COUNT(c) FROM Ciutat c WHERE c.propietari.id = :idJugador", Long.class)
                        .setParameter("idJugador", j.getId()).getSingleResult();

                Long carreteres = session.createQuery(
                        "SELECT COUNT(c) FROM Carretera c WHERE c.propietari.id = :idJugador", Long.class)
                        .setParameter("idJugador", j.getId()).getSingleResult();

                Long puntsCartes = session.createQuery(
                        "SELECT COUNT(c) FROM CartaDesenvolupament c WHERE c.jugador.id = :idJugador AND c.tipus = :tipusCarta",
                        Long.class)
                        .setParameter("idJugador", j.getId())
                        .setParameter("tipusCarta", TipusCarta.PUNT_VICTORIA)
                        .getSingleResult();

                int puntsVictoria = poblats.intValue() + (ciutats.intValue() * 2) + puntsCartes.intValue();
                if (idExercitMesGran != null && idExercitMesGran == j.getId()) {
                    puntsVictoria += 2;
                }

                Long totalRecursos = session.createQuery(
                        "SELECT COALESCE(SUM(r.quantitat), 0) FROM RecursJugador r WHERE r.jugador.id = :idJugador",
                        Long.class)
                        .setParameter("idJugador", j.getId()).getSingleResult();

                System.out.printf("Jugador: %s (Color: %s)\n", j.getNom(), j.getColor());
                System.out.printf(" └─ Punts de Victòria: %d\n", puntsVictoria);
                System.out.printf(" └─ Construccions: %d Poblat(s), %d Ciutat(s), %d Carretera(s)\n", poblats,
                        ciutats, carreteres);
                System.out.printf(" └─ Total de recursos: %d\n", totalRecursos);
                System.out.println("--------------------------------------------------");
            }
        }
    }

    @Override
    public void TirarDaus(int idPartida) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            Partida partida = session.get(Partida.class, idPartida);
            if (partida == null) {
                throw new IllegalStateException("La partida no existeix.");
            }
            if (partida.getEstat() != EstatPartida.EN_CURS) {
                throw new IllegalStateException("La partida no està en curs.");
            }
            if (partida.isDausTirats()) {
                throw new IllegalStateException("Ja s'han tirat els daus en aquest torn.");
            }
            if (partida.isLladrePendent()) {
                throw new IllegalStateException("El lladre està pendent de moure.");
            }

            int dau1 = (int) (Math.random() * 6) + 1;
            int dau2 = (int) (Math.random() * 6) + 1;
            int tirada = dau1 + dau2;
            partida.setUltimaTirada(tirada);
            partida.setDausTirats(true);

            System.out.println("Tirada de daus: " + dau1 + " + " + dau2 + " = " + tirada);

            if (tirada == 7) {
                partida.setLladrePendent(true);
                System.out.println("Ha sortit un 7! No hi ha producció. El lladre queda pendent.");

                // Descart de recursos per jugadors amb més de 7 recursos
                List<Jugador> jugadors = partida.getJugadors();
                for (Jugador jugador : jugadors) {
                    List<RecursJugador> recursos = session
                            .createNamedQuery("RecursJugador.findByJugador", RecursJugador.class)
                            .setParameter("idJugador", jugador.getId())
                            .getResultList();

                    int total = 0;
                    for (RecursJugador r : recursos) {
                        total += r.getQuantitat();
                    }

                    if (total > 7) {
                        int aDescartar = total / 2;
                        List<TipusRecurs> bossa = new ArrayList<>();
                        for (RecursJugador r : recursos) {
                            for (int i = 0; i < r.getQuantitat(); i++) {
                                bossa.add(r.getTipusRecurs());
                            }
                        }
                        Collections.shuffle(bossa);

                        Map<TipusRecurs, Integer> descartats = new HashMap<>();
                        for (int i = 0; i < aDescartar; i++) {
                            TipusRecurs t = bossa.get(i);
                            descartats.put(t, descartats.getOrDefault(t, 0) + 1);
                        }

                        for (RecursJugador r : recursos) {
                            int desc = descartats.getOrDefault(r.getTipusRecurs(), 0);
                            if (desc > 0) {
                                r.setQuantitat(r.getQuantitat() - desc);
                            }
                        }
                        System.out.println(jugador.getNom() + " tenia " + total + " recursos i n'ha descartat "
                                + aDescartar + ": " + descartats);
                    }
                }
                tx.commit();
                return;
            }

            // Tirada diferent de 7: cerca hexàgons amb aquest número i sense el lladre
            int idLladre = (partida.getHexagonLladre() != null) ? partida.getHexagonLladre().getId() : -1;

            List<Hexagon> hexagons = session
                    .createNamedQuery("Hexagon.findByPartidaAndNumeroSenseLladre", Hexagon.class)
                    .setParameter("idPartida", idPartida)
                    .setParameter("numero", tirada)
                    .setParameter("idLladre", idLladre)
                    .getResultList();

            Map<String, Map<TipusRecurs, Integer>> resumRecursos = new HashMap<>();

            for (Hexagon hexagon : hexagons) {
                TipusTerreny terreny = hexagon.getTipusTerreny();
                TipusRecurs tipus = obtenirRecurs(terreny);
                if (tipus == null) {
                    continue;
                }

                for (HexagonVertex hv : hexagon.getVertexs()) {
                    Vertex vertex = hv.getVertex();
                    Assentament assentament = session
                            .createNamedQuery("Assentament.findByVertex", Assentament.class)
                            .setParameter("idVertex", vertex.getId())
                            .uniqueResultOptional()
                            .orElse(null);

                    if (assentament == null) {
                        continue;
                    }

                    int quantitat = (assentament instanceof Ciutat) ? 2 : 1;
                    Jugador propietari = assentament.getPropietari();

                    RecursJugador recursJugador = session
                            .createNamedQuery("RecursJugador.findByJugadorAndTipus", RecursJugador.class)
                            .setParameter("idJugador", propietari.getId())
                            .setParameter("tipus", tipus)
                            .uniqueResult();

                    if (recursJugador != null) {
                        recursJugador.setQuantitat(recursJugador.getQuantitat() + quantitat);

                        resumRecursos.putIfAbsent(propietari.getNom(), new HashMap<>());
                        Map<TipusRecurs, Integer> mapaJugador = resumRecursos.get(propietari.getNom());
                        mapaJugador.put(tipus, mapaJugador.getOrDefault(tipus, 0) + quantitat);
                    }
                }
            }

            System.out.println("Resum de producció per la tirada " + tirada + ":");
            if (resumRecursos.isEmpty()) {
                System.out.println("  Cap jugador ha rebut recursos.");
            } else {
                resumRecursos.forEach((nom, mapa) -> {
                    System.out.println("  " + nom + " ha rebut: " + mapa);
                });
            }

            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    private TipusRecurs obtenirRecurs(TipusTerreny terreny) {
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

    @Override
    public void ComprovarVictoria(int idPartida) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            Partida partida = session.get(Partida.class, idPartida);
            if (partida == null) {
                throw new IllegalStateException("La partida no existeix.");
            }

            if (partida.getEstat() == EstatPartida.FINALITZADA) {
                tx.commit();
                return;
            }

            Integer idExercitMesGran = trobarIdExercitMesGran(session, partida);

            for (Jugador jugador : partida.getJugadors()) {
                Long poblats = session.createQuery(
                        "SELECT COUNT(p) FROM Poblat p WHERE p.propietari.id = :idJugador", Long.class)
                        .setParameter("idJugador", jugador.getId())
                        .getSingleResult();

                Long ciutats = session.createQuery(
                        "SELECT COUNT(c) FROM Ciutat c WHERE c.propietari.id = :idJugador", Long.class)
                        .setParameter("idJugador", jugador.getId())
                        .getSingleResult();

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

                int puntsVictoria = poblats.intValue() + (2 * ciutats.intValue()) + cartesVictoria.intValue();
                if (idExercitMesGran != null && idExercitMesGran == jugador.getId()) {
                    puntsVictoria += 2;
                }

                if (puntsVictoria >= partida.getPuntsPerGuanyar()) {
                    partida.setGuanyador(jugador);
                    partida.setEstat(EstatPartida.FINALITZADA);
                    System.out.println("La partida " + idPartida + " ha estat guanyada per " + jugador.getNom()
                            + " amb " + puntsVictoria + " punts de victòria.");
                    break;
                }
            }

            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
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
}
