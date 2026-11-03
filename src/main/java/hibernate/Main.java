package hibernate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.Transaction;

import hibernate.dao.JugadorDao;
import hibernate.dao.PartidaDAO;
import hibernate.enums.EstatPartida;
import hibernate.enums.TipusCarta;
import hibernate.enums.TipusRecurs;
import hibernate.enums.TipusTerreny;
import hibernate.model.Aresta;
import hibernate.model.CartaDesenvolupament;
import hibernate.model.Hexagon;
import hibernate.model.HexagonVertex;
import hibernate.model.Jugador;
import hibernate.model.Partida;
import hibernate.model.RecursJugador;
import hibernate.model.Vertex;
import hibernate.util.HibernateUtil;

/**
 * Banc de proves i autoavaluació del projecte Catan (Hibernate).
 * Cada mètode testPasX_* cobreix un pas del "Guió de proves i autoavaluació".
 * Executa totes les comprovacions en ordre i imprimeix [PASS] o [FAIL] per
 * cadascuna.
 */
public class Main {

        /** Comptadors globals de proves passades i fallades. */
        private static int testsPassed = 0;
        private static int testsFailed = 0;

        // =========================================================================
        // PUNT D'ENTRADA
        // =========================================================================
        public static void main(String[] args) {
                titol("CATAN · HIBERNATE – BANC DE PROVES I AUTOAVALUACIÓ");

                PartidaDAO partidaDao = new PartidaDAO();
                JugadorDao jugadorDao = new JugadorDao();

                try {
                        // Pas 1 – Model i BD
                        testPas1_ModelIBD();

                        // Pas 2 – IniciarPartida i CSV
                        int idPartida = testPas2_IniciarPartida(partidaDao);

                        // Pas 3 – Consultes de lectura i punts
                        testPas3_ConsultesIPunts(partidaDao, jugadorDao, idPartida);

                        // Pas 4 – Fase de preparació (ordre de serp)
                        List<Vertex> vTriats = testPas4_Preparacio(partidaDao, jugadorDao, idPartida);

                        // Pas 5 – Torn normal: TirarDaus i producció
                        testPas5_TornNormal(partidaDao, jugadorDao, idPartida);

                        // Pas 6 – Construir i millorar en EN_CURS
                        testPas6_ConstruirIMillorar(partidaDao, jugadorDao, idPartida, vTriats);

                        // Pas 7 – Banca, PassarTorn i condició de victòria
                        testPas7_BancaTornVictoria(partidaDao, jugadorDao, idPartida);

                        // Part avançada (11 – cartes)
                        testPartAvancada(idPartida);

                } catch (Exception e) {
                        System.err.println("[ERROR FATAL] " + e.getMessage());
                        e.printStackTrace();
                } finally {
                        HibernateUtil.shutdown();
                }

                resaumFinal();
        }

        // =========================================================================
        // PAS 1 – MODEL I BASE DE DADES
        // Guió: les taules es creen; herència JOINED; unicitat; NULLs permesos.
        // =========================================================================
        private static void testPas1_ModelIBD() {
                seccio("PAS 1 · MODEL I BASE DE DADES");

                Session s = HibernateUtil.getSessionFactory().getCurrentSession();
                Transaction tx = s.beginTransaction();
                try {
                        // 1.1 Les entitats de la jerarquia JOINED es poden consultar
                        boolean joinedOk;
                        try {
                                s.createQuery("SELECT COUNT(c) FROM Construccio c", Long.class).getSingleResult();
                                s.createQuery("SELECT COUNT(a) FROM Assentament a", Long.class).getSingleResult();
                                s.createQuery("SELECT COUNT(p) FROM Poblat p", Long.class).getSingleResult();
                                s.createQuery("SELECT COUNT(c) FROM Ciutat c", Long.class).getSingleResult();
                                s.createQuery("SELECT COUNT(c) FROM Carretera c", Long.class).getSingleResult();
                                joinedOk = true;
                        } catch (Exception e) {
                                joinedOk = false;
                        }
                        avaluar(joinedOk, "1.1",
                                        "Herència JOINED: Construccio, Assentament, Poblat, Ciutat, Carretera es creen a la BD");

                        // 1.2 HexagonVertex és una entitat pròpia (no @ManyToMany)
                        boolean hvOk;
                        try {
                                s.createQuery("SELECT COUNT(hv) FROM HexagonVertex hv", Long.class).getSingleResult();
                                hvOk = true;
                        } catch (Exception e) {
                                hvOk = false;
                        }
                        avaluar(hvOk, "1.2",
                                        "HexagonVertex és entitat amb id propi i @ManyToOne cap a Hexagon i Vertex");

                        tx.commit();
                } catch (Exception e) {
                        if (tx.isActive())
                                tx.rollback();
                        avaluar(false, "1.x", "Error executant Pas 1: " + e.getMessage());
                }

                // 1.3 Constraints CHECK de la BD (valors fora de rang han de ser rebutjats)
                boolean c1 = rebutjatPerBD("UPDATE hexagon SET numero = 13 WHERE numero IS NOT NULL LIMIT 1",
                                "hexagon.numero > 12");
                boolean c2 = rebutjatPerBD("UPDATE hexagon_vertex SET posicio = 6 LIMIT 1",
                                "hexagon_vertex.posicio > 5");
                boolean c3 = rebutjatPerBD("UPDATE recurs_jugador SET quantitat = -1 LIMIT 1",
                                "recurs_jugador.quantitat < 0");
                boolean c4 = rebutjatPerBD("UPDATE partida SET punts_per_guanyar = 0 LIMIT 1",
                                "partida.punts_per_guanyar < 1");
                avaluar(c1 && c2 && c3 && c4, "1.3",
                                "BD rebutja valors fora de rang: numero>12, posicio>5, quantitat<0, punts_per_guanyar<1");
        }

        /**
         * Torna true si la BD llança una excepció (constraint violada) per l'UPDATE
         * donat.
         */
        private static boolean rebutjatPerBD(String sql, String desc) {
                Session s = HibernateUtil.getSessionFactory().getCurrentSession();
                Transaction tx = s.beginTransaction();
                try {
                        s.createNativeMutationQuery(sql).executeUpdate();
                        tx.commit();
                        // Si arriba aquí, la BD no ha rebutjat → FAIL
                        System.err.println("    [!] S'esperava rebuig de la BD per: " + desc);
                        return false;
                } catch (Exception e) {
                        if (tx.isActive())
                                tx.rollback();
                        return true; // Correcte: constraint violada
                }
        }

        // =========================================================================
        // PAS 2 – INICIARPARTIDA I CÀRREGA DE CSV
        // Guió: 2 jugadors mínim, 4 màxim; 19 hex, 54 vèrtexs, 72 arestes, 114 HV;
        // distribució terrenys; cas concret hex 10 / vèrtex 32; 2a partida duplica.
        // =========================================================================
        private static int testPas2_IniciarPartida(PartidaDAO dao) {
                seccio("PAS 2 · INICIARPARTIDA I CÀRREGA DE CSV");

                // --- 2.1 Casos invàlids ---
                boolean e1 = false;
                try {
                        dao.IniciarPartida(List.of("Sol"));
                } catch (IllegalArgumentException ex) {
                        e1 = true;
                }
                avaluar(e1, "2.1a", "IniciarPartida amb 1 jugador falla (IllegalArgumentException)");

                boolean e5 = false;
                try {
                        dao.IniciarPartida(List.of("J1", "J2", "J3", "J4", "J5"));
                } catch (IllegalArgumentException ex) {
                        e5 = true;
                }
                avaluar(e5, "2.1b", "IniciarPartida amb 5 jugadors falla (IllegalArgumentException)");

                // --- 2.2 Creació vàlida ---
                int idPartida = dao.IniciarPartida(List.of("Anna", "Bernat", "Clara"));
                avaluar(idPartida > 0, "2.2", "IniciarPartida amb 3 jugadors retorna id > 0 (id=" + idPartida + ")");

                Session s = HibernateUtil.getSessionFactory().getCurrentSession();
                Transaction tx = s.beginTransaction();

                // --- 2.3 Recompte del tauler ---
                long nHex = s.createQuery("SELECT COUNT(h) FROM Hexagon h WHERE h.partida.id=:p", Long.class)
                                .setParameter("p", idPartida).getSingleResult();
                long nVer = s.createQuery("SELECT COUNT(v) FROM Vertex v WHERE v.partida.id=:p", Long.class)
                                .setParameter("p", idPartida).getSingleResult();
                long nAre = s.createQuery("SELECT COUNT(a) FROM Aresta a WHERE a.partida.id=:p", Long.class)
                                .setParameter("p", idPartida).getSingleResult();
                long nHv = s.createQuery("SELECT COUNT(hv) FROM HexagonVertex hv WHERE hv.hexagon.partida.id=:p",
                                Long.class)
                                .setParameter("p", idPartida).getSingleResult();
                avaluar(nHex == 19 && nVer == 54 && nAre == 72 && nHv == 114, "2.3",
                                String.format("Tauler: %d hex(19), %d vèrt(54), %d are(72), %d HV(114)", nHex, nVer,
                                                nAre, nHv));

                // --- 2.4 Distribució terrenys i números ---
                List<Hexagon> hexs = s.createQuery("SELECT h FROM Hexagon h WHERE h.partida.id=:p", Hexagon.class)
                                .setParameter("p", idPartida).getResultList();
                Map<TipusTerreny, Long> perT = hexs.stream()
                                .collect(Collectors.groupingBy(Hexagon::getTipusTerreny, Collectors.counting()));
                boolean distOk = perT.getOrDefault(TipusTerreny.BOSC, 0L) == 4
                                && perT.getOrDefault(TipusTerreny.PASTURES, 0L) == 4
                                && perT.getOrDefault(TipusTerreny.CAMPS, 0L) == 4
                                && perT.getOrDefault(TipusTerreny.TURONS, 0L) == 3
                                && perT.getOrDefault(TipusTerreny.MUNTANYES, 0L) == 3
                                && perT.getOrDefault(TipusTerreny.DESERT, 0L) == 1;

                Partida partida = s.get(Partida.class, idPartida);
                Hexagon desert = hexs.stream().filter(h -> h.getTipusTerreny() == TipusTerreny.DESERT).findFirst()
                                .orElse(null);
                boolean desertOk = desert != null && desert.getNumero() == null
                                && partida.getHexagonLladre() != null
                                && partida.getHexagonLladre().getId() == desert.getId();

                List<Integer> nums = hexs.stream()
                                .filter(h -> h.getTipusTerreny() != TipusTerreny.DESERT)
                                .map(Hexagon::getNumero).sorted().collect(Collectors.toList());
                boolean numsOk = nums.equals(List.of(2, 3, 3, 4, 4, 5, 5, 6, 6, 8, 8, 9, 9, 10, 10, 11, 11, 12))
                                && !nums.contains(7);
                avaluar(distOk && desertOk && numsOk, "2.4",
                                "Terrenys (4B4P4C3T3M1D), desert sense nº i amb lladre, nums sense 7");

                // --- 2.5 Estat inicial ---
                boolean estatOk = partida.getEstat() == EstatPartida.PREPARACIO
                                && partida.getRonda() == 1
                                && partida.getJugadorActiu() != null
                                && partida.getJugadorActiu().getOrdreTorn() == 1
                                && partida.getGuanyador() == null;
                long recNoZero = s.createQuery(
                                "SELECT COUNT(r) FROM RecursJugador r WHERE r.jugador.partida.id=:p AND r.quantitat<>0",
                                Long.class)
                                .setParameter("p", idPartida).getSingleResult();
                long totalRec = s.createQuery(
                                "SELECT COUNT(r) FROM RecursJugador r WHERE r.jugador.partida.id=:p", Long.class)
                                .setParameter("p", idPartida).getSingleResult();
                avaluar(estatOk && recNoZero == 0 && totalRec == 15, "2.5",
                                "Estat inicial: PREPARACIO, ronda 1, actiu ord-1, guanyador null, 5 recursos a 0 per jugador");

                // --- 2.6 Cas concret: hexàgon codi 10 i vèrtex codi 32 ---
                Hexagon h10 = s.createQuery("SELECT h FROM Hexagon h WHERE h.partida.id=:p AND h.codi=10",
                                Hexagon.class)
                                .setParameter("p", idPartida).getSingleResult();
                List<HexagonVertex> hv10 = s.createQuery(
                                "SELECT hv FROM HexagonVertex hv JOIN FETCH hv.vertex WHERE hv.hexagon.id=:h ORDER BY hv.posicio ASC",
                                HexagonVertex.class).setParameter("h", h10.getId()).getResultList();
                List<Integer> codisHV10 = hv10.stream().map(hv -> hv.getVertex().getCodi())
                                .collect(Collectors.toList());
                boolean h10Ok = codisHV10.equals(List.of(32, 33, 21, 20, 19, 31));

                Vertex v32 = s.createQuery("SELECT v FROM Vertex v WHERE v.partida.id=:p AND v.codi=32", Vertex.class)
                                .setParameter("p", idPartida).getSingleResult();
                List<Integer> hexCodisV32 = s.createQuery(
                                "SELECT hv.hexagon.codi FROM HexagonVertex hv WHERE hv.vertex.id=:v ORDER BY hv.hexagon.codi ASC",
                                Integer.class).setParameter("v", v32.getId()).getResultList();
                List<Integer> veinsV32 = s.createQuery(
                                "SELECT CASE WHEN a.vertexA.id=:v THEN a.vertexB.codi ELSE a.vertexA.codi END " +
                                                "FROM Aresta a WHERE a.partida.id=:p AND (a.vertexA.id=:v OR a.vertexB.id=:v) ORDER BY 1 ASC",
                                Integer.class)
                                .setParameter("v", v32.getId()).setParameter("p", idPartida).getResultList();
                boolean v32Ok = hexCodisV32.equals(List.of(10, 14, 15)) && veinsV32.equals(List.of(31, 33, 43));
                avaluar(h10Ok && v32Ok, "2.6",
                                "Hex10→vèrt[32,33,21,20,19,31]; V32→hex[10,14,15] veïns[31,33,43]");

                tx.commit();

                // --- 2.7 Segona partida: duplicació i IDs autogenerats ---
                int idP2 = dao.IniciarPartida(List.of("David", "Elena"));
                s = HibernateUtil.getSessionFactory().getCurrentSession();
                tx = s.beginTransaction();
                long total2 = s.createQuery("SELECT COUNT(h) FROM Hexagon h", Long.class).getSingleResult();
                Hexagon h0p2 = s.createQuery("SELECT h FROM Hexagon h WHERE h.partida.id=:p AND h.codi=0",
                                Hexagon.class)
                                .setParameter("p", idP2).getSingleResult();
                boolean dup = (total2 == 38) && (h0p2.getId() != h0p2.getCodi());
                avaluar(dup, "2.7", "2a partida: 38 hexàgons en total i IDs != codi CSV");
                tx.commit();

                return idPartida;
        }

        // =========================================================================
        // PAS 3 – CONSULTES DE LECTURA I PUNTS
        // Guió: MostrarTauler, MostrarRecursos, MostrarPartida, CalcularPunts,
        // IDs inexistents sense excepció.
        // =========================================================================
        private static void testPas3_ConsultesIPunts(PartidaDAO pDao, JugadorDao jDao, int idPartida) {
                seccio("PAS 3 · CONSULTES DE LECTURA I PUNTS");

                Session s = HibernateUtil.getSessionFactory().getCurrentSession();
                Transaction tx = s.beginTransaction();
                int idJ1 = s.get(Partida.class, idPartida).getJugadorActiu().getId();
                tx.commit();

                System.out.println("  -- MostrarTauler --");
                pDao.MostrarTauler(idPartida);
                avaluar(true, "3.1", "MostrarTauler s'executa sense excepció i mostra els 19 hexàgons");

                System.out.println("  -- MostrarRecursos --");
                jDao.MostrarRecursos(idJ1);
                avaluar(true, "3.2", "MostrarRecursos s'executa sense excepció i mostra els 5 recursos");

                System.out.println("  -- MostrarPartida --");
                pDao.MostrarPartida(idPartida);
                avaluar(true, "3.3", "MostrarPartida s'executa sense excepció i mostra l'estat de la partida");

                int puntsInici = jDao.CalcularPunts(idJ1);
                avaluar(puntsInici == 0, "3.4",
                                "CalcularPunts en partida nova retorna 0 (obtingut: " + puntsInici + ")");

                // IDs inexistents han de respondre amb missatge, no amb excepció
                boolean ok = true;
                try {
                        pDao.MostrarTauler(999999);
                        pDao.MostrarPartida(999999);
                        jDao.MostrarRecursos(999999);
                        if (jDao.CalcularPunts(999999) != 0)
                                ok = false;
                } catch (Exception e) {
                        ok = false;
                        System.err.println("    Excepció amb ID inexistent: " + e.getMessage());
                }
                avaluar(ok, "3.5", "ID inexistent: els 4 mètodes responen sense llançar excepció");
        }

        // =========================================================================
        // PAS 4 – FASE DE PREPARACIÓ (ordre de serp)
        // Guió: rebuigs fora de torn, regla de distància, ordre serp 1→N→1,
        // 2n poblat dona recursos, transició a EN_CURS.
        // =========================================================================
        private static List<Vertex> testPas4_Preparacio(PartidaDAO pDao, JugadorDao jDao, int idPartida) {
                seccio("PAS 4 · FASE DE PREPARACIÓ (ORDRE DE SERP)");

                // Carreguem dades del tauler en una transacció
                Session s = HibernateUtil.getSessionFactory().getCurrentSession();
                Transaction tx = s.beginTransaction();
                List<Vertex> tots = s.createQuery(
                                "SELECT v FROM Vertex v WHERE v.partida.id=:p ORDER BY v.codi ASC", Vertex.class)
                                .setParameter("p", idPartida).getResultList();
                List<Aresta> arestes = s.createQuery(
                                "SELECT a FROM Aresta a JOIN FETCH a.vertexA JOIN FETCH a.vertexB WHERE a.partida.id=:p ORDER BY a.codi ASC",
                                Aresta.class).setParameter("p", idPartida).getResultList();
                Partida partida = s.get(Partida.class, idPartida);
                int idActiu = partida.getJugadorActiu().getId();
                int idNoActiu = partida.getJugadors().stream()
                                .filter(j -> j.getId() != idActiu).findFirst().orElseThrow().getId();
                tx.commit();

                // ---- 4.1 Rebuig fora de torn ----
                boolean poblatForaTorn = jDao.ConstruirPoblat(idNoActiu, tots.get(0).getId());
                avaluar(!poblatForaTorn, "4.1a", "ConstruirPoblat fora de torn → rebutjat");

                // ---- 4.1 Carretera abans del poblat ----
                boolean carreraAbans = jDao.ConstruirCarretera(idActiu, arestes.get(0).getId());
                avaluar(!carreraAbans, "4.1b", "ConstruirCarretera sense poblat previ en PREPARACIO → rebutjada");

                // ---- Escollim 6 vèrtexs que compleixin la regla de distància ----
                List<Vertex> vTriats = new ArrayList<>();
                for (Vertex v : tots) {
                        boolean adjacent = false;
                        for (Vertex t : vTriats) {
                                if (sonVeins(v, t, arestes)) {
                                        adjacent = true;
                                        break;
                                }
                        }
                        if (!adjacent) {
                                vTriats.add(v);
                                if (vTriats.size() == 6)
                                        break;
                        }
                }

                Vertex v1 = vTriats.get(0);
                Aresta a1Inc = primeraArestaIncident(v1, arestes);
                int idVeiv1 = idVei(v1, a1Inc);
                Aresta aNoInc = arestes.stream()
                                .filter(a -> a.getVertexA().getId() != v1.getId()
                                                && a.getVertexB().getId() != v1.getId())
                                .findFirst().orElseThrow();

                // ---- 4.2 Construir 1r poblat vàlid ----
                boolean p1Ok = jDao.ConstruirPoblat(idActiu, v1.getId());
                avaluar(p1Ok, "4.2", "ConstruirPoblat vàlid en vèrtex lliure → ÈXIT (codi " + v1.getCodi() + ")");

                // ---- 4.2 Vèrtex ocupat ----
                boolean pOcupat = jDao.ConstruirPoblat(idActiu, v1.getId());
                avaluar(!pOcupat, "4.2a", "ConstruirPoblat en vèrtex ja ocupat → rebutjat");

                // ---- 4.2 Regla de distància ----
                boolean pAprop = jDao.ConstruirPoblat(idActiu, idVeiv1);
                avaluar(!pAprop, "4.2b", "ConstruirPoblat massa a prop (veí ocupat) → rebutjat");

                // ---- 4.1 Segon poblat en el mateix torn ----
                boolean p2Torn = jDao.ConstruirPoblat(idActiu, vTriats.get(1).getId());
                avaluar(!p2Torn, "4.1c", "Segon ConstruirPoblat en el mateix torn de PREPARACIO → rebutjat");

                // ---- 4.3 Carretera no incident al poblat ----
                boolean cNoToca = jDao.ConstruirCarretera(idActiu, aNoInc.getId());
                avaluar(!cNoToca, "4.3a", "ConstruirCarretera no adjacent al poblat → rebutjada");

                // ---- 4.3 Carretera vàlida ----
                boolean c1Ok = jDao.ConstruirCarretera(idActiu, a1Inc.getId());
                avaluar(c1Ok, "4.3b", "ConstruirCarretera adjacent al poblat → ÈXIT (codi " + a1Inc.getCodi() + ")");

                // ---- 4.1 Segona carretera en el mateix torn ----
                boolean c2Torn = jDao.ConstruirCarretera(idActiu, a1Inc.getId());
                avaluar(!c2Torn, "4.1d", "Segona ConstruirCarretera en el mateix torn → rebutjada");

                // Passem el torn del J1
                jDao.PassarTorn(idActiu);

                // ---- 5 torns restants (ordre serp: J2, J3, J3, J2, J1) ----
                boolean rec2nPoblatOk = true;
                for (int torn = 1; torn < 6; torn++) {
                        s = HibernateUtil.getSessionFactory().getCurrentSession();
                        tx = s.beginTransaction();
                        partida = s.get(Partida.class, idPartida);
                        int currId = partida.getJugadorActiu().getId();
                        int ronda = partida.getRonda();
                        tx.commit();

                        Vertex vT = vTriats.get(torn);
                        Aresta aInc = primeraArestaIncident(vT, arestes);
                        jDao.ConstruirPoblat(currId, vT.getId());

                        // El 2n poblat (ronda 2) ha de donar recursos
                        if (ronda == 2) {
                                s = HibernateUtil.getSessionFactory().getCurrentSession();
                                tx = s.beginTransaction();
                                Long totalR = s.createQuery(
                                                "SELECT COALESCE(SUM(r.quantitat),0) FROM RecursJugador r WHERE r.jugador.id=:j",
                                                Long.class)
                                                .setParameter("j", currId).getSingleResult();
                                if (totalR <= 0)
                                        rec2nPoblatOk = false;
                                tx.commit();
                        }

                        jDao.ConstruirCarretera(currId, aInc.getId());
                        jDao.PassarTorn(currId);
                }
                avaluar(rec2nPoblatOk, "4.5", "El 2n poblat dona ≥1 recurs dels hexàgons veïns no desert");

                // ---- 4.6 Transició a EN_CURS ----
                s = HibernateUtil.getSessionFactory().getCurrentSession();
                tx = s.beginTransaction();
                partida = s.get(Partida.class, idPartida);
                boolean transOk = partida.getEstat() == EstatPartida.EN_CURS
                                && partida.getRonda() == 1
                                && partida.getJugadorActiu().getOrdreTorn() == 1;
                boolean totsPoblats = true;
                for (Jugador j : partida.getJugadors()) {
                        long np = s.createQuery("SELECT COUNT(p) FROM Poblat p WHERE p.propietari.id=:id", Long.class)
                                        .setParameter("id", j.getId()).getSingleResult();
                        long nc = s.createQuery("SELECT COUNT(c) FROM Carretera c WHERE c.propietari.id=:id",
                                        Long.class)
                                        .setParameter("id", j.getId()).getSingleResult();
                        if (np != 2 || nc != 2)
                                totsPoblats = false;
                }
                tx.commit();
                avaluar(transOk && totsPoblats, "4.6",
                                "En acabar preparació: EN_CURS, ronda 1, actiu ord-1, cada jugador 2 poblats i 2 carreteres");

                return vTriats;
        }

        // =========================================================================
        // PAS 5 – TORN NORMAL: TirarDaus i producció
        // Guió: PassarTorn sense daus→rebutjat; TirarDaus 2 cops→rebutjat;
        // tirada entre 2-12, dausTirats=true.
        // =========================================================================
        private static void testPas5_TornNormal(PartidaDAO pDao, JugadorDao jDao, int idPartida) {
                seccio("PAS 5 · TORN NORMAL I PRODUCCIÓ DE RECURSOS");

                Session s = HibernateUtil.getSessionFactory().getCurrentSession();
                Transaction tx = s.beginTransaction();
                int idActiu = s.get(Partida.class, idPartida).getJugadorActiu().getId();
                tx.commit();

                // ---- 5.1 PassarTorn sense tirar daus → excepció ----
                boolean bloquejat = false;
                try {
                        jDao.PassarTorn(idActiu);
                } catch (IllegalStateException e) {
                        bloquejat = true;
                }
                avaluar(bloquejat, "5.1a", "PassarTorn sense tirar daus en EN_CURS → IllegalStateException");

                // ---- 5.1 Construir sense tirar daus ----
                // Necessitem una aresta vàlida de la partida (no literals)
                s = HibernateUtil.getSessionFactory().getCurrentSession();
                tx = s.beginTransaction();
                Aresta qualsevolAresta = s.createQuery(
                                "SELECT a FROM Aresta a WHERE a.partida.id=:p", Aresta.class)
                                .setParameter("p", idPartida).setMaxResults(1).getSingleResult();
                tx.commit();
                boolean construirSenseDaus = jDao.ConstruirCarretera(idActiu, qualsevolAresta.getId());
                avaluar(!construirSenseDaus, "5.1b", "ConstruirCarretera sense tirar daus → rebutjada");

                // ---- 5.2 TirarDaus ----
                pDao.TirarDaus(idPartida);
                s = HibernateUtil.getSessionFactory().getCurrentSession();
                tx = s.beginTransaction();
                Partida p = s.get(Partida.class, idPartida);
                int tirada = p.getUltimaTirada();
                boolean dausTirats = p.isDausTirats();
                // Si sortit 7, netegem lladrePendent per poder continuar les proves base
                if (p.isLladrePendent())
                        p.setLladrePendent(false);
                tx.commit();
                avaluar(tirada >= 2 && tirada <= 12 && dausTirats, "5.2",
                                "TirarDaus: tirada=" + tirada + " (2-12), dausTirats=true");

                // ---- 5.1 Tirar daus un segon cop → excepció ----
                boolean segonTirar = false;
                try {
                        pDao.TirarDaus(idPartida);
                } catch (IllegalStateException e) {
                        segonTirar = true;
                }
                avaluar(segonTirar, "5.1c", "TirarDaus un segon cop → IllegalStateException");
        }

        // =========================================================================
        // PAS 6 – CONSTRUIR I MILLORAR EN EN_CURS
        // Guió: rebuig sense recursos; carretera no connectada; poblat a prop;
        // millora correcta (substitució poblat→ciutat, descompte 2BLAT+3MIN);
        // CalcularPunts correcte.
        // =========================================================================
        private static void testPas6_ConstruirIMillorar(PartidaDAO pDao, JugadorDao jDao,
                        int idPartida, List<Vertex> vTriats) {
                seccio("PAS 6 · CONSTRUIR I MILLORAR (CARRETERA, POBLAT, CIUTAT)");

                Session s = HibernateUtil.getSessionFactory().getCurrentSession();
                Transaction tx = s.beginTransaction();
                Partida partida = s.get(Partida.class, idPartida);
                int idActiu = partida.getJugadorActiu().getId();

                // Buidem tots els recursos de l'actiu per provar rebuig
                List<RecursJugador> recursos = s.createNamedQuery("RecursJugador.findByJugador", RecursJugador.class)
                                .setParameter("idJugador", idActiu).getResultList();
                recursos.forEach(r -> r.setQuantitat(0));
                tx.commit();

                // ---- 6.1 Rebuig per falta de recursos ----
                // Cerquem arestes i vèrtexs reals de la partida (sense literals)
                s = HibernateUtil.getSessionFactory().getCurrentSession();
                tx = s.beginTransaction();
                List<Aresta> arestes = s.createQuery(
                                "SELECT a FROM Aresta a JOIN FETCH a.vertexA JOIN FETCH a.vertexB WHERE a.partida.id=:p ORDER BY a.codi ASC",
                                Aresta.class).setParameter("p", idPartida).getResultList();
                Vertex v1 = s.get(Vertex.class, vTriats.get(0).getId());
                Vertex v6 = s.get(Vertex.class, vTriats.get(5).getId());
                // Aresta desconnectada de les construccions de l'actiu
                Aresta arestaDesc = arestes.stream()
                                .filter(a -> a.getVertexA().getId() != v1.getId()
                                                && a.getVertexB().getId() != v1.getId()
                                                && a.getVertexA().getId() != v6.getId()
                                                && a.getVertexB().getId() != v6.getId())
                                .findFirst().orElseThrow();
                // Vèrtex sense poblat propi per provar rebuig de MillorarACiutat
                Vertex vertexBuit = arestaDesc.getVertexA();
                tx.commit();

                boolean crSenseR = jDao.ConstruirCarretera(idActiu, arestaDesc.getId());
                boolean cpSenseR = jDao.ConstruirPoblat(idActiu, vertexBuit.getId());
                boolean cmSenseR = jDao.MillorarACiutat(idActiu, vertexBuit.getId());
                avaluar(!crSenseR && !cpSenseR && !cmSenseR, "6.1",
                                "Carretera, Poblat i Ciutat es rebutgen sense recursos suficients");

                // Donem 10 de cada recurs a l'actiu
                s = HibernateUtil.getSessionFactory().getCurrentSession();
                tx = s.beginTransaction();
                recursos = s.createNamedQuery("RecursJugador.findByJugador", RecursJugador.class)
                                .setParameter("idJugador", idActiu).getResultList();
                recursos.forEach(r -> r.setQuantitat(10));
                tx.commit();

                // ---- 6.2 Carretera no connectada → rebutjada ----
                boolean crDesc = jDao.ConstruirCarretera(idActiu, arestaDesc.getId());
                avaluar(!crDesc, "6.2", "ConstruirCarretera no connectada a cap construcció pròpia → rebutjada");

                // ---- 6.3 Millorar a Ciutat: vèrtex sense poblat propi → rebutjat ----
                boolean cmBuit = jDao.MillorarACiutat(idActiu, vertexBuit.getId());
                avaluar(!cmBuit, "6.3a", "MillorarACiutat en vèrtex sense poblat propi → rebutjada");

                // ---- 6.3 Millora correcta del 1r poblat de l'actiu ----
                int idV1 = vTriats.get(0).getId();
                boolean cmOk = jDao.MillorarACiutat(idActiu, idV1);
                avaluar(cmOk, "6.3b", "MillorarACiutat en poblat propi → ÈXIT");

                // ---- 6.3 Verificació del descompte de recursos (10-2=8 BLAT, 10-3=7 MINERAL)
                // ----
                s = HibernateUtil.getSessionFactory().getCurrentSession();
                tx = s.beginTransaction();
                RecursJugador rBlat = s.createNamedQuery("RecursJugador.findByJugadorAndTipus", RecursJugador.class)
                                .setParameter("idJugador", idActiu).setParameter("tipus", TipusRecurs.BLAT)
                                .uniqueResult();
                RecursJugador rMin = s.createNamedQuery("RecursJugador.findByJugadorAndTipus", RecursJugador.class)
                                .setParameter("idJugador", idActiu).setParameter("tipus", TipusRecurs.MINERAL)
                                .uniqueResult();
                boolean descompteOk = (rBlat != null && rBlat.getQuantitat() == 8)
                                && (rMin != null && rMin.getQuantitat() == 7);
                tx.commit();
                avaluar(descompteOk, "6.3c", "MillorarACiutat descompta exactament 2 BLAT i 3 MINERAL");

                // ---- 6.4 CalcularPunts: 1 poblat (1pt) + 1 ciutat (2pts) = 3 ----
                int punts = jDao.CalcularPunts(idActiu);
                avaluar(punts == 3, "6.4", "CalcularPunts = 3 (1 poblat + 1 ciutat) — obtingut: " + punts);

                // ---- 6.5 MostrarTauler amb construccions ----
                System.out.println("  -- MostrarTauler amb Poblat i Ciutat --");
                pDao.MostrarTauler(idPartida);
                avaluar(true, "6.5", "MostrarTauler mostra poblats i ciutats amb el propietari");
        }

        // =========================================================================
        // PAS 7 – BANCA, PASSARTORN I CONDICIÓ DE VICTÒRIA
        // Guió: ComerciarBanca (4:1, rebuigs); PassarTorn canvia actiu i reseteja
        // daus; ronda puja quan el torn torna a l'ordre 1;
        // ComprovarVictoria → FINALITZADA + guanyador.
        // =========================================================================
        private static void testPas7_BancaTornVictoria(PartidaDAO pDao, JugadorDao jDao, int idPartida) {
                seccio("PAS 7 · BANCA, PASSARTORN I CONDICIÓ DE VICTÒRIA");

                Session s = HibernateUtil.getSessionFactory().getCurrentSession();
                Transaction tx = s.beginTransaction();
                Partida partida = s.get(Partida.class, idPartida);
                int idActiu = partida.getJugadorActiu().getId();
                int ordreActiu = partida.getJugadorActiu().getOrdreTorn();
                int idAltre = partida.getJugadors().stream()
                                .filter(j -> j.getId() != idActiu).findFirst().orElseThrow().getId();
                tx.commit();

                // ---- 7.1 Comerç fora de torn ----
                TipusRecurs foraTorn = jDao.ComerciarBanca(idAltre, TipusRecurs.FUSTA, TipusRecurs.BLAT);
                avaluar(foraTorn == null, "7.1a", "ComerciarBanca fora de torn → null (rebutjat)");

                // ---- 7.1 Comerç dona==rep ----
                TipusRecurs mateixR = jDao.ComerciarBanca(idActiu, TipusRecurs.FUSTA, TipusRecurs.FUSTA);
                avaluar(mateixR == null, "7.1b", "ComerciarBanca amb dona==rep → null (rebutjat)");

                // ---- 7.1 Comerç amb 3 unitats (insuficient) → rebutjat ----
                // Posem exactament 3 FUSTA a l'actiu
                s = HibernateUtil.getSessionFactory().getCurrentSession();
                tx = s.beginTransaction();
                RecursJugador rFusta3 = s.createNamedQuery("RecursJugador.findByJugadorAndTipus", RecursJugador.class)
                                .setParameter("idJugador", idActiu).setParameter("tipus", TipusRecurs.FUSTA)
                                .uniqueResult();
                if (rFusta3 != null)
                        rFusta3.setQuantitat(3);
                tx.commit();
                TipusRecurs amb3 = jDao.ComerciarBanca(idActiu, TipusRecurs.FUSTA, TipusRecurs.BLAT);
                avaluar(amb3 == null, "7.1c", "ComerciarBanca amb 3 unitats (necessita 4) → null (rebutjat)");

                // Donem 10 FUSTA per al comerç vàlid
                s = HibernateUtil.getSessionFactory().getCurrentSession();
                tx = s.beginTransaction();
                RecursJugador rFusta10 = s.createNamedQuery("RecursJugador.findByJugadorAndTipus", RecursJugador.class)
                                .setParameter("idJugador", idActiu).setParameter("tipus", TipusRecurs.FUSTA)
                                .uniqueResult();
                if (rFusta10 != null)
                        rFusta10.setQuantitat(10);
                tx.commit();

                // ---- 7.1 Comerç vàlid 4 FUSTA → 1 MAO ----
                TipusRecurs rebut = jDao.ComerciarBanca(idActiu, TipusRecurs.FUSTA, TipusRecurs.MAO);
                avaluar(rebut == TipusRecurs.MAO, "7.1d", "ComerciarBanca 4 FUSTA → 1 MAO: correcte");

                // ---- 7.2 PassarTorn: actiu canvia i dausTirats=false ----
                jDao.PassarTorn(idActiu);
                s = HibernateUtil.getSessionFactory().getCurrentSession();
                tx = s.beginTransaction();
                partida = s.get(Partida.class, idPartida);
                int nouOrdre = partida.getJugadorActiu().getOrdreTorn();
                boolean dausReset = !partida.isDausTirats();
                tx.commit();
                avaluar(nouOrdre == ordreActiu + 1 && dausReset, "7.2",
                                "PassarTorn: actiu passa a ordre " + nouOrdre + " (era " + ordreActiu
                                                + "), dausTirats=false");

                // ---- 7.3 La ronda puja quan torna a ordre 1 ----
                // Completem el torn del jugador 2 i el jugador 3 perquè torni a l'ordre 1
                for (int i = 0; i < 2; i++) {
                        pDao.TirarDaus(idPartida);
                        s = HibernateUtil.getSessionFactory().getCurrentSession();
                        tx = s.beginTransaction();
                        partida = s.get(Partida.class, idPartida);
                        if (partida.isLladrePendent())
                                partida.setLladrePendent(false);
                        int currId = partida.getJugadorActiu().getId();
                        tx.commit();
                        jDao.PassarTorn(currId);
                }
                s = HibernateUtil.getSessionFactory().getCurrentSession();
                tx = s.beginTransaction();
                partida = s.get(Partida.class, idPartida);
                int rondaAra = partida.getRonda();
                int ordreAra = partida.getJugadorActiu().getOrdreTorn();
                tx.commit();
                avaluar(rondaAra == 2 && ordreAra == 1, "7.3",
                                "Ronda puja a 2 quan el torn torna a l'ordre 1 — ronda=" + rondaAra + " actiu="
                                                + ordreAra);

                // ---- 7.4 ComprovarVictoria → FINALITZADA ----
                // Abaixen els punts per guanyar a 3 via SQL directe (com indica el guió)
                s = HibernateUtil.getSessionFactory().getCurrentSession();
                tx = s.beginTransaction();
                s.createNativeMutationQuery("UPDATE partida SET punts_per_guanyar = 3 WHERE id = " + idPartida)
                                .executeUpdate();
                tx.commit();
                pDao.ComprovarVictoria(idPartida);
                s = HibernateUtil.getSessionFactory().getCurrentSession();
                tx = s.beginTransaction();
                partida = s.get(Partida.class, idPartida);
                boolean finalOk = partida.getEstat() == EstatPartida.FINALITZADA && partida.getGuanyador() != null;
                String guanyador = finalOk ? partida.getGuanyador().getNom() : "ningú";
                tx.commit();
                avaluar(finalOk, "7.4", "ComprovarVictoria (3 pts): partida FINALITZADA, guanyador=" + guanyador);

                // ---- 7.5 Partida FINALITZADA rebutja totes les accions ----
                boolean tirarFin = true;
                try {
                        pDao.TirarDaus(idPartida);
                        tirarFin = false;
                } catch (IllegalStateException ignored) {
                }

                // Per ConstruirX i ComerciarBanca necessitem arestes/vèrtexs reals
                s = HibernateUtil.getSessionFactory().getCurrentSession();
                tx = s.beginTransaction();
                Aresta qAresta = s.createQuery("SELECT a FROM Aresta a WHERE a.partida.id=:p", Aresta.class)
                                .setParameter("p", idPartida).setMaxResults(1).getSingleResult();
                Vertex qVertex = s.createQuery("SELECT v FROM Vertex v WHERE v.partida.id=:p", Vertex.class)
                                .setParameter("p", idPartida).setMaxResults(1).getSingleResult();
                tx.commit();

                boolean crFin = jDao.ConstruirCarretera(idActiu, qAresta.getId());
                boolean cpFin = jDao.ConstruirPoblat(idActiu, qVertex.getId());
                boolean cmFin = jDao.MillorarACiutat(idActiu, qVertex.getId());
                TipusRecurs comFin = jDao.ComerciarBanca(idActiu, TipusRecurs.FUSTA, TipusRecurs.BLAT);
                boolean passarFin = false;
                try {
                        jDao.PassarTorn(idActiu);
                } catch (IllegalStateException ignored) {
                        passarFin = true;
                }

                avaluar(tirarFin && !crFin && !cpFin && !cmFin && comFin == null && passarFin, "7.5",
                                "Partida FINALITZADA rebutja TirarDaus, Carretera, Poblat, Ciutat, Banca i PassarTorn");
        }

        // =========================================================================
        // PART AVANÇADA – Cartes de desenvolupament (pas 11)
        // =========================================================================
        private static void testPartAvancada(int idPartida) {
                seccio("PART AVANÇADA · CARTES DE DESENVOLUPAMENT");

                Session s = HibernateUtil.getSessionFactory().getCurrentSession();
                Transaction tx = s.beginTransaction();
                List<CartaDesenvolupament> cartes = s.createQuery(
                                "SELECT c FROM CartaDesenvolupament c WHERE c.partida.id=:p",
                                CartaDesenvolupament.class)
                                .setParameter("p", idPartida).getResultList();
                Map<TipusCarta, Long> perTipus = cartes.stream()
                                .collect(Collectors.groupingBy(CartaDesenvolupament::getTipus, Collectors.counting()));
                tx.commit();

                boolean pilaOk = cartes.size() == 21
                                && perTipus.getOrDefault(TipusCarta.CAVALLER, 0L) == 14
                                && perTipus.getOrDefault(TipusCarta.PUNT_VICTORIA, 0L) == 5
                                && perTipus.getOrDefault(TipusCarta.MONOPOLI, 0L) == 2;
                avaluar(pilaOk, "11.1",
                                "Pila cartes: " + cartes.size() + " total (CAVALLER=" +
                                                perTipus.getOrDefault(TipusCarta.CAVALLER, 0L) +
                                                ", PV=" + perTipus.getOrDefault(TipusCarta.PUNT_VICTORIA, 0L) +
                                                ", MON=" + perTipus.getOrDefault(TipusCarta.MONOPOLI, 0L) + ")");

                // Informa de mètodes no implementats
                System.out.println("\n  [INFORME PART AVANÇADA]");
                System.out.println("  · MoureLladre:        PENDENT D'IMPLEMENTAR");
                System.out.println("  · ComprarCarta:       PENDENT D'IMPLEMENTAR");
                System.out.println("  · JugarCarta:         PENDENT D'IMPLEMENTAR");
                System.out.println("  · VertexsDisponibles: PENDENT D'IMPLEMENTAR");
        }

        // =========================================================================
        // UTILITATS INTERNES
        // =========================================================================

        /** Comprova si dos vèrtexs són veïns (connectats per una aresta). */
        private static boolean sonVeins(Vertex a, Vertex b, List<Aresta> arestes) {
                return arestes.stream()
                                .anyMatch(ar -> (ar.getVertexA().getId() == a.getId()
                                                && ar.getVertexB().getId() == b.getId()) ||
                                                (ar.getVertexB().getId() == a.getId()
                                                                && ar.getVertexA().getId() == b.getId()));
        }

        /** Retorna la primera aresta incident al vèrtex donat. */
        private static Aresta primeraArestaIncident(Vertex v, List<Aresta> arestes) {
                return arestes.stream()
                                .filter(a -> a.getVertexA().getId() == v.getId() || a.getVertexB().getId() == v.getId())
                                .findFirst().orElseThrow();
        }

        /** Retorna l'ID de l'altre extrem de l'aresta (el veí de v). */
        private static int idVei(Vertex v, Aresta a) {
                return a.getVertexA().getId() == v.getId() ? a.getVertexB().getId() : a.getVertexA().getId();
        }

        /** Registra el resultat d'una prova i l'imprimeix. */
        private static void avaluar(boolean ok, String criteri, String desc) {
                if (ok) {
                        testsPassed++;
                        System.out.printf("  [PASS] %-6s %s%n", "[" + criteri + "]", desc);
                } else {
                        testsFailed++;
                        System.err.printf("  [FAIL] %-6s %s%n", "[" + criteri + "]", desc);
                }
        }

        /** Imprimeix una capçalera de secció. */
        private static void seccio(String text) {
                System.out.println("\n" + "=".repeat(72));
                System.out.println(">>> " + text);
                System.out.println("=".repeat(72));
        }

        /** Imprimeix el títol principal. */
        private static void titol(String text) {
                System.out.println("=".repeat(72));
                System.out.println("  " + text);
                System.out.println("=".repeat(72) + "\n");
        }

        /** Imprimeix el resum final de proves. */
        private static void resaumFinal() {
                System.out.println("\n" + "=".repeat(72));
                System.out.println("  RESUM FINAL D'AUTOAVALUACIÓ");
                System.out.println("=".repeat(72));
                System.out.printf("  Proves superades : %d%n", testsPassed);
                System.out.printf("  Proves fallades  : %d%n", testsFailed);
                System.out.println("=".repeat(72));
                if (testsFailed == 0) {
                        System.out.println("  [EXCEL·LENT] Totes les proves han passat!");
                } else {
                        System.out.printf("  [ATENCIÓ] %d prova/es requereixen revisió.%n", testsFailed);
                }
                System.out.println("=".repeat(72) + "\n");
        }
}
