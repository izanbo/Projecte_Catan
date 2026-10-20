package hibernate.dao;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import hibernate.util.HibernateUtil;


public class GenericDAO<T, ID extends Serializable> implements IGenericDao<T, ID> {

	protected SessionFactory sessionFactory;

	// Utilitzem el constructor per obtenir la SessionFactory (singleton)
	public GenericDAO() {
		sessionFactory = HibernateUtil.getSessionFactory();
	}

	/*
	 * Ara si, desenvolupem el codi de manera genèrica ja que totes les classes necessiten aquests mètodes
	 */
	@Override
	public void saveOrUpdate(T entity) {
		Session session = sessionFactory.getCurrentSession();
		try {
			// Tota operació de modificació necessita una transacció
			session.beginTransaction();
			session.merge(entity);
			session.getTransaction().commit();
		} catch (RuntimeException e) {
			handleException(session, e);
		}
	}

	@Override
	public T get(ID id) {
		Session session = sessionFactory.getCurrentSession();
		try {
			session.beginTransaction();
			// Recuperem la classe mitjançant getEntityClass
			T entity = session.get(getEntityClass(), id);
			session.getTransaction().commit();
			return entity;
		} catch (RuntimeException e) {
			handleException(session, e);
			return null;
		}
	}

	@Override
	public void delete(ID id) {
		Session session = sessionFactory.getCurrentSession();
		try {
			session.beginTransaction();
			// Dins de la transacció fem servir la mateixa sessió
			// (no podem cridar get() perquè obriria una altra transacció)
			T entity = session.get(getEntityClass(), id);
			if (entity != null) {
				session.remove(entity);
			} else {
				System.out.println("No existeix aquest ID");
			}
			session.getTransaction().commit();
		} catch (RuntimeException e) {
			handleException(session, e);
		}
	}

	@Override
	public void delete(T entity) {
		Session session = sessionFactory.getCurrentSession();
		try {
			session.beginTransaction();
			// Si l'objecte ve d'una altra sessió (detached), remove() el rebutja:
			// primer el tornem gestionat amb merge
			session.remove(session.merge(entity));
			session.getTransaction().commit();
		} catch (RuntimeException e) {
			handleException(session, e);
		}
	}

	@Override
	public List<T> list() {
		Session session = sessionFactory.getCurrentSession();
		try {
			session.beginTransaction();
			// Fem el SELECT ALL
			List<T> entities = session.createQuery("FROM " + getEntityClass().getName(), getEntityClass())
					.getResultList();
			session.getTransaction().commit();
			return entities;
		} catch (RuntimeException e) {
			handleException(session, e);
			return null;
		}
	}

	/**
	 * Mètode auxiliar que utilitza reflection per obtenir la classe de l'entitat
	 */
	@SuppressWarnings("unchecked")
	private Class<T> getEntityClass() {
		return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}

	/**
	 * Mètode auxiliar per gestionar les excepcions: les mostrem i fem rollback.
	 * És protected perquè també el fan servir els DAO concrets.
	 * RuntimeException i no HibernateException: també volem rollback amb
	 * IllegalArgumentException, IllegalStateException, errors de validació...
	 */
	protected void handleException(Session session, RuntimeException e) {
		e.printStackTrace();
		if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
			session.getTransaction().rollback();
		}
	}
}
