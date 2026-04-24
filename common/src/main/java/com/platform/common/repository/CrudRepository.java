package com.platform.common.repository;

import java.util.List;
import java.util.Optional;

/**
 * Minimal generic CRUD contract used by any data store (JDBC, Mongo, in-memory).
 * Lets higher layers depend on the contract instead of a specific driver.
 *
 * @param <T>  entity type
 * @param <ID> primary-key type
 */
public interface CrudRepository<T, ID> {

    T save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    void deleteById(ID id);

    long count();
}
