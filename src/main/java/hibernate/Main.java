package hibernate;

import java.sql.Date;
import java.time.LocalDate;

import org.hibernate.Session;
import org.hibernate.Transaction;

import hibernate.enums.EstatPartida;
import hibernate.model.Partida;
import hibernate.util.HibernateUtil;

public class Main {
    public static void main(String[] args) {
        Partida partida = new Partida();
        partida.setEstat(EstatPartida.PREPARACIO);
        partida.setDataInici(Date.valueOf(LocalDate.now()));
        partida.setRonda(0);
        partida.setUltimaTirada(0);
        partida.setPuntsPerGuanyar(10);
        partida.setDausTirats(false);
        partida.setLladre(false);

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            session.persist(partida);
            tx.commit();
        }

        HibernateUtil.carregarDades(partida);

        HibernateUtil.shutdown();
    }
}