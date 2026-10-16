package hibernate;

import hibernate.util.HibernateUtil;

public class Main {

    public static void main(String[] args) {
        HibernateUtil.getSessionFactory();
        System.out.println("SessionFactory creada correctament!");
        HibernateUtil.shutdown();
    }

    

}