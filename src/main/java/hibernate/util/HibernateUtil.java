package hibernate.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import hibernate.enums.TipusTerreny;
import hibernate.model.Aresta;
import hibernate.model.Hexagon;
import hibernate.model.HexagonVertex;
import hibernate.model.Partida;
import hibernate.model.Vertex;

public class HibernateUtil {

    private static final String CARPETA_CSV = "tauler/";
    private static final SessionFactory sessionFactory = buildSessionFactory();

    // ------------------------------------------------------------
    // SessionFactory
    // ------------------------------------------------------------

    private static SessionFactory buildSessionFactory() {
        try {
            return new Configuration().configure().buildSessionFactory();
        } catch (Throwable ex) {
            System.err.println("Error creant la SessionFactory: " + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        getSessionFactory().close();
    }

    // ------------------------------------------------------------
    // Càrrega del tauler des dels CSV (Opció A: rep la Partida)
    // ------------------------------------------------------------

    public static void carregarDades(Session session, Partida partida) {
        // 1) Vèrtexs (54)
        Map<Integer, Vertex> vertexs = carregarVertexs(session);

        // 2) Hexàgons (19) -> necessiten la Partida
        Map<Integer, Hexagon> hexagons = carregarHexagons(session, partida);

        // 3) HexagonVertex (114) -> enllaça hexàgons i vèrtexs
        carregarHexagonVertex(session, hexagons, vertexs);

        // 4) Arestes (72) -> enllaça dos vèrtexs
        carregarArestes(session, vertexs);

        System.out.println("Tauler carregat correctament!");
    }

    // ------------------------------------------------------------
    // 1) Vertex
    // ------------------------------------------------------------
    private static Map<Integer, Vertex> carregarVertexs(Session session) {
        List<String[]> files = llegirCsv("vertexs.csv");
        Map<Integer, Vertex> vertexs = new HashMap<>();

        for (String[] fila : files) {
            int codi = Integer.parseInt(fila[0]);
            double x = Double.parseDouble(fila[1]);
            double y = Double.parseDouble(fila[2]);

            Vertex v = new Vertex();
            v.setCodi(codi);
            v.setX(x);
            v.setY(y);

            session.persist(v);
            vertexs.put(codi, v);
        }
        System.out.println("Carregats " + vertexs.size() + " vèrtexs.");
        return vertexs;
    }

    // ------------------------------------------------------------
    // 2) Hexagon (rep la Partida)
    // ------------------------------------------------------------
    private static Map<Integer, Hexagon> carregarHexagons(Session session, Partida partida) {
        List<String[]> files = llegirCsv("hexagons.csv");
        Map<Integer, Hexagon> hexagons = new HashMap<>();

        // Terrenys: 4 BOSC, 4 PASTURES, 4 CAMPS, 3 TURONS, 3 MUNTANYES, 1 DESERT
        List<TipusTerreny> terrenys = new ArrayList<>();
        for (int i = 0; i < 4; i++) terrenys.add(TipusTerreny.BOSC);
        for (int i = 0; i < 4; i++) terrenys.add(TipusTerreny.PASTURES);
        for (int i = 0; i < 4; i++) terrenys.add(TipusTerreny.CAMPS);
        for (int i = 0; i < 3; i++) terrenys.add(TipusTerreny.TURONS);
        for (int i = 0; i < 3; i++) terrenys.add(TipusTerreny.MUNTANYES);
        terrenys.add(TipusTerreny.DESERT);

        // Números: 2,3,3,4,4,5,5,6,6,8,8,9,9,10,10,11,11,12
        List<Integer> numeros = new ArrayList<>(List.of(
                2, 3, 3, 4, 4, 5, 5, 6, 6, 8, 8, 9, 9, 10, 10, 11, 11, 12));

        Collections.shuffle(terrenys);
        Collections.shuffle(numeros);

        int indexTerreny = 0;
        int indexNumero = 0;

        for (String[] fila : files) {
            int codi = Integer.parseInt(fila[0]);
            int q = Integer.parseInt(fila[1]);
            int r = Integer.parseInt(fila[2]);

            Hexagon h = new Hexagon();
            h.setCodi(codi);
            h.setQ(q);
            h.setR(r);

            TipusTerreny terreny = terrenys.get(indexTerreny++);
            h.setTipusTerreny(terreny);

            if (terreny == TipusTerreny.DESERT) {
                h.setNumero(null);
                partida.setHexagonLladre(h);
            } else {
                h.setNumero(numeros.get(indexNumero++));
            }

            partida.addHexagon(h);

            session.persist(h);
            hexagons.put(codi, h);
        }
        System.out.println("Carregats " + hexagons.size() + " hexàgons.");
        return hexagons;
    }

    // ------------------------------------------------------------
    // 3) HexagonVertex
    // ------------------------------------------------------------
    private static void carregarHexagonVertex(Session session,
                                              Map<Integer, Hexagon> hexagons,
                                              Map<Integer, Vertex> vertexs) {
        List<String[]> files = llegirCsv("hexagon_vertex.csv");
        int contador = 0;

        for (String[] fila : files) {
            int codiHexagon = Integer.parseInt(fila[0]);
            int codiVertex = Integer.parseInt(fila[1]);
            int posicio = Integer.parseInt(fila[2]);

            Hexagon h = hexagons.get(codiHexagon);
            Vertex v = vertexs.get(codiVertex);

            if (h == null) throw new IllegalStateException(
                    "Hexàgon no trobat: " + codiHexagon);
            if (v == null) throw new IllegalStateException(
                    "Vèrtex no trobat: " + codiVertex);

            HexagonVertex hv = new HexagonVertex();
            hv.setHexagon(h);
            hv.setVertex(v);
            hv.setPosicio(posicio);

            session.persist(hv);
            contador++;
        }
        System.out.println("Carregats " + contador + " HexagonVertex.");
    }

    // ------------------------------------------------------------
    // 4) Aresta
    // ------------------------------------------------------------
    private static void carregarArestes(Session session,
                                        Map<Integer, Vertex> vertexs) {
        List<String[]> files = llegirCsv("arestes.csv");
        int contador = 0;

        for (String[] fila : files) {
            int codi = Integer.parseInt(fila[0]);
            int codiA = Integer.parseInt(fila[1]);
            int codiB = Integer.parseInt(fila[2]);

            Vertex a = vertexs.get(codiA);
            Vertex b = vertexs.get(codiB);

            if (a == null) throw new IllegalStateException(
                    "Vèrtex A no trobat: " + codiA);
            if (b == null) throw new IllegalStateException(
                    "Vèrtex B no trobat: " + codiB);

            Aresta aresta = new Aresta();
            aresta.setCodi(codi);
            aresta.setVertexA(a);
            aresta.setVertexB(b);

            session.persist(aresta);
            contador++;
        }
        System.out.println("Carregades " + contador + " arestes.");
    }

    // ------------------------------------------------------------
    // Lectura genèrica de CSV
    // ------------------------------------------------------------

    private static List<String[]> llegirCsv(String nomFitxer) {
        InputStream is = HibernateUtil.class.getClassLoader()
                .getResourceAsStream(CARPETA_CSV + nomFitxer);
        if (is == null) {
            throw new IllegalStateException(
                    "No s'ha trobat el fitxer " + CARPETA_CSV + nomFitxer);
        }

        List<String[]> files = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {

            br.readLine(); // saltar capçalera
            String linia;
            while ((linia = br.readLine()) != null) {
                if (!linia.isBlank()) {
                    String[] camps = linia.split(",", -1);
                    for (int i = 0; i < camps.length; i++) {
                        camps[i] = camps[i].trim();
                    }
                    files.add(camps);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error llegint " + nomFitxer, e);
        }
        return files;
    }
}