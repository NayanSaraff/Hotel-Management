package com.hotel.dao;

import java.util.List;
import java.util.Optional;

/**
 * Generic CRUD DAO interface.
 *
 * @param <T>  Entity type
 * @param <ID> Primary key type
 */
public interface GenericDAO<T, ID> {

    /**
     * Persist a new entity. Returns the generated ID.
     */
    int save(T entity);

    /**
     * Update an existing entity.
     */
    boolean update(T entity);

    /**
     * Delete an entity by primary key.
     */
    boolean delete(ID id);

    /**
     * Find an entity by primary key.
     */
    Optional<T> findById(ID id);

    /**
     * Return all entities.
     */
    List<T> findAll();
}
