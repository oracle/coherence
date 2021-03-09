/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.repository;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.NamedMap;

import com.tangosol.util.Aggregators;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.Extractors;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.Fragment;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.Processors;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;

import com.tangosol.util.comparator.ExtractorComparator;

import com.tangosol.util.extractor.DeserializationAccelerator;

import com.tangosol.util.filter.MapEventFilter;

import com.tangosol.util.function.Remote;

import com.tangosol.util.stream.RemoteCollector;
import com.tangosol.util.stream.RemoteCollectors;
import com.tangosol.util.stream.RemoteStream;

import java.math.BigDecimal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tangosol.util.function.Remote.Comparator.comparing;

import static com.tangosol.util.stream.RemoteCollectors.groupingBy;

/**
 * Abstract base class for Coherence
 * <a href="https://martinfowler.com/eaaCatalog/repository.html">repository</a>
 * implementations.
 *
 * @param <ID>  the type of entity's identifier
 * @param <T>   the type of entity stored in this repository
 *
 * @author Aleks Seovic  2021.01.28
 * @since 21.06
 */
public abstract class AbstractRepository<ID, T>
    {
    // ---- abstract methods ------------------------------------------------

    /**
     * Return the {@link NamedMap} that is used as the underlying entity store.
     *
     * @return the {@link NamedMap} that is used as the underlying entity store
     */
    protected abstract NamedMap<ID, T> getMap();

    /**
     * Return the identifier of the specified entity instance.
     *
     * @param entity  the entity to get the identifier from
     *
     * @return the identifier of the specified entity instance
     */
    protected abstract ID getId(T entity);

    /**
     * Return the type of entities in this repository.
     *
     * @return the type of entities in this repository
     */
    protected abstract Class<? extends T> getEntityType();

    // ---- CRUD support ----------------------------------------------------

    /**
     * Return an entity with a given identifier.
     *
     * @param id  the entity's identifier
     *
     * @return an entity with a given identifier
     */
    public T findById(ID id)
        {
        return getMapInternal().get(id);
        }

    /**
     * Return all entities in this repository.
     *
     * @return all entities in this repository
     */
    public Collection<? extends T> findAll()
        {
        return getMapInternal().values();
        }

    /**
     * Return all entities that satisfy the specified criteria.
     *
     * @param filter  the criteria to evaluate
     *
     * @return all entities that satisfy the specified criteria
     */
    public Collection<T> findAll(Filter<?> filter)
        {
        return getMapInternal().values(filter);
        }

    /**
     * Return all entities in this repository, sorted using
     * specified {@link Comparable} attribute.
     *
     * @param orderBy  the {@link Comparable} attribute to sort the results by
     * @param <R>      the type of the extracted values
     *
     * @return all entities in this repository, sorted using
     *         specified {@link Comparable} attribute.
     */
    public <R extends Comparable<? super R>> Collection<T> findAll(ValueExtractor<? super T, ? extends R> orderBy)
        {
        return findAll(Filters.always(), Remote.comparator(orderBy));
        }

    /**
     * Return all entities that satisfy the specified criteria, sorted using
     * specified {@link Comparable} attribute.
     *
     * @param filter   the criteria to evaluate
     * @param orderBy  the {@link Comparable} attribute to sort the results by
     * @param <R>      the type of the extracted values
     *
     * @return all entities that satisfy specified criteria, sorted using
     *         specified {@link Comparable} attribute.
     */
    public <R extends Comparable<? super R>> Collection<T> findAll(Filter<?> filter, ValueExtractor<? super T, ? extends R> orderBy)
        {
        return findAll(filter, Remote.comparator(orderBy));
        }

    /**
     * Return all entities in this repository, sorted using
     * specified {@link Remote.Comparator}.
     *
     * @param orderBy  the comparator to sort the results with
     *
     * @return all entities in this repository, sorted using
     *         specified {@link Remote.Comparator}.
     */
    public Collection<T> findAll(Remote.Comparator<?> orderBy)
        {
        return getMapInternal().values(Filters.always(), orderBy);
        }

    /**
     * Return all entities that satisfy the specified criteria, sorted using
     * specified {@link Remote.Comparator}.
     *
     * @param filter   the criteria to evaluate
     * @param orderBy  the comparator to sort the results with
     *
     * @return all entities that satisfy specified criteria, sorted using
     * specified {@link Remote.Comparator}.
     */
    public Collection<T> findAll(Filter<?> filter, Remote.Comparator<?> orderBy)
        {
        return getMapInternal().values(filter, orderBy);
        }

    /**
     * Store the specified entity.
     *
     * @param entity  the entity to store
     *
     * @return the saved entity
     */
    public T save(T entity)
        {
        getMapInternal().putAll(Collections.singletonMap(getId(entity), entity));
        return entity;
        }

    /**
     * Store all specified entities as a batch.
     *
     * @param entities  the entities to store
     */
    @SuppressWarnings("unchecked")
    public void saveAll(T... entities)
        {
        saveAll(Stream.of(entities));
        }

    /**
     * Store all specified entities as a batch.
     *
     * @param colEntities  the entities to store
     */
    public void saveAll(Collection<? extends T> colEntities)
        {
        saveAll(colEntities.stream());
        }

    /**
     * Store all specified entities as a batch.
     *
     * @param strEntities  the entities to store
     */
    public void saveAll(Stream<? extends T> strEntities)
        {
        getMapInternal().putAll(strEntities.collect(Collectors.toMap(this::getId, v -> v)));
        }

    /**
     * Return the value extracted from an entity with a given identifier.
     * <p/>
     * For example, you could extract {@code Person}'s {@code name} attribute by
     * calling a getter on a remote {@code Person} entity instance:
     * <pre>
     *     people.get(ssn, Person::getName);
     * </pre>
     * Note that the actual extraction (via the invocation of the specified
     * getter method) will happen on the primary owner for the specified entity,
     * and only the extracted value will be sent over the network to the client,
     * which can significantly reduce the amount of data transferred.
     *
     * @param id         the entity's identifier
     * @param extractor  the {@link ValueExtractor} to extract value with
     * @param <R>        the type of the extracted value
     *
     * @return the extracted value
     */
    public <R> R get(ID id, ValueExtractor<? super T, ? extends R> extractor)
        {
        return getMapInternal().invoke(id, Processors.extract(extractor));
        }

    /**
     * Return a {@link Fragment} extracted from an entity with a given
     * identifier.
     * <p/>
     * For example, you could extract {@code Person}'s {@code name} and {@code
     * age} attributes by calling corresponding getters on the remote {@code
     * Person} entity instance:
     * <pre>
     *     Fragment&lt;Person> person = people.get(ssn, Person::getName, Person::getAge);
     *     System.out.println("name: " + person.get(Person::getName));
     *     System.out.println(" age: " + person.get(Person::getAge));
     * </pre>
     * You can also extract nested attributes by defining additional fragments in the
     * {@code extractors} array:
     * <pre>
     *     Fragment&lt;Person> person = people.get(ssn,
     *                                             Person::getName, Person::getAge,
     *                                             Extractors.fragment(Person::getAddress, Address::getCity, Address::getState));
     *     System.out.println(" name: " + person.get(Person::getName));
     *     System.out.println("  age: " + person.get(Person::getAge));
     *
     *     Fragment&lt;Address> address = person.getFragment(Person::getAddress);
     *     System.out.println(" city: " + address.get(Address::getCity));
     *     System.out.println("state: " + address.get(Address::getState));
     * </pre>
     * Note that the actual extraction (via the invocation of the specified
     * getter methods) will happen on the primary owner for the specified entity,
     * and only the extracted fragment will be sent over the network to the client,
     * which can significantly reduce the amount of data transferred.
     *
     * @param id          the entity's identifier
     * @param extractors  the {@link ValueExtractor}s to extract values with
     *
     * @return the extracted {@link Fragment}
     */
    @SuppressWarnings("unchecked")
    public Fragment<T> get(ID id, ValueExtractor<? super T, ?>... extractors)
        {
        return getMapInternal().invoke(id, Processors.extract(Extractors.fragment(extractors)));
        }

    /**
     * Return a map of values extracted from all entities in the repository.
     *
     * @param extractor  the {@link ValueExtractor} to extract values with
     * @param <R>        the type of the extracted values
     *
     * @return the map of extracted values, keyed by entity id
     * @see #get(Object, ValueExtractor)
     */
    public <R> Map<ID, R> getAll(ValueExtractor<? super T, ? extends R> extractor)
        {
        return getAll(Filters.always(), extractor);
        }

    /**
     * Return a map of values extracted from a set of entities with the given
     * identifiers.
     *
     * @param colIds     the entity identifiers
     * @param extractor  the {@link ValueExtractor} to extract values with
     * @param <R>        the type of the extracted values
     *
     * @return the map of extracted values, keyed by entity id
     * @see #get(Object, ValueExtractor)
     */
    public <R> Map<ID, R> getAll(Collection<? extends ID> colIds, ValueExtractor<? super T, ? extends R> extractor)
        {
        return getMapInternal().invokeAll(colIds, Processors.extract(extractor));
        }

    /**
     * Return a map of values extracted from a set of entities based on the
     * specified criteria.
     *
     * @param filter     the criteria to use to select entities for extraction
     * @param extractor  the {@link ValueExtractor} to extract values with
     * @param <R>        the type of the extracted values
     *
     * @return the map of extracted values, keyed by entity id
     * @see #get(Object, ValueExtractor)
     */
    public <R> Map<ID, R> getAll(Filter<?> filter, ValueExtractor<? super T, ? extends R> extractor)
        {
        return getMapInternal().invokeAll(filter, Processors.extract(extractor));
        }

    /**
     * Return a map of a {@link Fragment}s extracted from all entities in the
     * repository.
     *
     * @param extractors  the {@link ValueExtractor}s to extract the list of
     *                    values with
     *
     * @return the map of extracted {@link Fragment}s, keyed by entity id
     * @see #get(Object, ValueExtractor[])
     */
    @SuppressWarnings("unchecked")
    public Map<ID, Fragment<T>> getAll(ValueExtractor<? super T, ?>... extractors)
        {
        return getAll(Filters.always(), extractors);
        }

    /**
     * Return a map of {@link Fragment}s extracted from a set of entities with the
     * given identifiers.
     *
     * @param colIds      the entity identifiers
     * @param extractors  the {@link ValueExtractor}s to extract the list of
     *                    values with
     *
     * @return the map of extracted {@link Fragment}s, keyed by entity id
     * @see #get(Object, ValueExtractor[])
     */
    @SuppressWarnings("unchecked")
    public Map<ID, Fragment<T>> getAll(Collection<? extends ID> colIds, ValueExtractor<? super T, ?>... extractors)
        {
        return getMapInternal().invokeAll(colIds, Processors.extract(Extractors.fragment(extractors)));
        }

    /**
     * Return a map of {@link Fragment}s extracted from a set of entities based on
     * the specified criteria.
     *
     * @param filter      the criteria to use to select entities for extraction
     * @param extractors  the {@link ValueExtractor}s to extract the list of
     *                    values with
     *
     * @return the map of extracted {@link Fragment}s, keyed by entity id
     * @see #get(Object, ValueExtractor[])
     */
    @SuppressWarnings("unchecked")
    public Map<ID, Fragment<T>> getAll(Filter<?> filter, ValueExtractor<? super T, ?>... extractors)
        {
        return getMapInternal().invokeAll(filter, Processors.extract(Extractors.fragment(extractors)));
        }

    /**
     * Update an entity using specified updater and the new value.
     * <p/>
     * For example, you could update {@code Person}'s {@code age} attribute by
     * calling a setter on a remote {@code Person} entity instance:
     * <pre>
     *     people.update(ssn, Person::setAge, 21);
     * </pre>
     * Note that the actual update (via the invocation of the specified setter
     * method) will happen on the primary owner for the specified entity, and
     * the updater will have exclusive access to an entity during the
     * execution.
     *
     * @param id       the entity's identifier
     * @param updater  the updater function to use
     * @param value    the value to update entity with, which will be passed as
     *                 an argument to the updater function
     * @param <U>      the type of value to update
     */
    public <U> void update(ID id,
                           ValueUpdater<? super T, ? super U> updater,
                           U value)
        {
        update(id, updater, value, null);
        }

    /**
     * Update an entity using specified updater and the new value, and optional
     * {@link EntityFactory} that will be used to create entity instance if it
     * doesn't already exist in the repository.
     * <p/>
     * For example, you could update {@code Person}'s {@code age} attribute by
     * calling a setter on a remote {@code Person} entity instance:
     * <pre>
     *     people.update(ssn, Person::setAge, 21, Person::new);
     * </pre>
     * If the person with the specified identifier does not exist, the {@link
     * EntityFactory} will be used to create a new instance. In the example
     * above, it will invoke a constructor on the {@code Person} class that
     * takes identifier as an argument.
     * <p/>
     * Note that the actual update (via the invocation of the specified setter
     * method) will happen on the primary owner for the specified entity, and
     * the updater will have exclusive access to an entity during the
     * execution.
     *
     * @param id       the entity's identifier
     * @param updater  the updater function to use
     * @param value    the value to update entity with, which will be passed as
     *                 an argument to the updater function
     * @param <U>      the type of value to update
     * @param factory  the entity factory to use to create new entity instance
     */
    public <U> void update(ID id,
                           ValueUpdater<? super T, ? super U> updater,
                           U value,
                           EntityFactory<? super ID, ? extends T> factory)
        {
        getMapInternal().invoke(id, updaterProcessor(updater, value, factory));
        }

    /**
     * Update an entity using specified updater function.
     * <p/>
     * For example, you could increment {@code Person}'s {@code age} attribute
     * and return the updated {@code Person} entity:
     * <pre>
     *    people.update(ssn, person ->
     *        {
     *        person.setAge(person.getAge() + 1);
     *        return person;
     *        });
     * </pre>
     * This variant of the {@code update} method offers ultimate flexibility, as
     * it allows you to return any value you want as the result of the
     * invocation, at the cost of typically slightly more complex logic at the
     * call site.
     * <p/>
     * Note that the actual update (via the evaluation of the specified
     * function) will happen on the primary owner for the specified entity, and
     * the updater will have exclusive access to an entity during the
     * execution.
     *
     * @param id       the entity's identifier
     * @param updater  the updater function to use
     * @param <R>      the type of return value of the updater function
     *
     * @return the result of updater function evaluation
     */
    public <R> R update(ID id,
                        Remote.Function<? super T, ? extends R> updater)
        {
        return update(id, updater, null);
        }

    /**
     * Update an entity using specified updater function, and optional {@link
     * EntityFactory} that will be used to create entity instance if it doesn't
     * already exist in the repository.
     * <p/>
     * For example, you could increment {@code Person}'s {@code age} attribute
     * and return the updated {@code Person} entity:
     * <pre>
     *    people.update(ssn, person ->
     *        {
     *        person.setAge(person.getAge() + 1);
     *        return person;
     *        }, Person::new);
     * </pre>
     * If the person with the specified identifier does not exist, the {@link
     * EntityFactory} will be used to create a new instance. In the example
     * above, it will invoke a constructor on the {@code Person} class that
     * takes identifier as an argument.
     * <p/>
     * This variant of the {@code update} method offers ultimate flexibility, as
     * it allows you to return any value you want as the result of the
     * invocation, at the cost of typically slightly more complex logic at the
     * call site.
     * <p/>
     * Note that the actual update (via the evaluation of the specified
     * function) will happen on the primary owner for the specified entity, and
     * the updater will have exclusive access to an entity during the
     * execution.
     *
     * @param id       the entity's identifier
     * @param updater  the updater function to use
     * @param factory  the entity factory to use to create new entity instance
     * @param <R>      the type of return value of the updater function
     *
     * @return the result of updater function evaluation
     */
    public <R> R update(ID id,
                        Remote.Function<? super T, ? extends R> updater,
                        EntityFactory<? super ID, ? extends T> factory)
        {
        return getMapInternal().invoke(id, updateFunctionProcessor(updater, factory));
        }

    /**
     * Update an entity using specified updater and the new value.
     * <p/>
     * Unlike {@link #update(Object, ValueUpdater, Object)}, which doesn't
     * return anything, this method is typically used to invoke "fluent" methods
     * on the target entity that return entity itself (although they are free to
     * return any value they want).
     * <p/>
     * For example, you could use it to add an item to the {@code ShoppingCart}
     * entity and return the updated {@code ShoppingCart} instance in a single
     * call:
     * <pre>
     *     Item item = ...
     *     ShoppingCart cart = carts.update(cartId, ShoppingCart::addItem, item);
     * </pre>
     * Note that the actual update (via the invocation of the specified setter
     * method) will happen on the primary owner for the specified entity, and
     * the updater will have exclusive access to an entity during the
     * execution.
     *
     * @param id       the entity's identifier
     * @param updater  the updater function to use
     * @param value    the value to update entity with, which will be passed as
     *                 an argument to the updater function
     * @param <U>      the type of value to update
     * @param <R>      the type of return value of the updater function
     *
     * @return the result of updater function evaluation
     */
    public <U, R> R update(ID id,
                           Remote.BiFunction<? super T, ? super U, ? extends R> updater,
                           U value)
        {
        return update(id, updater, value, null);
        }

    /**
     * Update an entity using specified updater function, and optional {@link
     * EntityFactory} that will be used to create entity instance if it doesn't
     * already exist in the repository.
     * <p/>
     * Unlike {@link #update(Object, ValueUpdater, Object)}, which doesn't
     * return anything, this method is typically used to invoke "fluent" methods
     * on the target entity that return entity itself (although they are free to
     * return any value they want).
     * <p/>
     * For example, you could use it to add an item to the {@code ShoppingCart}
     * entity and return the updated {@code ShoppingCart} instance in a single
     * call:
     * <pre>
     *     Item item = ...
     *     ShoppingCart cart = carts.update(cartId, ShoppingCart::addItem, item, ShoppingCart::new);
     * </pre>
     * If the cart with the specified identifier does not exist, the specified
     * {@link EntityFactory} will be used to create a new instance. In the
     * example above, it will invoke a constructor on the {@code ShoppingCart}
     * class that takes identifier as an argument.
     * <p/>
     * Note that the actual update (via the evaluation of the specified
     * function) will happen on the primary owner for the specified entity, and
     * the updater will have exclusive access to an entity during the
     * execution.
     *
     * @param id       the entity's identifier
     * @param updater  the updater function to use
     * @param value    the value to update entity with, which will be passed as
     *                 an argument to the updater function
     * @param factory  the entity factory to use to create new entity instance
     * @param <U>      the type of value to update
     * @param <R>      the type of return value of the updater function
     *
     * @return the result of updater function evaluation
     */
    public <U, R> R update(ID id,
                           Remote.BiFunction<? super T, ? super U, ? extends R> updater,
                           U value,
                           EntityFactory<? super ID, ? extends T> factory)
        {
        return getMapInternal().invoke(id, updateBiFunctionProcessor(updater, value, factory));
        }

    /**
     * Update multiple entities using specified updater and the new value.
     *
     * @param filter   the criteria to use to select entities to update
     * @param updater  the updater function to use
     * @param value    the value to update each entity with, which will be passed
     *                 as an argument to the updater function
     * @param <U>      the type of value to update
     */
    public <U> void updateAll(Filter<?> filter,
                              ValueUpdater<? super T, ? super U> updater,
                              U value)
        {
        getMapInternal().invokeAll(filter, updaterProcessor(updater, value, null));
        }

    /**
     * Update multiple entities using specified updater function.
     *
     * @param filter   the criteria to use to select entities to update
     * @param updater  the updater function to use
     * @param <R>      the type of return value of the updater function
     *
     * @return a map of updater function results, keyed by entity id
     */
    public <R> Map<ID, R> updateAll(Filter<?> filter,
                                    Remote.Function<? super T, ? extends R> updater)
        {
        return getMapInternal().invokeAll(filter, updateFunctionProcessor(updater, null));
        }

    /**
     * Update multiple entities using specified updater and the new value.
     *
     * @param filter   the criteria to use to select entities to update
     * @param updater  the updater function to use
     * @param value    the value to update each entity with, which will be passed
     *                 as an argument to the updater function
     * @param <U>      the type of value to update
     * @param <R>      the type of return value of the updater function
     *
     * @return a map of updater function results, keyed by entity id
     */
    public <U, R> Map<ID, R> updateAll(Filter<?> filter,
                                       Remote.BiFunction<? super T, ? super U, ? extends R> updater,
                                       U value)
        {
        return getMapInternal().invokeAll(filter, updateBiFunctionProcessor(updater, value, null));
        }

    /**
     * Remove entity with a specified identifier.
     *
     * @param id  the identifier of an entity to remove, if present
     *
     * @return {@code true} if this repository contained the specified entity
     */
    public boolean removeById(ID id)
        {
        return getMapInternal().invoke(id, Processors.remove());
        }

    /**
     * Remove entity with a specified identifier.
     *
     * @param id       the identifier of an entity to remove
     * @param fReturn  the flag specifying whether to return removed entity
     *
     * @return removed entity, iff {@code fReturn == true}; {@code null}
     * otherwise
     */
    public T removeById(ID id, boolean fReturn)
        {
        return getMapInternal().invoke(id, Processors.remove(fReturn));
        }

    /**
     * Remove specified entity.
     *
     * @param entity  the entity to remove
     *
     * @return {@code true} if this repository contained the specified entity
     */
    public boolean remove(T entity)
        {
        return removeById(getId(entity));
        }

    /**
     * Remove specified entity.
     *
     * @param entity   the entity to remove
     * @param fReturn  the flag specifying whether to return removed entity
     *
     * @return removed entity, iff {@code fReturn == true}; {@code null}
     * otherwise
     */
    public T remove(T entity, boolean fReturn)
        {
        return removeById(getId(entity), fReturn);
        }

    /**
     * Remove entities with the specified identifiers.
     *
     * @param colIds  the identifiers of the entities to remove
     *
     * @return {@code true} if this repository changed as a result of the call
     */
    public boolean removeAllById(Collection<? extends ID> colIds)
        {
        Map<ID, Boolean> mapResults = getMapInternal().invokeAll(colIds, Processors.remove());
        for (boolean fResult : mapResults.values())
            {
            if (fResult)
                {
                return true;
                }
            }

        return false;
        }

    /**
     * Remove entities with the specified identifiers.
     *
     * @param colIds   the identifiers of the entities to remove
     * @param fReturn  the flag specifying whether to return removed entity
     *
     * @return the map of removed entity identifiers as keys, and the removed
     * entities as values iff {@code fReturn == true}; {@code null} otherwise
     */
    public Map<ID, T> removeAllById(Collection<? extends ID> colIds, boolean fReturn)
        {
        return getMapInternal().invokeAll(colIds, Processors.remove(fReturn));
        }

    /**
     * Remove specified entities.
     *
     * @param entities  the entities to remove
     *
     * @return {@code true} if this repository changed as a result of the call
     */
    @SuppressWarnings("unchecked")
    public boolean removeAll(T... entities)
        {
        return removeAll(Arrays.asList(entities));
        }

    /**
     * Remove specified entities.
     *
     * @param colEntities  the entities to remove
     *
     * @return {@code true} if this repository changed as a result of the call
     */
    public boolean removeAll(Collection<? extends T> colEntities)
        {
        return removeAll(colEntities.stream());
        }

    /**
     * Remove specified entities.
     *
     * @param colEntities  the entities to remove
     * @param fReturn      the flag specifying whether to return removed entity
     *
     * @return the map of removed entity identifiers as keys, and the removed
     * entities as values iff {@code fReturn == true}; {@code null} otherwise
     */
    public Map<ID, T> removeAll(Collection<? extends T> colEntities, boolean fReturn)
        {
        return removeAll(colEntities.stream(), fReturn);
        }

    /**
     * Remove specified entities.
     *
     * @param strEntities  the entities to remove
     *
     * @return {@code true} if this repository changed as a result of the call
     */
    public boolean removeAll(Stream<? extends T> strEntities)
        {
        return removeAllById(strEntities.map(this::getId).collect(Collectors.toSet()));
        }

    /**
     * Remove specified entities.
     *
     * @param strEntities  the entities to remove
     * @param fReturn      the flag specifying whether to return removed entity
     *
     * @return the map of removed entity identifiers as keys, and the removed
     * entities as values iff {@code fReturn == true}; {@code null} otherwise
     */
    public Map<ID, T> removeAll(Stream<? extends T> strEntities, boolean fReturn)
        {
        return removeAllById(strEntities.map(this::getId).collect(Collectors.toSet()), fReturn);
        }

    /**
     * Remove all entities based on the specified criteria.
     *
     * @param filter  the criteria that should be used to select entities to
     *                remove
     *
     * @return {@code true} if this repository changed as a result of the call
     */
    public boolean removeAll(Filter<?> filter)
        {
        Map<ID, Boolean> mapResults = getMapInternal().invokeAll(filter, Processors.remove());
        for (boolean fResult : mapResults.values())
            {
            if (fResult)
                {
                return true;
                }
            }

        return false;
        }

    /**
     * Remove all entities based on the specified criteria.
     *
     * @param filter   the criteria that should be used to select entities to
     *                 remove
     * @param fReturn  the flag specifying whether to return removed entity
     *
     * @return the map of removed entity identifiers as keys, and the removed
     * entities as values iff {@code fReturn == true}; {@code null} otherwise
     */
    public Map<ID, T> removeAll(Filter<?> filter, boolean fReturn)
        {
        return getMapInternal().invokeAll(filter, Processors.remove(fReturn));
        }

    // ---- Stream API support ----------------------------------------------

    /**
     * Return a stream of all entities in this repository.
     *
     * @return a stream of all entities in this repository
     */
    public RemoteStream<T> stream()
        {
        return getMapInternal().stream().map(Map.Entry::getValue);
        }

    /**
     * Return a stream of entities with the specified identifiers.
     *
     * @param colIds  the identifiers of the entities to include in the
     *                returned stream
     *
     * @return a stream of entities for the specified identifiers
     */
    public RemoteStream<T> stream(Collection<? extends ID> colIds)
        {
        return getMapInternal().stream(colIds).map(Map.Entry::getValue);
        }

    /**
     * Return a stream of all entities in this repository that satisfy the
     * specified criteria.
     *
     * @param filter  the criteria an entity must satisfy in order to be
     *                included in the returned stream
     *
     * @return a stream of entities that satisfy the specified criteria
     */
    public RemoteStream<T> stream(Filter<?> filter)
        {
        return getMapInternal().stream(filter).map(Map.Entry::getValue);
        }

    // ---- aggregation support ---------------------------------------------

    /**
     * Return the number of entities in this repository.
     *
     * @return the number of entities in this repository
     */
    public long count()
        {
        return getMapInternal().aggregate(Aggregators.count());
        }

    /**
     * Return the number of entities in this repository that satisfy specified
     * filter.
     *
     * @param filter  the filter to evaluate
     *
     * @return the number of entities in this repository that satisfy specified
     *         filter
     */
    public long count(Filter<?> filter)
        {
        return getMapInternal().aggregate(filter, Aggregators.count());
        }

    /**
     * Return the maximum value of the specified function.
     *
     * @param extractor  the function to determine the maximum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     *
     * @return the maximum value of the specified function
     */
    public int max(Remote.ToIntFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(Aggregators.max(extractor)).intValue();
        }

    /**
     * Return the maximum value of the specified function.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the function to determine the maximum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     *
     * @return the maximum value of the specified function
     */
    public int max(Filter<?> filter, Remote.ToIntFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(filter, Aggregators.max(extractor)).intValue();
        }

    /**
     * Return the maximum value of the specified function.
     *
     * @param extractor  the function to determine the maximum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     *
     * @return the maximum value of the specified function
     */
    public long max(Remote.ToLongFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(Aggregators.max(extractor));
        }

    /**
     * Return the maximum value of the specified function.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the function to determine the maximum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     *
     * @return the maximum value of the specified function
     */
    public long max(Filter<?> filter, Remote.ToLongFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(filter, Aggregators.max(extractor));
        }

    /**
     * Return the maximum value of the specified function.
     *
     * @param extractor  the function to determine the maximum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getWeight}
     *
     * @return the maximum value of the specified function
     */
    public double max(Remote.ToDoubleFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(Aggregators.max(extractor));
        }

    /**
     * Return the maximum value of the specified function.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the function to determine the maximum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getWeight}
     *
     * @return the maximum value of the specified function
     */
    public double max(Filter<?> filter, Remote.ToDoubleFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(filter, Aggregators.max(extractor));
        }

    /**
     * Return the maximum value of the specified function.
     *
     * @param extractor  the function to determine the maximum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getSalary}
     *
     * @return the maximum value of the specified function
     */
    public BigDecimal max(Remote.ToBigDecimalFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(Aggregators.max(extractor));
        }

    /**
     * Return the maximum value of the specified function.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the function to determine the maximum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getSalary}
     *
     * @return the maximum value of the specified function
     */
    public BigDecimal max(Filter<?> filter, Remote.ToBigDecimalFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(filter, Aggregators.max(extractor));
        }

    /**
     * Return the maximum value of the specified function.
     *
     * @param extractor  the function to determine the maximum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getName}
     * @param <R>        the type of the extracted value
     *
     * @return the maximum value of the specified function
     */
    public <R extends Comparable<? super R>> R max(Remote.ToComparableFunction<? super T, R> extractor)
        {
        return getMapInternal().aggregate(Aggregators.max(extractor));
        }

    /**
     * Return the maximum value of the specified function.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the function to determine the maximum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getName}
     * @param <R>        the type of the extracted value
     *
     * @return the maximum value of the specified function
     */
    public <R extends Comparable<? super R>> R max(Filter<?> filter, Remote.ToComparableFunction<? super T, R> extractor)
        {
        return getMapInternal().aggregate(filter, Aggregators.max(extractor));
        }

    /**
     * Return the entity with the maximum value of the specified function.
     *
     * @param extractor  the function to determine the maximum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     * @param <R>        the type of the extracted value
     *
     * @return the entity with the maximum value of the specified function
     */
    public <R extends Comparable<? super R>> Optional<T> maxBy(ValueExtractor<? super T, ? extends R> extractor)
        {
        return stream().reduce(Remote.BinaryOperator.maxBy(comparing(extractor)));
        }

    /**
     * Return the entity with the maximum value of the specified function.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the function to determine the maximum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     * @param <R>        the type of the extracted value
     *
     * @return the entity with the maximum value of the specified function
     */
    public <R extends Comparable<? super R>> Optional<T> maxBy(Filter<?> filter, ValueExtractor<? super T, ? extends R> extractor)
        {
        return stream(filter).reduce(Remote.BinaryOperator.maxBy(comparing(extractor)));
        }

    /**
     * Return the minimum value of the specified function.
     *
     * @param extractor  the function to determine the minimum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     *
     * @return the minimum value of the specified function
     */
    public int min(Remote.ToIntFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(Aggregators.min(extractor)).intValue();
        }

    /**
     * Return the minimum value of the specified function.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the function to determine the minimum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     *
     * @return the minimum value of the specified function
     */
    public int min(Filter<?> filter, Remote.ToIntFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(filter, Aggregators.min(extractor)).intValue();
        }

    /**
     * Return the minimum value of the specified function.
     *
     * @param extractor  the function to determine the minimum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     *
     * @return the minimum value of the specified function
     */
    public long min(Remote.ToLongFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(Aggregators.min(extractor));
        }

    /**
     * Return the minimum value of the specified function.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the function to determine the minimum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     *
     * @return the minimum value of the specified function
     */
    public long min(Filter<?> filter, Remote.ToLongFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(filter, Aggregators.min(extractor));
        }

    /**
     * Return the minimum value of the specified function.
     *
     * @param extractor  the function to determine the minimum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getWeight}
     *
     * @return the minimum value of the specified function
     */
    public double min(Remote.ToDoubleFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(Aggregators.min(extractor));
        }

    /**
     * Return the minimum value of the specified function.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the function to determine the minimum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getWeight}
     *
     * @return the minimum value of the specified function
     */
    public double min(Filter<?> filter, Remote.ToDoubleFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(filter, Aggregators.min(extractor));
        }

    /**
     * Return the minimum value of the specified function.
     *
     * @param extractor  the function to determine the minimum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getSalary}
     *
     * @return the minimum value of the specified function
     */
    public BigDecimal min(Remote.ToBigDecimalFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(Aggregators.min(extractor));
        }

    /**
     * Return the minimum value of the specified function.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the function to determine the minimum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getSalary}
     *
     * @return the minimum value of the specified function
     */
    public BigDecimal min(Filter<?> filter, Remote.ToBigDecimalFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(filter, Aggregators.min(extractor));
        }

    /**
     * Return the minimum value of the specified function.
     *
     * @param extractor  the function to determine the minimum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getName}
     * @param <R>        the type of the extracted value
     *
     * @return the minimum value of the specified function
     */
    public <R extends Comparable<? super R>> R min(Remote.ToComparableFunction<? super T, R> extractor)
        {
        return getMapInternal().aggregate(Aggregators.min(extractor));
        }

    /**
     * Return the minimum value of the specified function.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the function to determine the minimum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getName}
     * @param <R>        the type of the extracted value
     *
     * @return the minimum value of the specified function
     */
    public <R extends Comparable<? super R>> R min(Filter<?> filter, Remote.ToComparableFunction<? super T, R> extractor)
        {
        return getMapInternal().aggregate(filter, Aggregators.min(extractor));
        }

    /**
     * Return the entity with the minimum value of the specified function.
     *
     * @param extractor  the function to determine the minimum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     * @param <R>        the type of the extracted value
     *
     * @return the entity with the minimum value of the specified function
     */
    public <R extends Comparable<? super R>> Optional<T> minBy(ValueExtractor<? super T, ? extends R> extractor)
        {
        return stream().reduce(Remote.BinaryOperator.minBy(comparing(extractor)));
        }

    /**
     * Return the entity with the minimum value of the specified function.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the function to determine the minimum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     * @param <R>        the type of the extracted value
     *
     * @return the entity with the minimum value of the specified function
     */
    public <R extends Comparable<? super R>> Optional<T> minBy(Filter<?> filter, ValueExtractor<? super T, ? extends R> extractor)
        {
        return stream(filter).reduce(Remote.BinaryOperator.minBy(comparing(extractor)));
        }

    /**
     * Return the sum of the specified function.
     *
     * @param extractor  the function to sum;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     *
     * @return the sum of the specified function
     */
    public long sum(Remote.ToIntFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(Aggregators.sum(extractor));
        }

    /**
     * Return the sum of the specified function.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the function to sum;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     *
     * @return the sum of the specified function
     */
    public long sum(Filter<?> filter, Remote.ToIntFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(filter, Aggregators.sum(extractor));
        }

    /**
     * Return the sum of the specified function.
     *
     * @param extractor  the function to sum;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     *
     * @return the sum of the specified function
     */
    public long sum(Remote.ToLongFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(Aggregators.sum(extractor));
        }

    /**
     * Return the sum of the specified function.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the function to sum;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     *
     * @return the sum of the specified function
     */
    public long sum(Filter<?> filter, Remote.ToLongFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(filter, Aggregators.sum(extractor));
        }

    /**
     * Return the sum of the specified function.
     *
     * @param extractor  the function to sum;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getWeight}
     *
     * @return the sum of the specified function
     */
    public double sum(Remote.ToDoubleFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(Aggregators.sum(extractor));
        }

    /**
     * Return the sum of the specified function.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the function to sum;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getWeight}
     *
     * @return the sum of the specified function
     */
    public double sum(Filter<?> filter, Remote.ToDoubleFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(filter, Aggregators.sum(extractor));
        }

    /**
     * Return the sum of the specified function.
     *
     * @param extractor  the function to sum;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getSalary}
     *
     * @return the sum of the specified function
     */
    public BigDecimal sum(Remote.ToBigDecimalFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(Aggregators.sum(extractor));
        }

    /**
     * Return the sum of the specified function.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the function to sum;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getSalary}
     *
     * @return the sum of the specified function
     */
    public BigDecimal sum(Filter<?> filter, Remote.ToBigDecimalFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(filter, Aggregators.sum(extractor));
        }

    /**
     * Return the average of the specified function.
     *
     * @param extractor  the function to average;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     *
     * @return the average of the specified function
     */
    public double average(Remote.ToIntFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(Aggregators.average(extractor));
        }

    /**
     * Return the average of the specified function.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the function to average;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     *
     * @return the average of the specified function
     */
    public double average(Filter<?> filter, Remote.ToIntFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(filter, Aggregators.average(extractor));
        }

    /**
     * Return the average of the specified function.
     *
     * @param extractor  the function to average;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     *
     * @return the average of the specified function
     */
    public double average(Remote.ToLongFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(Aggregators.average(extractor));
        }

    /**
     * Return the average of the specified function.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the function to average;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     *
     * @return the average of the specified function
     */
    public double average(Filter<?> filter, Remote.ToLongFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(filter, Aggregators.average(extractor));
        }

    /**
     * Return the average of the specified function.
     *
     * @param extractor  the function to average;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getWeight}
     *
     * @return the average of the specified function
     */
    public double average(Remote.ToDoubleFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(Aggregators.average(extractor));
        }

    /**
     * Return the average of the specified function.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the function to average;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getWeight}
     *
     * @return the average of the specified function
     */
    public double average(Filter<?> filter, Remote.ToDoubleFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(filter, Aggregators.average(extractor));
        }

    /**
     * Return the average of the specified function.
     *
     * @param extractor  the function to average;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getSalary}
     *
     * @return the average of the specified function
     */
    public BigDecimal average(Remote.ToBigDecimalFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(Aggregators.average(extractor));
        }

    /**
     * Return the average of the specified function.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the function to average;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getSalary}
     *
     * @return the average of the specified function
     */
    public BigDecimal average(Filter<?> filter, Remote.ToBigDecimalFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(filter, Aggregators.average(extractor));
        }

    /**
     * Return the set of distinct values for the specified extractor.
     *
     * @param extractor  the extractor to get a value from;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getName}
     * @param <R>        the type of extracted values
     *
     * @return the set of distinct values for the specified extractor
     */
    public <R> Collection<? extends R> distinct(ValueExtractor<? super T, ? extends R> extractor)
        {
        return getMapInternal().aggregate(Aggregators.distinctValues(extractor));
        }

    /**
     * Return the set of distinct values for the specified extractor.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the extractor to get a value from;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getName}
     * @param <R>        the type of extracted values
     *
     * @return the set of distinct values for the specified extractor
     */
    public <R> Collection<? extends R> distinct(Filter<?> filter, ValueExtractor<? super T, ? extends R> extractor)
        {
        return getMapInternal().aggregate(filter, Aggregators.distinctValues(extractor));
        }

    /**
     * Return the grouping of entities by the specified extractor.
     *
     * @param extractor  the extractor to get a grouping value from;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getGender}
     * @param <K>        the type of extracted grouping keys
     *
     * @return the the grouping of entities by the specified extractor; the keys
     *         in the returned map will be distinct values extracted by the
     *         specified {@code extractor}, and the values will be sets of entities
     *         that match each extracted key
     */
    public <K> Map<K, Set<T>> groupBy(ValueExtractor<? super T, ? extends K> extractor)
        {
        return stream().collect(groupingBy(extractor, RemoteCollectors.toSet()));
        }

    /**
     * Return the grouping of entities by the specified extractor, ordered by
     * the specified attribute within each group.
     *
     * @param extractor  the extractor to get a grouping value from;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getGender}
     * @param orderBy    the {@link Remote.Comparator} to sort the results
     *                   within each group by
     * @param <K>        the type of extracted grouping keys
     *
     * @return the the grouping of entities by the specified extractor; the keys
     *         in the returned map will be distinct values extracted by the
     *         specified {@code extractor}, and the values will be sorted sets
     *         of entities that match each extracted key
     */
    public <K> Map<K, SortedSet<T>> groupBy(ValueExtractor<? super T, ? extends K> extractor,
                                            Remote.Comparator<? super T> orderBy)
        {
        return stream().collect(groupingBy(extractor, RemoteCollectors.toSortedSet(orderBy)));
        }

    /**
     * Return the grouping of entities by the specified extractor.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the extractor to get a grouping value from;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getGender}
     * @param <K>        the type of extracted grouping keys
     *
     * @return the the grouping of entities by the specified extractor; the keys
     *         in the returned map will be distinct values extracted by the
     *         specified {@code extractor}, and the values will be sets of entities
     *         that match each extracted key
     */
    public <K> Map<K, Set<T>> groupBy(Filter<?> filter, ValueExtractor<? super T, ? extends K> extractor)
        {
        return stream(filter).collect(groupingBy(extractor, RemoteCollectors.toSet()));
        }

    /**
     * Return the grouping of entities by the specified extractor, ordered by
     * the specified attribute within each group.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the extractor to get a grouping value from;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getGender}
     * @param orderBy    the {@link Remote.Comparator} to sort the results
     *                   within each group by
     * @param <K>        the type of extracted grouping keys
     *
     * @return the the grouping of entities by the specified extractor; the keys
     *         in the returned map will be distinct values extracted by the
     *         specified {@code extractor}, and the values will be sorted sets
     *         of entities that match each extracted key
     */
    public <K> Map<K, SortedSet<T>> groupBy(Filter<?> filter,
                                            ValueExtractor<? super T, ? extends K> extractor,
                                            Remote.Comparator<? super T> orderBy)
        {
        return stream(filter).collect(groupingBy(extractor, RemoteCollectors.toSortedSet(orderBy)));
        }

    /**
     * Return the grouping of entities by the specified extractor.
     *
     * @param extractor  the extractor to get a grouping value from;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getGender}
     * @param collector  the {@link RemoteCollector} to apply to grouped entities
     * @param <K>        the type of extracted grouping keys
     * @param <A>        the type of collector's accumulator
     * @param <R>        the type of collector's result
     *
     * @return the the grouping of entities by the specified extractor; the keys
     *         in the returned map will be distinct values extracted by the
     *         specified {@code extractor}, and the values will be results of
     *         the specified {@code collector} for each group
     *
     * @see RemoteCollectors
     */
    public <K, A, R> Map<K, R> groupBy(ValueExtractor<? super T, ? extends K> extractor,
                                       RemoteCollector<? super T, A, R> collector)
        {
        return stream().collect(groupingBy(extractor, collector));
        }

    /**
     * Return the grouping of entities by the specified extractor.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the extractor to get a grouping value from;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getGender}
     * @param collector  the {@link RemoteCollector} to apply to grouped entities
     * @param <K>        the type of extracted grouping keys
     * @param <A>        the type of collector's accumulator
     * @param <R>        the type of collector's result
     *
     * @return the the grouping of entities by the specified extractor; the keys
     *         in the returned map will be distinct values extracted by the
     *         specified {@code extractor}, and the values will be results of
     *         the specified {@code collector} for each group
     *
     * @see RemoteCollectors
     */
    public <K, A, R> Map<K, R> groupBy(Filter<?> filter,
                                       ValueExtractor<? super T, ? extends K> extractor,
                                       RemoteCollector<? super T, A, R> collector)
        {
        return stream(filter).collect(groupingBy(extractor, collector));
        }

    /**
     * Return the grouping of entities by the specified extractor.
     *
     * @param extractor   the extractor to get a grouping value from;
     *                    typically a method reference on the entity class,
     *                    such as {@code Person::getGender}
     * @param mapFactory  the supplier to use to create result {@code Map}
     * @param collector   the {@link RemoteCollector} to apply to grouped entities
     * @param <K>         the type of extracted grouping keys
     * @param <A>         the type of collector's accumulator
     * @param <R>         the type of collector's result
     * @param <M>         the type of result {@code Map}
     *
     * @return the the grouping of entities by the specified extractor; the keys
     *         in the returned map will be distinct values extracted by the
     *         specified {@code extractor}, and the values will be results of
     *         the specified {@code collector} for each group
     *
     * @see RemoteCollectors
     */
    public <K, A, R, M extends Map<K, R>> M groupBy(ValueExtractor<? super T, ? extends K> extractor,
                                                    Remote.Supplier<M> mapFactory,
                                                    RemoteCollector<? super T, A, R> collector)
        {
        return stream().collect(groupingBy(extractor, mapFactory, collector));
        }

    /**
     * Return the grouping of entities by the specified extractor.
     *
     * @param filter      the entity selection criteria
     * @param extractor   the extractor to get a grouping value from;
     *                    typically a method reference on the entity class,
     *                    such as {@code Person::getGender}
     * @param mapFactory  the supplier to use to create result {@code Map}
     * @param collector   the {@link RemoteCollector} to apply to grouped entities
     * @param <K>         the type of extracted grouping keys
     * @param <A>         the type of collector's accumulator
     * @param <R>         the type of collector's result
     * @param <M>         the type of result {@code Map}
     *
     * @return the the grouping of entities by the specified extractor; the keys
     *         in the returned map will be distinct values extracted by the
     *         specified {@code extractor}, and the values will be results of
     *         the specified {@code collector} for each group
     *
     * @see RemoteCollectors
     */
    public <K, A, R, M extends Map<K, R>> M groupBy(Filter<?> filter,
                                                    ValueExtractor<? super T, ? extends K> extractor,
                                                    Remote.Supplier<M> mapFactory,
                                                    RemoteCollector<? super T, A, R> collector)
        {
        return stream(filter).collect(groupingBy(extractor, mapFactory, collector));
        }

    /**
     * Return the top N highest values for the specified extractor.
     *
     * @param extractor  the extractor to get the values to compare with
     * @param cResults   the number of highest values to return
     * @param <R>        the type of returned values
     *
     * @return the top N highest values for the specified extractor
     */
    @SuppressWarnings("unchecked")
    public <R extends Comparable<? super R>> List<R> top(ValueExtractor<? super T, ? extends R> extractor, int cResults)
        {
        Object[] aoResults = getMapInternal().aggregate(Aggregators.topN(extractor, cResults));
        return Stream.of(aoResults).map(o -> (R) o).collect(Collectors.toList());
        }

    /**
     * Return the top N highest values for the specified extractor.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the extractor to get the values to compare with
     * @param cResults   the number of highest values to return
     * @param <R>        the type of returned values
     *
     * @return the top N highest values for the specified extractor
     */
    @SuppressWarnings("unchecked")
    public <R extends Comparable<? super R>> List<R> top(Filter<?> filter, ValueExtractor<? super T, ? extends R> extractor, int cResults)
        {
        Object[] aoResults = getMapInternal().aggregate(filter, Aggregators.topN(extractor, cResults));
        return Stream.of(aoResults).map(o -> (R) o).collect(Collectors.toList());
        }

    /**
     * Return the top N highest values for the specified extractor.
     *
     * @param extractor   the extractor to get the values to compare with
     * @param comparator  the comparator to use when comparing extracted values
     * @param cResults    the number of highest values to return
     * @param <R>         the type of returned values
     *
     * @return the top N highest values for the specified extractor
     */
    public <R> List<R> top(ValueExtractor<? super T, ? extends R> extractor, Remote.Comparator<? super R> comparator, int cResults)
        {
        R[] results = getMapInternal().aggregate(Aggregators.topN(extractor, comparator, cResults));
        return Arrays.asList(results);
        }

    /**
     * Return the top N highest values for the specified extractor.
     *
     * @param filter      the entity selection criteria
     * @param extractor   the extractor to get the values to compare with
     * @param comparator  the comparator to use when comparing extracted values
     * @param cResults    the number of highest values to return
     * @param <R>         the type of returned values
     *
     * @return the top N highest values for the specified extractor
     */
    public <R> List<R> top(Filter<?> filter, ValueExtractor<? super T, ? extends R> extractor, Remote.Comparator<? super R> comparator, int cResults)
        {
        R[] results = getMapInternal().aggregate(filter, Aggregators.topN(extractor, comparator, cResults));
        return Arrays.asList(results);
        }

    /**
     * Return the top N entities with the highest values for the specified extractor.
     *
     * @param extractor  the extractor to get the values to compare with
     * @param cResults   the number of highest values to return
     * @param <R>        the type of values used for comparison
     *
     * @return the top N entities with the highest values for the specified extractor
     */
    public <R extends Comparable<? super R>> List<T> topBy(ValueExtractor<? super T, ? extends R> extractor, int cResults)
        {
        T[] results = getMapInternal().aggregate(Aggregators.topN(ValueExtractor.identity(), new ExtractorComparator<>(extractor), cResults));
        return Arrays.asList(results);
        }

    /**
     * Return the top N entities with the highest values for the specified extractor.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the extractor to get the values to compare with
     * @param cResults   the number of highest values to return
     * @param <R>        the type of values used for comparison
     *
     * @return the top N entities with the highest values for the specified extractor
     */
    public <R extends Comparable<? super R>> List<T> topBy(Filter<?> filter, ValueExtractor<? super T, ? extends R> extractor, int cResults)
        {
        T[] results = getMapInternal().aggregate(filter, Aggregators.topN(ValueExtractor.identity(), new ExtractorComparator<>(extractor), cResults));
        return Arrays.asList(results);
        }

    /**
     * Return the top N entities with the highest values for the specified extractor.
     *
     * @param comparator  the comparator to use when comparing extracted values
     * @param cResults    the number of highest values to return
     *
     * @return the top N entities with the highest values for the specified extractor
     */
    public List<T> topBy(Remote.Comparator<? super T> comparator, int cResults)
        {
        T[] results = getMapInternal().aggregate(Aggregators.topN(ValueExtractor.identity(), comparator, cResults));
        return Arrays.asList(results);
        }

    /**
     * Return the top N entities with the highest values for the specified extractor.
     *
     * @param filter      the entity selection criteria
     * @param comparator  the comparator to use when comparing extracted values
     * @param cResults    the number of highest values to return
     *
     * @return the top N entities with the highest values for the specified extractor
     */
    public List<T> topBy(Filter<?> filter, Remote.Comparator<? super T> comparator, int cResults)
        {
        T[] results = getMapInternal().aggregate(filter, Aggregators.topN(ValueExtractor.identity(), comparator, cResults));
        return Arrays.asList(results);
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Ensures that this repository is initialized when accessed for the first
     * time, and calls {@link #getMap()}.
     *
     * @return the {@link NamedMap} returned by the {@link #getMap()} method
     *         implemented by the concrete subclass
     */
    private NamedMap<ID, T> getMapInternal()
        {
        ensureInitialized();
        return getMap();
        }

    /**
     * Ensures that this repository is initialized by creating necessary indices
     * on the backing map.
     * <p/>
     * Base framework classes that extend this class should call this method
     * after the backing map has been initialized, but before any other calls
     * are made.
     */
    protected void ensureInitialized()
        {
        if (!m_fInitialized)
            {
            createIndices();
            m_fInitialized = true;
            }
        }

    /**
     * Creates indices for this repository that are defined via
     * {@link Accelerated @Accelerated} and {@link Indexed @Indexed} annotations.
     * <p/>
     * If overriding this method, please call {@code super.createIndices()} or
     * the standard behavior will not work.
     */
    @SuppressWarnings("unchecked")
    protected void createIndices()
        {
        Class<? extends T> entityType = getEntityType();
        if (getClass().isAnnotationPresent(Accelerated.class) || entityType.isAnnotationPresent(Accelerated.class))
            {
            Logger.info("Configuring deserialization accelerator for " + getClass().getName());
            getMap().addIndex(new DeserializationAccelerator());
            }

        Stream.of(entityType.getMethods())
                .filter(m -> m.isAnnotationPresent(Indexed.class))
                .forEach(m ->
                         {
                         try
                             {
                             Indexed idx = m.getAnnotation(Indexed.class);
                             boolean fOrdered = idx.ordered();
                             Comparator<?> comparator = Comparator.class.equals(idx.comparator())
                                                                ? null
                                                                : (Comparator<?>) ClassHelper.newInstance(idx.comparator(), null);

                             Logger.info(() -> String.format("Creating index %s::%s (ordered=%b, comparator=%s)",
                                                       entityType.getSimpleName(), m.getName(), fOrdered, comparator));

                             getMap().addIndex(ValueExtractor.forMethod(m), fOrdered, comparator);
                             }
                         catch (Exception e)
                             {
                             throw Exceptions.ensureRuntimeException(e);
                             }
                         });
        }

    /**
     * An entry processor factory that is used by {@code update} methods that
     * accept {@link ValueUpdater} as an argument.
     *
     * @param updater  the updater function to use
     * @param value    the value to update each entity with, which will be passed
     *                 as an argument to the updater function
     * @param factory  the entity factory to use to create new entity instance
     * @param <U>      the type of value to update
     *
     * @return an entry processor that should be used to perform the update
     */
    private <U> InvocableMap.EntryProcessor<ID, T, Void> updaterProcessor(ValueUpdater<? super T, ? super U> updater, U value, EntityFactory<? super ID, ? extends T> factory)
        {
        return entry ->
            {
            T entity = entry.getValue();
            if (entity == null && factory != null)
                {
                entity = factory.create(entry.getKey());
                }
            updater.update(entity, value);
            entry.setValue(entity);
            return null;
            };
        }

    /**
     * An entry processor factory that is used by {@code update} methods that
     * accept {@link Remote.Function} as an argument.
     *
     * @param updater  the updater function to use
     * @param factory  the entity factory to use to create new entity instance
     * @param <R>      the type of return value of the updater function
     *
     * @return an entry processor that should be used to perform the update
     */
    private <R> InvocableMap.EntryProcessor<ID, T, R> updateFunctionProcessor(Remote.Function<? super T, ? extends R> updater, EntityFactory<? super ID, ? extends T> factory)
        {
        return entry ->
            {
            T entity = entry.getValue();
            if (entity == null && factory != null)
                {
                entity = factory.create(entry.getKey());
                }
            R result = updater.apply(entity);
            entry.setValue(entity);
            return result;
            };
        }

    /**
     * An entry processor factory that is used by {@code update} methods that
     * accept {@link Remote.BiFunction} as an argument.
     *
     * @param updater  the updater function to use
     * @param value    the value to update each entity with, which will be passed
     *                 as an argument to the updater function
     * @param factory  the entity factory to use to create new entity instance
     * @param <U>      the type of value to update
     * @param <R>      the type of return value of the updater function
     *
     * @return an entry processor that should be used to perform the update
     */
    private <U, R> InvocableMap.EntryProcessor<ID, T, R> updateBiFunctionProcessor(Remote.BiFunction<? super T, ? super U, ? extends R> updater, U value, EntityFactory<? super ID, ? extends T> factory)
        {
        return entry ->
            {
            T entity = entry.getValue();
            if (entity == null && factory != null)
                {
                entity = factory.create(entry.getKey());
                }
            R result = updater.apply(entity, value);
            entry.setValue(entity);
            return result;
            };
        }

    // ---- listener support ------------------------------------------------

    /**
     * Factory method for {@link Listener} adapter to {@link MapListener}.
     *
     * @param delegate  the {@link Listener} to delegate events to
     *
     * @return a {@link MapListener} that can be registered with a {@link NamedMap}
     *         and will delegate events to the {@link Listener} it wraps
     */
    protected MapListener<? super ID, ? super T> instantiateMapListener(Listener<? super T> delegate)
        {
        return new MapListenerAdapter<>(delegate);
        }

    /**
     * Register a listener that will observe all repository events.
     *
     * @param listener  the event listener to register
     */
    public void addListener(Listener<? super T> listener)
        {
        getMapInternal().addMapListener(instantiateMapListener(listener));
        }

    /**
     * Unregister a listener that observes all repository events.
     *
     * @param listener  the event listener to unregister
     */
    public void removeListener(Listener<? super T> listener)
        {
        getMapInternal().removeMapListener(instantiateMapListener(listener));
        }

    /**
     * Register a listener that will observe all events for a specific entity.
     *
     * @param id        the identifier of the entity to observe
     * @param listener  the event listener to register
     */
    public void addListener(ID id, Listener<? super T> listener)
        {
        getMapInternal().addMapListener(instantiateMapListener(listener), id, false);
        }

    /**
     * Unregister a listener that observes all events for a specific entity.
     *
     * @param id        the identifier of the entity to observe
     * @param listener  the event listener to unregister
     */
    public void removeListener(ID id, Listener<? super T> listener)
        {
        getMapInternal().removeMapListener(instantiateMapListener(listener), id);
        }

    /**
     * Register a listener that will observe all events for entities that
     * satisfy the specified criteria.
     *
     * @param filter    the criteria to use to select entities to observe
     * @param listener  the event listener to register
     */
    public void addListener(Filter<?> filter, Listener<? super T> listener)
        {
        if (!(filter instanceof MapEventFilter))
            {
            filter = new MapEventFilter<>(MapEventFilter.E_ALL, filter);
            }
        getMapInternal().addMapListener(instantiateMapListener(listener), filter, false);
        }

    /**
     * Unregister a listener that observes all events for entities that satisfy
     * the specified criteria.
     *
     * @param filter    the criteria to use to select entities to observe
     * @param listener  the event listener to unregister
     */
    public void removeListener(Filter<?> filter, Listener<? super T> listener)
        {
        if (!(filter instanceof MapEventFilter))
            {
            filter = new MapEventFilter<>(MapEventFilter.E_ALL, filter);
            }
        getMapInternal().removeMapListener(instantiateMapListener(listener), filter);
        }

    /**
     * Create new {@link Listener.Builder} instance.
     *
     * @return a new {@link Listener.Builder} instance
     */
    public Listener.Builder<T> listener()
        {
        return Listener.builder();
        }

    // ---- inner interface: Listener ---------------------------------------

    /**
     * An interface that should be implemented by the clients interested in
     * repository events.
     *
     * @param <T>  the entity type
     */
    public interface Listener<T>
        {
        /**
         * An event callback that will be called when a new entity is inserted
         * into the repository.
         *
         * @param entity  inserted entity
         */
        default void onInserted(T entity)
            {
            }

        /**
         * An event callback that will be called when an entity is updated.
         *
         * @param oldEntity  previous entity
         * @param newEntity  new entity
         */
        default void onUpdated(T oldEntity, T newEntity)
            {
            }

        /**
         * An event callback that will be called when an entity is removed from
         * the repository.
         *
         * @param entity  removed entity
         */
        default void onRemoved(T entity)
            {
            }

        /**
         * Create new {@link Listener.Builder} instance.
         *
         * @param <T>  the entity type
         *
         * @return a new {@link Listener.Builder} instance
         */
        static <T> Builder<T> builder()
            {
            return new Builder<>();
            }

        /**
         * A builder for a simple, lambda-based {@link Listener}.
         *
         * @param <T>  the entity type
         */
        class Builder<T>
            {
            /**
             * Build {@link Listener} instance.
             *
             * @return the {@link Listener} instance
             */
            public Listener<T> build()
                {
                return new DefaultListener<>(m_onInsert, m_onUpdate, m_onRemove);
                }

            // ---- event handler registration methods ----------------------

            /**
             * Add the event handler for INSERT events.
             * <p/>
             * The specified {@code eventHandler} will receive the inserted
             * entity as an argument when fired.
             *
             * @param eventHandler  the event handler to add
             *
             * @return this {@link Builder}
             */
            public Builder<T> onInsert(Consumer<T> eventHandler)
                {
                m_onInsert = addHandler(m_onInsert, eventHandler);
                return this;
                }

            /**
             * Add the event handler for UPDATE events.
             * <p/>
             * The specified {@code eventHandler} will receive the new value
             * of the updated entity as an argument when fired.
             *
             * @param eventHandler  the event handler to execute
             *
             * @return this Listener
             */
            public Builder<T> onUpdate(Consumer<T> eventHandler)
                {
                m_onUpdate = addHandler(m_onUpdate, (tOld, tNew) -> eventHandler.accept(tNew));
                return this;
                }

            /**
             * Add the event handler for UPDATE events.
             * <p/>
             * The specified {@code eventHandler} will receive both the old and
             * the new value of the updated entity as arguments when fired.
             *
             * @param eventHandler  the event handler to execute
             *
             * @return this Listener
             */
            public Builder<T> onUpdate(BiConsumer<T, T> eventHandler)
                {
                m_onUpdate = addHandler(m_onUpdate, eventHandler);
                return this;
                }

            /**
             * Add the event handler for REMOVE events.
             * <p/>
             * The specified {@code eventHandler} will receive the removed
             * entity as an argument when fired.
             *
             * @param eventHandler  the event handler to execute
             *
             * @return this Listener
             */
            public Builder<T> onRemove(Consumer<T> eventHandler)
                {
                m_onRemove = addHandler(m_onRemove, eventHandler);
                return this;
                }

            /**
             * Add the event handler for all events.
             * <p/>
             * The specified {@code eventHandler} will receive the new value of
             * the inserted or updated entity, and the old value of the removed
             * entity as an argument when fired.
             *
             * @param eventHandler  the event handler to execute
             *
             * @return this MapListener
             */
            public Builder<T> onEvent(Consumer<T> eventHandler)
                {
                onInsert(eventHandler);
                onUpdate(eventHandler);
                onRemove(eventHandler);
                return this;
                }

            // ---- helper methods ------------------------------------------

            /**
             * Add a handler to a handler chain.
             *
             * @param handlerChain  the existing handler chain (could be null)
             * @param handler       the handler to add
             *
             * @return new handler chain
             */
            private Consumer<T> addHandler(Consumer<T> handlerChain, Consumer<T> handler)
                {
                return handlerChain == null
                       ? handler
                       : handlerChain.andThen(handler);
                }

            /**
             * Add a handler to a handler chain.
             *
             * @param handlerChain  the existing handler chain (could be null)
             * @param handler       the handler to add
             *
             * @return new handler chain
             */
            private BiConsumer<T, T> addHandler(BiConsumer<T, T> handlerChain, BiConsumer<T, T> handler)
                {
                return handlerChain == null
                       ? handler
                       : handlerChain.andThen(handler);
                }

            // ---- data members ------------------------------------------------

            /**
             * The event handler to execute on INSERT event.
             */
            private Consumer<T> m_onInsert;

            /**
             * The event handler to execute on UPDATE event.
             */
            private BiConsumer<T, T> m_onUpdate;

            /**
             * The event handler to execute on REMOVE event.
             */
            private Consumer<T> m_onRemove;
            }
        }

    // ---- inner class: DefaultListener ------------------------------------

    /**
     * Simple {@link Listener} implementation that delegates each event to
     * the consumer functions it was constructed with.
     *
     * @param <T> the entity type
     */
    static class DefaultListener<T>
            implements Listener<T>
        {
        // ---- constructor -------------------------------------------------

        /**
         * Construct {@link DefaultListener} instance.
         *
         * @param onInsert  the consumer function to delegate INSERT events to
         * @param onUpdate  the consumer function to delegate UPDATE events to
         * @param onRemove  the consumer function to delegate REMOVE events to
         */
        DefaultListener(Consumer<T> onInsert, BiConsumer<T, T> onUpdate, Consumer<T> onRemove)
            {
            m_onInsert = onInsert;
            m_onUpdate = onUpdate;
            m_onRemove = onRemove;
            }

        // ---- Listener interface ------------------------------------------

        @Override
        public void onInserted(T entity)
            {
            if (m_onInsert != null)
                {
                m_onInsert.accept(entity);
                }
            }

        @Override
        public void onUpdated(T entityOld, T entityNew)
            {
            if (m_onUpdate != null)
                {
                m_onUpdate.accept(entityOld, entityNew);
                }
            }

        @Override
        public void onRemoved(T entity)
            {
            if (m_onRemove != null)
                {
                m_onRemove.accept(entity);
                }
            }

        // ---- data members ------------------------------------------------

        /**
         * The event handler to execute on INSERT event.
         */
        private final Consumer<T> m_onInsert;

        /**
         * The event handler to execute on UPDATE event.
         */
        private final BiConsumer<T, T> m_onUpdate;

        /**
         * The event handler to execute on REMOVE event.
         */
        private final Consumer<T> m_onRemove;
        }

    // ---- inner class: MapListenerAdapter ---------------------------------

    /**
     * Adapter from {@link Listener} to {@link MapListener} that can be
     * registered with the backing {@link NamedMap}.
     *
     * @param <ID>  the type of entity's identifier
     * @param <T>   the type of entity
     */
    protected static class MapListenerAdapter<ID, T>
            implements MapListener<ID, T>
        {
        /**
         * Construct MapListenerAdapter instance.
         *
         * @param listener  the listener to adapt
         */
        MapListenerAdapter(Listener<? super T> listener)
            {
            this.m_listener = listener;
            }

        // ---- MapListener interface ---------------------------------------

        @Override
        public void entryInserted(MapEvent<ID, T> mapEvent)
            {
            m_listener.onInserted(mapEvent.getNewValue());
            }

        @Override
        public void entryUpdated(MapEvent<ID, T> mapEvent)
            {
            m_listener.onUpdated(mapEvent.getOldValue(), mapEvent.getNewValue());
            }

        @Override
        public void entryDeleted(MapEvent<ID, T> mapEvent)
            {
            m_listener.onRemoved(mapEvent.getOldValue());
            }

        // ---- Object methods ----------------------------------------------

        @Override
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (o == null || getClass() != o.getClass())
                {
                return false;
                }
            MapListenerAdapter<?, ?> that = (MapListenerAdapter<?, ?>) o;
            return m_listener.equals(that.m_listener);
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(m_listener);
            }

        // ---- data members ------------------------------------------------

        /**
         * The listener to adapt.
         */
        private final Listener<? super T> m_listener;
        }

    // ---- data members ----------------------------------------------------

    /**
     * Flag indicating initialization status.
     */
    private volatile boolean m_fInitialized;
    }
