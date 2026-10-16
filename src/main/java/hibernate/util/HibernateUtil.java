package hibernate.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import hibernate.Main;
import hibernate.model.Aresta;
import hibernate.model.Hexagon;
import hibernate.model.Vertex;

public class HibernateUtil {

    private static final String CARPETA_CSV = "tauler/";
    private static final SessionFactory sessionFactory = buildSessionFactory();

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

    private static void carregarDades() {
        try (Session session = getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                // 1) Entitats independents (sense FK)
                Map<String, Hexagon> categories = carregarHexagons(session);
                Map<String, Vertex> plataformes = carregarVertexs(session);
                Map<String, Aresta> estudis = carregarArestes(session);

                // 2) Entitats amb FK cap a les anteriors (N:1) + la seva 1:1
                Map<String, Videojoc> videojocs = carregarVideojocs(session, estudis);

                // 3) Fitxers de RELACIÓ: només enllacen entitats que ja existeixen
                int nmPura = carregarVideojocCategoria(videojocs, categories);
                int nmAtributs = carregarLlancaments(videojocs, plataformes);

                tx.commit();
            } catch (RuntimeException e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                System.err.println("Error durant la càrrega: " + e.getMessage());
                throw e;
            }
        }
    }

    private static Map<String, Hexagon> carregarHexagons(Session session) {
        List<String[]> files = llegirCsv("hexagons.csv");
        Map<String, Hexagon> hexagons = new LinkedHashMap<>();

        for (String[] fila : files) { // 1a passada: crear-les totes
            hexagons.put(fila[0], new Hexagon(fila[0]));
        }
        for (String[] fila : files) { // 2a passada: enllaçar amb el pare
            if (!fila[1].isEmpty()) {
                Hexagon pare = obtenir(hexagons, fila[1], "hexagons.csv");
                pare.afegirFilla(hexagons.get(fila[0]));
            }
        }
        for (Hexagon c : hexagons.values()) { // persist de les ARRELS: cascade a les filles
            if (c.getPare() == null) {
                session.persist(c);
            }
        }
        return hexagons;
    }

    private static Map<String, Vertex> carregarVertexs(Session session) {
        List<String[]> files = llegirCsv("vertexs.csv");
        Map<String, Vertex> vertexs = new LinkedHashMap<>();

        for (String[] fila : files) { // 1a passada: crear-les totes
            vertexs.put(fila[0], new Vertex(fila[0]));
        }
        for (String[] fila : files) { // 2a passada: enllaçar amb el pare
            if (!fila[1].isEmpty()) {
                Vertex pare = obtenir(vertexs, fila[1], "vertexs.csv");
                pare.afegirFilla(vertexs.get(fila[0]));
            }
        }
        for (Vertex c : vertexs.values()) { // persist de les ARRELS: cascade a les filles
            if (c.getPare() == null) {
                session.persist(c);
            }
        }
        return vertexs;
    }

    private static Map<String, Aresta> carregarArestes(Session session) {
        List<String[]> files = llegirCsv("arestes.csv");
        Map<String, Aresta> arestes = new LinkedHashMap<>();

        for (String[] fila : files) { // 1a passada: crear-les totes
            arestes.put(fila[0], new Aresta(fila[0]));
        }
        for (String[] fila : files) { // 2a passada: enllaçar amb el pare
            if (!fila[1].isEmpty()) {
                Aresta pare = obtenir(arestes, fila[1], "arestes.csv");
                pare.afegirFilla(arestes.get(fila[0]));
            }
        }
        for (Aresta c : arestes.values()) { // persist de les ARRELS: cascade a les filles
            if (c.getPare() == null) {
                session.persist(c);
            }
        }
        return arestes;
    }

    private static Map<String, Hexagon> carregarHexagons(Session session) {
        List<String[]> files = llegirCsv("hexagons.csv");
        Map<String, Hexagon> hexagons = new LinkedHashMap<>();

        for (String[] fila : files) { // 1a passada: crear-les totes
            hexagons.put(fila[0], new Hexagon(fila[0]));
        }
        for (String[] fila : files) { // 2a passada: enllaçar amb el pare
            if (!fila[1].isEmpty()) {
                Hexagon pare = obtenir(hexagons, fila[1], "hexagons.csv");
                pare.afegirFilla(hexagons.get(fila[0]));
            }
        }
        for (Hexagon c : hexagons.values()) { // persist de les ARRELS: cascade a les filles
            if (c.getPare() == null) {
                session.persist(c);
            }
        }
        return hexagons;
    }

    private static List<String[]> llegirCsv(String nomFitxer) {
        InputStream is = Main.class.getClassLoader().getResourceAsStream(CARPETA_CSV + nomFitxer);
        if (is == null) {
            throw new IllegalStateException("No s'ha trobat el fitxer " + CARPETA_CSV + nomFitxer);
        }
        List<String[]> files = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String linia = br.readLine();
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
            throw new IllegalStateException("Error llegint el fitxer " + nomFitxer, e);
        }
        return files;
    }

}