/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.repository;

import com.tangosol.net.NamedMap;

import com.tangosol.util.Aggregators;

import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.Fragment;
import com.tangosol.util.Processors;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;

import com.tangosol.util.comparator.ExtractorComparator;

import com.tangosol.util.function.Remote;

import com.tangosol.util.stream.RemoteCollector;
import com.tangosol.util.stream.RemoteCollectors;
import com.tangosol.util.stream.RemoteStream;

import java.math.BigDecimal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

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
        extends AbstractRepositoryBase<ID, T, NamedMap<ID, T>>
    {
    // ----- AbstractRepositoryBase methods ---------------------------------

    @Override
    NamedMap<ID, T> getNamedMap()
        {
        return getMap();
        }

    // ----- CRUD support ---------------------------------------------------

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
     * Return an entity with a given identifier.
     *
     * @param id  the entity's identifier
     *
     * @return an entity with a given identifier
     */
    public T get(ID id)
        {
        return getMapInternal().get(id);
        }

    /**
     * Returns true if this repository contains the entity with the specified identifier.
     *
     * @param id  the identifier of an entity to check if it exists
     *
     * @return {@code true} if this repository contains the entity with the specified identifier
     */
    public boolean exists(ID id)
        {
        return getMapInternal().containsKey(id);
        }

    /**
     * Return the value extracted from an entity with a given identifier.
     * <p/>
     * For example, you could extract {@code Person}'s {@code name} attribute by
     * calling a getter on a remote {@code Person} entity instance:
     * <pre>
     *     people.get(ssn, Person::getName);
     * </pre>
     *
     * You could also extract a {@link Fragment} containing the {@code Person}'s
     * {@code name} and {@code age} attributes by calling corresponding getters
     * on the remote {@code Person} entity instance:
     * <pre>
     *     Fragment&lt;Person> person = people.get(ssn, Extractors.fragment(Person::getName, Person::getAge));
     *     System.out.println("name: " + person.get(Person::getName));
     *     System.out.println(" age: " + person.get(Person::getAge));
     * </pre>
     *
     * Finally, you can also extract nested fragments:
     * <pre>
     *     Fragment&lt;Person> person = people.get(ssn,
     *           Extractors.fragment(Person::getName, Person::getAge,
     *                               Extractors.fragment(Person::getAddress, Address::getCity, Address::getState));
     *     System.out.println(" name: " + person.get(Person::getName));
     *     System.out.println("  age: " + person.get(Person::getAge));
     *
     *     Fragment&lt;Address> address = person.getFragment(Person::getAddress);
     *     System.out.println(" city: " + address.get(Address::getCity));
     *     System.out.println("state: " + address.get(Address::getState));
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
     * Return all entities in this repository.
     *
     * @return all entities in this repository
     */
    public Collection<T> getAll()
        {
        return getMapInternal().values();
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
     * Return the entities with the specified identifiers.
     *
     * @param colIds  the entity identifiers
     *
     * @return the entities with the specified identifiers
     */
    public Collection<T> getAll(Collection<? extends ID> colIds)
        {
        return getMapInternal().getAll(colIds).values();
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
     * Return all entities that satisfy the specified criteria.
     *
     * @param filter  the criteria to evaluate
     *
     * @return all entities that satisfy the specified criteria
     */
    public Collection<T> getAll(Filter<?> filter)
        {
        return getMapInternal().values(filter);
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
     * Return all entities in this repository, sorted using
     * specified {@link Comparable} attribute.
     *
     * @param orderBy  the {@link Comparable} attribute to sort the results by
     * @param <R>      the type of the extracted values
     *
     * @return all entities in this repository, sorted using
     *         specified {@link Comparable} attribute.
     */
    public <R extends Comparable<? super R>> Collection<T> getAllOrderedBy(ValueExtractor<? super T, ? extends R> orderBy)
        {
        return getAllOrderedBy(Filters.always(), Remote.comparator(orderBy));
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
    public <R extends Comparable<? super R>> Collection<T> getAllOrderedBy(Filter<?> filter, ValueExtractor<? super T, ? extends R> orderBy)
        {
        return getAllOrderedBy(filter, Remote.comparator(orderBy));
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
    public Collection<T> getAllOrderedBy(Remote.Comparator<? super T> orderBy)
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
    public Collection<T> getAllOrderedBy(Filter<?> filter, Remote.Comparator<? super T> orderBy)
        {
        return getMapInternal().values(filter, orderBy);
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
     * Remove the entity with a specified identifier.
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
     * Remove the entity with a specified identifier.
     *
     * @param id       the identifier of an entity to remove
     * @param fReturn  the flag specifying whether to return removed entity
     *
     * @return removed entity, iff {@code fReturn == true}; {@code null}
     *         otherwise
     */
    public T removeById(ID id, boolean fReturn)
        {
        return getMapInternal().invoke(id, Processors.remove(fReturn));
        }

    /**
     * Remove the specified entity.
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
     *         otherwise
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
     *         entities as values iff {@code fReturn == true}; {@code null} otherwise
     */
    public Map<ID, T> removeAllById(Collection<? extends ID> colIds, boolean fReturn)
        {
        return getMapInternal().invokeAll(colIds, Processors.remove(fReturn));
        }

    /**
     * Remove the specified entities.
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
     * Remove the specified entities.
     *
     * @param colEntities  the entities to remove
     * @param fReturn      the flag specifying whether to return removed entity
     *
     * @return the map of removed entity identifiers as keys, and the removed
     *         entities as values iff {@code fReturn == true}; {@code null} otherwise
     */
    public Map<ID, T> removeAll(Collection<? extends T> colEntities, boolean fReturn)
        {
        return removeAll(colEntities.stream(), fReturn);
        }

    /**
     * Remove the specified entities.
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
     * @return the grouping of entities by the specified extractor; the keys
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
     * @return the grouping of entities by the specified extractor; the keys
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
     * @return the grouping of entities by the specified extractor; the keys
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
     * @return the grouping of entities by the specified extractor; the keys
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
     * @return the grouping of entities by the specified extractor; the keys
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
     * @return the grouping of entities by the specified extractor; the keys
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
     * @return the grouping of entities by the specified extractor; the keys
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
     * @return the grouping of entities by the specified extractor; the keys
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
     */
    private NamedMap<ID, T> getMapInternal()
        {
        ensureInitialized();
        return getMap();
        }
    }
