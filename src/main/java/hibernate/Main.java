package hibernate;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import hibernate.dao.JugadorDao;
import hibernate.dao.PartidaDAO;
import hibernate.enums.EstatPartida;
import hibernate.enums.TipusRecurs;
import hibernate.model.Aresta;
import hibernate.model.Jugador;
import hibernate.model.Partida;
import hibernate.model.RecursJugador;
import hibernate.model.Vertex;
import hibernate.util.HibernateUtil;

public class Main {

    public static void main(String[] args) {
        try {
            System.out.println("============================================================");
            System.out.println("       DEMOSTRACIÓ I PROVES DEL JOC CATAN (HIBERNATE)       ");
            System.out.println("============================================================\n");

            PartidaDAO partidaDao = new PartidaDAO();
            JugadorDao jugadorDao = new JugadorDao();

            // ------------------------------------------------------------
            // 1. INICIAR PARTIDA
            // ------------------------------------------------------------
            System.out.println(">>> 1. CREANT UNA NOVA PARTIDA AMB 3 JUGADORS...");
            List<String> nomsJugadors = List.of("Anna", "Bernat", "Clara");
            int idPartida = partidaDao.IniciarPartida(nomsJugadors);
            System.out.println("[OK] Partida creada amb ID: " + idPartida);

            // Mostrar el tauler complet generat des dels fitxers CSV
            System.out.println("\n>>> MOSTRANT EL TAULER INICIAL DE LA PARTIDA:");
            partidaDao.MostrarTauler(idPartida);

            // Mostrar la situació de la partida inicial
            System.out.println("\n>>> MOSTRANT L'ESTAT INICIAL DE LA PARTIDA:");
            partidaDao.MostrarPartida(idPartida);

            // Comprovar recursos inicials d'un jugador (tots a 0)
            Session session = HibernateUtil.getSessionFactory().getCurrentSession();
            session.beginTransaction();
            Partida partida = session.get(Partida.class, idPartida);
            Jugador primerActiu = partida.getJugadorActiu();
            int idPrimerActiu = primerActiu.getId();
            String nomPrimerActiu = primerActiu.getNom();
            session.getTransaction().commit();

            System.out.println("\n>>> INVENTARI INICIAL DEL JUGADOR ACTIU (" + nomPrimerActiu + "):");
            jugadorDao.MostrarRecursos(idPrimerActiu);

            // ------------------------------------------------------------
            // 2. FASE DE PREPARACIÓ (PREPARACIO) - ORDRE DE SERP
            // ------------------------------------------------------------
            System.out.println("\n============================================================");
            System.out.println(">>> 2. FASE DE PREPARACIÓ (ORDRE DE SERP: 1..N, N..1)");
            System.out.println("============================================================");

            // Carreguem vèrtexs i arestes de la partida per seleccionar posicions vàlides
            session = HibernateUtil.getSessionFactory().getCurrentSession();
            session.beginTransaction();
            List<Vertex> vertexs = session.createQuery(
                    "SELECT v FROM Vertex v WHERE v.partida.id = :idPartida ORDER BY v.codi ASC", Vertex.class)
                    .setParameter("idPartida", idPartida).getResultList();
            List<Aresta> arestes = session.createQuery(
                    "SELECT a FROM Aresta a JOIN FETCH a.vertexA JOIN FETCH a.vertexB WHERE a.partida.id = :idPartida ORDER BY a.codi ASC",
                    Aresta.class)
                    .setParameter("idPartida", idPartida).getResultList();
            session.getTransaction().commit();

            // Cerquem 6 vèrtexs que compleixin estrictament la regla de la distància (no adjacents)
            List<Vertex> vTriats = new ArrayList<>();
            for (Vertex v : vertexs) {
                boolean adjacent = false;
                for (Vertex triat : vTriats) {
                    boolean sonVeins = arestes.stream().anyMatch(a ->
                        (a.getVertexA().getId() == v.getId() && a.getVertexB().getId() == triat.getId()) ||
                        (a.getVertexB().getId() == v.getId() && a.getVertexA().getId() == triat.getId())
                    );
                    if (sonVeins) {
                        adjacent = true;
                        break;
                    }
                }
                if (!adjacent) {
                    vTriats.add(v);
                    if (vTriats.size() == 6) break;
                }
            }

            // Execució dels 6 torns de la fase de preparació:
            // Ronda 1: Jugadors 1, 2, 3
            // Ronda 2: Jugadors 3, 2, 1
            for (int torn = 0; torn < 6; torn++) {
                session = HibernateUtil.getSessionFactory().getCurrentSession();
                session.beginTransaction();
                partida = session.get(Partida.class, idPartida);
                Jugador actiu = partida.getJugadorActiu();
                int idActiu = actiu.getId();
                String nomActiu = actiu.getNom();
                int ordreTorn = actiu.getOrdreTorn();
                int rondaActual = partida.getRonda();
                session.getTransaction().commit();

                Vertex targetV = vTriats.get(torn);
                Aresta incidentA = arestes.stream()
                        .filter(a -> a.getVertexA().getId() == targetV.getId() || a.getVertexB().getId() == targetV.getId())
                        .findFirst().orElseThrow();

                System.out.printf("\n--- TORN PREPARACIÓ %d/6 [Ronda %d] | Jugador: %s (Ordre %d) ---\n",
                        (torn + 1), rondaActual, nomActiu, ordreTorn);

                // 1. Construir Poblat
                boolean poblatOk = jugadorDao.ConstruirPoblat(idActiu, targetV.getId());
                System.out.printf("  [+] Poblat construït al vèrtex codi %d: %s\n", targetV.getCodi(), poblatOk ? "ÈXIT" : "FALLADA");

                // 2. Construir Carretera adjacent
                boolean carreteraOk = jugadorDao.ConstruirCarretera(idActiu, incidentA.getId());
                System.out.printf("  [+] Carretera construïda a l'aresta codi %d: %s\n", incidentA.getCodi(), carreteraOk ? "ÈXIT" : "FALLADA");

                // Si és el segon poblat, mostrem si ha rebut recursos inicials dels hexàgons veïns
                if (rondaActual == 2) {
                    System.out.println("  [*] 2n poblat col·locat: comprovant recursos inicials rebuts...");
                    jugadorDao.MostrarRecursos(idActiu);
                }

                // 3. Passar Torn
                jugadorDao.PassarTorn(idActiu);
                System.out.println("  [>] Torn passat correctament.");
            }

            // ------------------------------------------------------------
            // 3. COMPROVACIÓ DE TRANSICIÓ A EN_CURS
            // ------------------------------------------------------------
            System.out.println("\n============================================================");
            System.out.println(">>> 3. VERIFICACIÓ DE TRANSICIÓ A FASE EN_CURS");
            System.out.println("============================================================");

            session = HibernateUtil.getSessionFactory().getCurrentSession();
            session.beginTransaction();
            partida = session.get(Partida.class, idPartida);
            System.out.println("Estat actual de la partida: " + partida.getEstat());
            System.out.println("Ronda actual: " + partida.getRonda());
            System.out.println("Jugador actiu: " + partida.getJugadorActiu().getNom() + " (Ordre " + partida.getJugadorActiu().getOrdreTorn() + ")");
            int idJugador1 = partida.getJugadorActiu().getId();
            session.getTransaction().commit();

            // ------------------------------------------------------------
            // 4. ACCIONS DEL TORN EN FASE EN_CURS
            // ------------------------------------------------------------
            System.out.println("\n============================================================");
            System.out.println(">>> 4. JUGANT EL TORN EN FASE EN_CURS (JUGADOR 1)");
            System.out.println("============================================================");

            // A) Intentar passar torn sense tirar els daus (ha de ser rebutjat)
            System.out.println("A) Intentant passar torn sense haver tirat els daus...");
            try {
                jugadorDao.PassarTorn(idJugador1);
                System.err.println("  [ERROR]: Hauria d'haver estat bloquejat!");
            } catch (IllegalStateException e) {
                System.out.println("  [OK]: Bloquejat correctament -> " + e.getMessage());
            }

            // B) Tirar els daus i executar producció
            System.out.println("\nB) Tirant els daus...");
            partidaDao.TirarDaus(idPartida);

            // Si surt un 7, el lladre queda pendent. Com que MoureLladre no s'ha d'implementar en aquesta fase,
            // netegem el flag temporalment per permetre testejar la resta d'accions del torn.
            session = HibernateUtil.getSessionFactory().getCurrentSession();
            session.beginTransaction();
            partida = session.get(Partida.class, idPartida);
            if (partida.isLladrePendent()) {
                System.out.println("  [*] Notificació: Ha sortit 7 (lladre pendent). Com que MoureLladre no s'implementa encara, continuem les proves.");
                partida.setLladrePendent(false);
            }
            // Proporcionem recursos suficients al jugador actiu per provar comerç i millora
            List<RecursJugador> recursosJ1 = session
                    .createNamedQuery("RecursJugador.findByJugador", RecursJugador.class)
                    .setParameter("idJugador", idJugador1)
                    .getResultList();
            for (RecursJugador r : recursosJ1) {
                r.setQuantitat(Math.max(r.getQuantitat(), 8)); // 8 de cada per provar
            }
            session.getTransaction().commit();

            System.out.println("\nC) Recursos del Jugador 1 per a les proves de construcció:");
            jugadorDao.MostrarRecursos(idJugador1);

            // C) Prova de Comerç amb la Banca (4:1)
            System.out.println("\nD) Comerciant amb la banca (4 FUSTA -> 1 BLAT)...");
            TipusRecurs rebut = jugadorDao.ComerciarBanca(idJugador1, TipusRecurs.FUSTA, TipusRecurs.BLAT);
            System.out.println("  [+] Comerç finalitzat amb èxit. Recurs rebut: " + rebut);

            // D) Millorar un Poblat a Ciutat (costa 2 BLAT + 3 MINERAL)
            System.out.println("\nE) Millorant el primer poblat a Ciutat...");
            Vertex vPrimerPoblat = vTriats.get(0); // Vèrtex del primer poblat del Jugador 1
            boolean ciutatOk = jugadorDao.MillorarACiutat(idJugador1, vPrimerPoblat.getId());
            System.out.println("  [+] Poblat millorat a Ciutat: " + (ciutatOk ? "ÈXIT" : "FALLADA"));

            // E) Calcular Punts de Victòria del jugador
            System.out.println("\nF) Calculant punts de victòria del Jugador 1...");
            int punts = jugadorDao.CalcularPunts(idJugador1);
            System.out.println("  [+] Punts de Victòria totals del Jugador 1: " + punts + " punts (1 Ciutat [2pts] + 1 Poblat [1pt])");

            // F) Passar el torn en EN_CURS
            System.out.println("\nG) Passant el torn al següent jugador...");
            jugadorDao.PassarTorn(idJugador1);

            session = HibernateUtil.getSessionFactory().getCurrentSession();
            session.beginTransaction();
            partida = session.get(Partida.class, idPartida);
            System.out.println("  [+] Nou jugador actiu: " + partida.getJugadorActiu().getNom() + " (Ordre " + partida.getJugadorActiu().getOrdreTorn() + ")");
            session.getTransaction().commit();

            // ------------------------------------------------------------
            // 5. RESUM FINAL DE LA PARTIDA
            // ------------------------------------------------------------
            System.out.println("\n============================================================");
            System.out.println(">>> 5. RESUM DE LA PARTIDA DESPRÉS DE LES ACCIONS");
            System.out.println("============================================================");
            partidaDao.MostrarPartida(idPartida);

            System.out.println("\n>>> COMPROVACIÓ DE CONDICIONS DE VICTÒRIA:");
            partidaDao.ComprovarVictoria(idPartida);

            System.out.println("\n============================================================");
            System.out.println("     TOTA LA DEMOSTRACIÓ HA FINALITZAT AMB ÈXIT!            ");
            System.out.println("============================================================");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            HibernateUtil.shutdown();
        }
    }
}
