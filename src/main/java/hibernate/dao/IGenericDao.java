package hibernate.dao;

import java.io.Serializable;
import java.util.List;

public interface IGenericDao <T, ID extends Serializable> {
	// Com a mínim configurarem el CRUD bàsic:

	/*
	 * INSERT i UPDATE
	 */
	void saveOrUpdate(T entity);

	/*
	 * SELECT per ID
	 */
	T get(ID id);

	/*
	 * DELETE per ID
	 */
	void delete(ID id);

	/*
	 * DELETE per objecte
	 */
	void delete(T entity);

	/*
	 * SELECT ALL
	 */
	List<T> list();
}
