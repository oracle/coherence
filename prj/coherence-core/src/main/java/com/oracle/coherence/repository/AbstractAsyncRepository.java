/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.repository;

import com.tangosol.net.AsyncNamedMap;
import com.tangosol.net.NamedMap;

import com.tangosol.util.Aggregators;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.Fragment;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.Processors;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;

import com.tangosol.util.comparator.ExtractorComparator;

import com.tangosol.util.function.Remote;

import com.tangosol.util.stream.RemoteCollector;
import com.tangosol.util.stream.RemoteCollectors;

import java.math.BigDecimal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import java.util.concurrent.CompletableFuture;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tangosol.util.function.Remote.Comparator.comparing;

import static com.tangosol.util.stream.RemoteCollectors.groupingBy;

/**
 * Abstract base class for asynchronous Coherence
 * <a href="https://martinfowler.com/eaaCatalog/repository.html">repository</a>
 * implementations.
 *
 * @param <ID>  the type of entity's identifier
 * @param <T>   the type of entity stored in this repository
 *
 * @author Ryan Lubke    2021.04.08
 * @author Aleks Seovic
 * @since 21.06
 */
public abstract class AbstractAsyncRepository<ID, T>
        extends AbstractRepositoryBase<ID, T, AsyncNamedMap<ID, T>>
    {
    // ----- AbstractRepositoryBase methods ---------------------------------

    @Override
    NamedMap<ID, T> getNamedMap()
        {
        return getMap().getNamedMap();
        }

    // ----- CRUD support ---------------------------------------------------

    /**
     * Store the specified entity.
     *
     * @param entity  the entity to store
     *
     * @return the saved entity
     */
    public CompletableFuture<T> save(T entity)
        {
        return getMapInternal().putAll(Collections.singletonMap(getId(entity), entity)).thenApply(unused -> entity);
        }

    /**
     * Store all specified entities as a batch.
     *
     * @param colEntities  the entities to store
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     */
    public CompletableFuture<Void> saveAll(Collection<? extends T> colEntities)
        {
        return saveAll(colEntities.stream());
        }

    /**
     * Store all specified entities as a batch.
     *
     * @param strEntities  the entities to store
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     */
    public CompletableFuture<Void> saveAll(Stream<? extends T> strEntities)
        {
        return getMapInternal().putAll(strEntities.collect(Collectors.toMap(this::getId, v -> v)));
        }

    /**
     * Return an entity with a given identifier.
     *
     * @param id  the entity's identifier
     *
     * @return a {@link CompletableFuture} for the value to which the specified
     *         {@code id} is mapped
     */
    public CompletableFuture<T> get(ID id)
        {
        return getMapInternal().get(id);
        }

    /**
     * Returns true if this repository contains the entity with the specified identifier.
     *
     * @param id  the identifier of an entity to check if it exists
     *
     * @return a {@link CompletableFuture} that will resolve to {@code true} if this repository contains the entity with the specified identifier
     */
    public CompletableFuture<Boolean> exists(ID id)
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
     * @return a {@link CompletableFuture} that will resolve to the extracted value
     */
    public <R> CompletableFuture<R> get(ID id, ValueExtractor<? super T, ? extends R> extractor)
        {
        return getMapInternal().invoke(id, Processors.extract(extractor));
        }

    /**
     * Return all entities in this repository.
     *
     * @return a {@link CompletableFuture} that will resolve to all entities in this repository
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public CompletableFuture<Collection<? extends T>> getAll()
        {
        return (CompletableFuture) getMapInternal().values();
        }

    /**
     * Stream all entities all entities in this repository.
     *
     * @param callback  a {@link Consumer consumer} of results as they become available
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     * @see #getAll()
     */
    public CompletableFuture<Void> getAll(Consumer<? super T> callback)
        {
        return getMapInternal().values(callback);
        }

    /**
     * Return a map of values extracted from all entities in the repository.
     *
     * @param extractor  the {@link ValueExtractor} to extract values with
     * @param <R>        the type of the extracted values
     *
     * @return a {@link CompletableFuture} that will resolve to a map of extracted values, keyed by entity id
     * @see #get(Object, ValueExtractor)
     */
    public <R> CompletableFuture<Map<ID, R>> getAll(ValueExtractor<? super T, ? extends R> extractor)
        {
        return getAll(Filters.always(), extractor);
        }

    /**
     * Streams the id and the associated extracted value from all entities in the repository.
     *
     * @param extractor  the {@link ValueExtractor} to extract values with
     * @param callback   a {@link BiConsumer consumer} of results as they become available
     * @param <R>        the type of the extracted values
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     * @see #getAll(ValueExtractor)
     */
    public <R> CompletableFuture<Void> getAll(ValueExtractor<? super T, ? extends R> extractor,
                BiConsumer<? super ID, ? super R> callback)
        {
        return getAll(Filters.always(), extractor, callback);
        }

    /**
     * Return the entities with the specified identifiers.
     *
     * @param colIds  the entity identifiers
     *
     * @return a {@link CompletableFuture} that will resolve to the entities with the specified identifiers
     */
    public CompletableFuture<Collection<T>> getAll(Collection<? extends ID> colIds)
        {
        return getMapInternal().getAll(colIds).thenApply(Map::values);
        }

    /**
     * Stream the entities associated with the specified ids to the provided callback.
     *
     * @param colIds    a {@link Collection} of ids that may be present in this repository
     * @param callback  a {@link Consumer consumer} of results as they become available
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     * @see #getAll(Collection)
     */
    public CompletableFuture<Void> getAll(Collection<? extends ID> colIds, Consumer<? super T> callback)
        {
        return getMapInternal().getAll(colIds, entry -> callback.accept(entry.getValue()));
        }

    /**
     * Return a map of values extracted from a set of entities with the given
     * identifiers.
     *
     * @param colIds     the entity identifiers
     * @param extractor  the {@link ValueExtractor} to extract values with
     * @param <R>        the type of the extracted values
     *
     * @return a {@link CompletableFuture} that will resolve to a map of extracted values, keyed by entity id
     * @see #get(Object, ValueExtractor)
     */
    public <R> CompletableFuture<Map<ID, R>> getAll(Collection<? extends ID> colIds,
            ValueExtractor<? super T, ? extends R> extractor)
        {
        return getMapInternal().invokeAll(colIds, Processors.extract(extractor));
        }

    /**
     * Stream the entities associated with the specified ids to the provided callback.
     *
     * @param colIds     a {@link Collection} of ids that may be present in this repository
     * @param extractor  the {@link ValueExtractor} to extract values with
     * @param callback   a {@link BiConsumer consumer} of results as they become available
     * @param <R>        the type of the extracted values
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     * @see #getAll(Collection)
     */
    public <R> CompletableFuture<Void> getAll(Collection<? extends ID> colIds,
                 ValueExtractor<? super T, ? extends R> extractor, BiConsumer<? super ID, ? super R> callback)
        {
        return getMapInternal().invokeAll(colIds, Processors.extract(extractor), callback);
        }

    /**
     * Return all entities that satisfy the specified criteria.
     *
     * @param filter  the criteria to evaluate
     *
     * @return a {@link CompletableFuture} that will resolve to all entities that satisfy the specified criteria
     */
    public CompletableFuture<Collection<T>> getAll(Filter<?> filter)
        {
        return getMapInternal().values(filter);
        }

    /**
     * Stream all entities that satisfy the specified criteria.
     *
     * @param filter    the criteria to evaluate
     * @param callback  a consumer of results as they become available
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     * @see #getAll(Filter)
     */
    public CompletableFuture<Void> getAll(Filter<?> filter, Consumer<? super T> callback)
        {
        return getMapInternal().values(filter, callback);
        }

    /**
     * Return a map of values extracted from a set of entities based on the
     * specified criteria.
     *
     * @param filter     the criteria to use to select entities for extraction
     * @param extractor  the {@link ValueExtractor} to extract values with
     * @param <R>        the type of the extracted values
     *
     * @return a {@link CompletableFuture} that will resolve to a map of extracted values, keyed by entity id
     * @see #get(Object, ValueExtractor)
     */
    public <R> CompletableFuture<Map<ID, R>> getAll(Filter<?> filter, ValueExtractor<? super T, ? extends R> extractor)
        {
        return getMapInternal().invokeAll(filter, Processors.extract(extractor));
        }

    /**
     * Streams the id and the associated extracted value from a set of entities based on the
     * specified criteria.
     *
     * @param filter     the criteria to use to select entities for extraction
     * @param extractor  the {@link ValueExtractor} to extract values with
     * @param callback   a {@link BiConsumer consumer} of results as they become available
     * @param <R>        the type of the extracted values
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     * @see #getAll(Filter, ValueExtractor)
     */
    public <R> CompletableFuture<Void> getAll(Filter<?> filter, ValueExtractor<? super T, ? extends R> extractor,
                BiConsumer<? super ID, ? super R> callback)
        {
        return getMapInternal().invokeAll(filter, Processors.extract(extractor), callback);
        }

    /**
     * Return all entities in this repository, sorted using
     * specified {@link Comparable} attribute.
     *
     * @param orderBy  the {@link Comparable} attribute to sort the results by
     * @param <R>      the type of the extracted values
     *
     * @return a {@link CompletableFuture} that will resolve to all entities in this repository, sorted using
     *         specified {@link Comparable} attribute.
     */
    public <R extends Comparable<? super R>> CompletableFuture<Collection<T>> getAllOrderedBy(
            ValueExtractor<? super T, ? extends R> orderBy)
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
     * @return a {@link CompletableFuture} that will resolve to all entities that satisfy specified criteria,
     *         sorted using specified {@link Comparable} attribute.
     */
    public <R extends Comparable<? super R>> CompletableFuture<Collection<T>> getAllOrderedBy(
            Filter<?> filter, ValueExtractor<? super T, ? extends R> orderBy)
        {
        return getAllOrderedBy(filter, Remote.comparator(orderBy));
        }

    /**
     * Return all entities in this repository, sorted using
     * specified {@link Remote.Comparator}.
     *
     * @param orderBy  the comparator to sort the results with
     *
     * @return a {@link CompletableFuture} that will resolve to all entities in this repository, sorted using
     *         specified {@link Remote.Comparator}.
     */
    public CompletableFuture<Collection<T>> getAllOrderedBy(Remote.Comparator<? super T> orderBy)
        {
        return getAllOrderedBy(Filters.always(), orderBy);
        }

    /**
     * Return all entities that satisfy the specified criteria, sorted using
     * specified {@link Remote.Comparator}.
     *
     * @param filter   the criteria to evaluate
     * @param orderBy  the comparator to sort the results with
     *
     * @return a {@link CompletableFuture} that will resolve to all entities that satisfy specified criteria,
     *         sorted using specified {@link Remote.Comparator}.
     */
    public CompletableFuture<Collection<T>> getAllOrderedBy(Filter<?> filter, Remote.Comparator<? super T> orderBy)
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
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     */
    public <U> CompletableFuture<Void> update(ID id, ValueUpdater<? super T, ? super U> updater, U value)
        {
        return update(id, updater, value, null);
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
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     */
    public <U> CompletableFuture<Void> update(ID id, ValueUpdater<? super T, ? super U> updater, U value,
                EntityFactory<? super ID, ? extends T> factory)
        {
        return getMapInternal().invoke(id, updaterProcessor(updater, value, factory));
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
     * @return a {@link CompletableFuture} that will resolve to the result of updater function evaluation
     */
    public <R> CompletableFuture<R> update(ID id, Remote.Function<? super T, ? extends R> updater)
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
     * @return a {@link CompletableFuture} that will resolve to the result of updater function evaluation
     */
    public <R> CompletableFuture<R> update(ID id, Remote.Function<? super T, ? extends R> updater,
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
     * @return a {@link CompletableFuture} that will resolve to the result of updater function evaluation
     */
    public <U, R> CompletableFuture<R> update(ID id, Remote.BiFunction<? super T, ? super U, ? extends R> updater,
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
     * @return a {@link CompletableFuture} that will resolve to the result of updater function evaluation
     */
    public <U, R> CompletableFuture<R> update(ID id, Remote.BiFunction<? super T, ? super U, ? extends R> updater,
                U value, EntityFactory<? super ID, ? extends T> factory)
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
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     */
    @SuppressWarnings("CheckStyle")
    public <U> CompletableFuture<Void> updateAll(Filter<?> filter, ValueUpdater<? super T, ? super U> updater,
                U value)
        {
        return getMapInternal().invokeAll(filter, updaterProcessor(updater, value, null)).thenAccept(m -> {});
        }

    /**
     * Update multiple entities using specified updater function.
     *
     * @param filter   the criteria to use to select entities to update
     * @param updater  the updater function to use
     * @param <R>      the type of return value of the updater function
     *
     * @return a {@link CompletableFuture} that will resolve to a map of updater function results, keyed by entity id
     */
    public <R> CompletableFuture<Map<ID, R>> updateAll(Filter<?> filter,
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
     * @return a {@link CompletableFuture} that will resolve to a map of updater function results, keyed by entity id
     */
    public <U, R> CompletableFuture<Map<ID, R>> updateAll(Filter<?> filter,
            Remote.BiFunction<? super T, ? super U, ? extends R> updater, U value)
        {
        return getMapInternal().invokeAll(filter, updateBiFunctionProcessor(updater, value, null));
        }

    /**
     * Remove the entity with a specified identifier.
     *
     * @param id  the identifier of an entity to remove, if present
     *
     * @return a {@link CompletableFuture} that will resolve to {@code true} if this repository contained
     *        the specified entity
     */
    public CompletableFuture<Boolean> removeById(ID id)
        {
        return getMapInternal().invoke(id, Processors.remove());
        }

    /**
     * Remove the entity with a specified identifier.
     *
     * @param id       the identifier of an entity to remove
     * @param fReturn  the flag specifying whether to return removed entity
     *
     * @return a {@link CompletableFuture} that will resolve to the removed entity, iff {@code fReturn == true};
     *         {@code null} otherwise
     */
    public CompletableFuture<T> removeById(ID id, boolean fReturn)
        {
        return getMapInternal().invoke(id, Processors.remove(fReturn));
        }

    /**
     * Remove the specified entity.
     *
     * @param entity  the entity to remove
     *
     * @return a {@link CompletableFuture} that will resolve to {@code true} if this repository contained
     *         the specified entity
     */
    public CompletableFuture<Boolean> remove(T entity)
        {
        return removeById(getId(entity));
        }

    /**
     * Remove the specified entity.
     *
     * @param entity   the entity to remove
     * @param fReturn  the flag specifying whether to return removed entity
     *
     * @return a {@link CompletableFuture} that will resolve to the removed entity, iff {@code fReturn == true};
     *         {@code null} otherwise
     */
    public CompletableFuture<T> remove(T entity, boolean fReturn)
        {
        return removeById(getId(entity), fReturn);
        }

    /**
     * Remove entities with the specified identifiers.
     *
     * @param colIds  the identifiers of the entities to remove
     *
     * @return a {@link CompletableFuture} that will resolve to {@code true} if this repository changed
     *         as a result of the call
     */
    public CompletableFuture<Boolean> removeAllById(Collection<? extends ID> colIds)
        {
        return getMapInternal().invokeAll(colIds, Processors.remove())
                               .thenApply(idBooleanMap ->
                                              {
                                              for (boolean fResult : idBooleanMap.values())
                                                  {
                                                  if (fResult)
                                                      {
                                                      return true;
                                                      }
                                                  }

                                              return false;
                                              });
        }

    /**
     * Remove entities with the specified identifiers.
     *
     * @param colIds   the identifiers of the entities to remove
     * @param fReturn  the flag specifying whether to return removed entity
     *
     * @return a {@link CompletableFuture} that will resolve to a map of removed entity identifiers as keys,
     *         and the removed entities as values iff {@code fReturn == true}; {@code null} otherwise
     */
    public CompletableFuture<Map<ID, T>> removeAllById(Collection<? extends ID> colIds, boolean fReturn)
        {
        return getMapInternal().invokeAll(colIds, Processors.remove(fReturn));
        }

    /**
     * Remove the specified entities.
     *
     * @param colEntities  the entities to remove
     *
     * @return a {@link CompletableFuture} that will resolve to {@code true} if this repository changed
     *         as a result of the call
     */
    public CompletableFuture<Boolean> removeAll(Collection<? extends T> colEntities)
        {
        return removeAll(colEntities.stream());
        }

    /**
     * Remove the specified entities.
     *
     * @param colEntities  the entities to remove
     * @param fReturn      the flag specifying whether to return removed entity
     *
     * @return a {@link CompletableFuture} that will resolve to a map of removed entity identifiers as keys,
     *         and the removed entities as values iff {@code fReturn == true}; {@code null} otherwise
     */
    public CompletableFuture<Map<ID, T>> removeAll(Collection<? extends T> colEntities, boolean fReturn)
        {
        return removeAll(colEntities.stream(), fReturn);
        }

    /**
     * Remove the specified entities.
     *
     * @param strEntities  the entities to remove
     *
     * @return a {@link CompletableFuture} that will resolve to {@code true} if this repository changed
     *         as a result of the call
     */
    public CompletableFuture<Boolean> removeAll(Stream<? extends T> strEntities)
        {
        return removeAllById(strEntities.map(this::getId).collect(Collectors.toSet()));
        }

    /**
     * Remove the specified entities.
     *
     * @param strEntities  the entities to remove
     * @param fReturn      the flag specifying whether to return removed entity
     *
     * @return a {@link CompletableFuture} that will resolve to a map of removed entity identifiers as keys,
     *         and the removed entities as values iff {@code fReturn == true}; {@code null} otherwise
     */
    public CompletableFuture<Map<ID, T>> removeAll(Stream<? extends T> strEntities, boolean fReturn)
        {
        return removeAllById(strEntities.map(this::getId).collect(Collectors.toSet()), fReturn);
        }

    /**
     * Remove all entities based on the specified criteria.
     *
     * @param filter  the criteria that should be used to select entities to
     *                remove
     *
     * @return a {@link CompletableFuture} that will resolve to {@code true} if this repository changed
     *         as a result of the call
     */
    public CompletableFuture<Boolean> removeAll(Filter<?> filter)
        {
        return getMapInternal().invokeAll(filter, Processors.remove())
                .thenApply(idBooleanMap ->
                               {
                               for (boolean fResult : idBooleanMap.values())
                                   {
                                   if (fResult)
                                       {
                                       return true;
                                       }
                                   }
                               return false;
                               });
        }

    /**
     * Remove all entities based on the specified criteria.
     *
     * @param filter   the criteria that should be used to select entities to
     *                 remove
     * @param fReturn  the flag specifying whether to return removed entity
     *
     * @return a {@link CompletableFuture} that will resolve to a map of removed entity identifiers as keys,
     *         and the removed entities as values iff {@code fReturn == true}; {@code null} otherwise
     */
    public CompletableFuture<Map<ID, T>> removeAll(Filter<?> filter, boolean fReturn)
        {
        return getMapInternal().invokeAll(filter, Processors.remove(fReturn));
        }

    // ---- aggregation support ---------------------------------------------

    /**
     * Return the number of entities in this repository.
     *
     * @return a {@link CompletableFuture} that will resolve to the number of entities in this repository
     */
    public CompletableFuture<Long> count()
        {
        return getMapInternal().aggregate(Aggregators.count()).thenApply(Integer::longValue);
        }

    /**
     * Return the number of entities in this repository that satisfy specified
     * filter.
     *
     * @param filter  the filter to evaluate
     *
     * @return a {@link CompletableFuture} that will resolve to the number of entities in this repository
     *         that satisfy specified filter
     */
    public CompletableFuture<Long> count(Filter<?> filter)
        {
        return getMapInternal().aggregate(filter, Aggregators.count()).thenApply(Integer::longValue);
        }

    /**
     * Return the maximum value of the specified function.
     *
     * @param extractor  the function to determine the maximum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     *
     * @return a {@link CompletableFuture} that will resolve to the maximum value of the specified function
     */
    public CompletableFuture<Integer> max(Remote.ToIntFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(Aggregators.max(extractor)).thenApply(Long::intValue);
        }

    /**
     * Return the maximum value of the specified function.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the function to determine the maximum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     *
     * @return a {@link CompletableFuture} that will resolve to the maximum value of the specified function
     */
    public CompletableFuture<Integer> max(Filter<?> filter, Remote.ToIntFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(filter, Aggregators.max(extractor)).thenApply(Long::intValue);
        }

    /**
     * Return the maximum value of the specified function.
     *
     * @param extractor  the function to determine the maximum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     *
     * @return a {@link CompletableFuture} that will resolve to the maximum value of the specified function
     */
    public CompletableFuture<Long> max(Remote.ToLongFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the maximum value of the specified function
     */
    public CompletableFuture<Long> max(Filter<?> filter, Remote.ToLongFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the maximum value of the specified function
     */
    public CompletableFuture<Double> max(Remote.ToDoubleFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the maximum value of the specified function
     */
    public CompletableFuture<Double> max(Filter<?> filter, Remote.ToDoubleFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the maximum value of the specified function
     */
    public CompletableFuture<BigDecimal> max(Remote.ToBigDecimalFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the maximum value of the specified function
     */
    public CompletableFuture<BigDecimal> max(Filter<?> filter, Remote.ToBigDecimalFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the maximum value of the specified function
     */
    public <R extends Comparable<? super R>> CompletableFuture<R> max(
            Remote.ToComparableFunction<? super T, R> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the maximum value of the specified function
     */
    public <R extends Comparable<? super R>> CompletableFuture<R> max(
            Filter<?> filter,
            Remote.ToComparableFunction<? super T, R> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the entity with the maximum value
     *        of the specified function
     */
    public <R extends Comparable<? super R>> CompletableFuture<Optional<T>> maxBy(
            ValueExtractor<? super T, ? extends R> extractor)
        {
        return CompletableFuture.supplyAsync(() -> getNamedMap().stream()
                .map(Map.Entry::getValue)
                .reduce(Remote.BinaryOperator.maxBy(comparing(extractor))));
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
     * @return a {@link CompletableFuture} that will resolve to the entity with the maximum value
     *        of the specified function
     */
    public <R extends Comparable<? super R>> CompletableFuture<Optional<T>> maxBy(
            Filter<?> filter, ValueExtractor<? super T, ? extends R> extractor)
        {
        return CompletableFuture.supplyAsync(() -> getNamedMap().stream(filter)
                .map(Map.Entry::getValue)
                .reduce(Remote.BinaryOperator.maxBy(comparing(extractor))));
        }

    /**
     * Return the minimum value of the specified function.
     *
     * @param extractor  the function to determine the minimum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     *
     * @return a {@link CompletableFuture} that will resolve to the minimum value of the specified function
     */
    public CompletableFuture<Integer> min(Remote.ToIntFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(Aggregators.min(extractor)).thenApply(Long::intValue);
        }

    /**
     * Return the minimum value of the specified function.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the function to determine the minimum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     *
     * @return a {@link CompletableFuture} that will resolve to the minimum value of the specified function
     */
    public CompletableFuture<Integer> min(Filter<?> filter, Remote.ToIntFunction<? super T> extractor)
        {
        return getMapInternal().aggregate(filter, Aggregators.min(extractor)).thenApply(Long::intValue);
        }

    /**
     * Return the minimum value of the specified function.
     *
     * @param extractor  the function to determine the minimum value for;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     *
     * @return a {@link CompletableFuture} that will resolve to the minimum value of the specified function
     */
    public CompletableFuture<Long> min(Remote.ToLongFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the minimum value of the specified function
     */
    public CompletableFuture<Long> min(Filter<?> filter, Remote.ToLongFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the minimum value of the specified function
     */
    public CompletableFuture<Double> min(Remote.ToDoubleFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the minimum value of the specified function
     */
    public CompletableFuture<Double> min(Filter<?> filter, Remote.ToDoubleFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the minimum value of the specified function
     */
    public CompletableFuture<BigDecimal> min(Remote.ToBigDecimalFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the minimum value of the specified function
     */
    public CompletableFuture<BigDecimal> min(Filter<?> filter, Remote.ToBigDecimalFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the minimum value of the specified function
     */
    public <R extends Comparable<? super R>> CompletableFuture<R> min(
            Remote.ToComparableFunction<? super T, R> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the minimum value of the specified function
     */
    public <R extends Comparable<? super R>> CompletableFuture<R> min(Filter<?> filter,
                Remote.ToComparableFunction<? super T, R> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the entity with the minimum value
     *         of the specified function
     */
    public <R extends Comparable<? super R>> CompletableFuture<Optional<T>> minBy(
                ValueExtractor<? super T, ? extends R> extractor)
        {
        return CompletableFuture.supplyAsync(() -> getNamedMap().stream()
                .map(Map.Entry::getValue)
                .reduce(Remote.BinaryOperator.minBy(comparing(extractor))));
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
     * @return a {@link CompletableFuture} that will resolve to the entity with the minimum value
     *         of the specified function
     */
    public <R extends Comparable<? super R>> CompletableFuture<Optional<T>> minBy(Filter<?> filter,
                ValueExtractor<? super T, ? extends R> extractor)
        {
        return CompletableFuture.supplyAsync(() -> getNamedMap().stream(filter)
                .map(Map.Entry::getValue)
                .reduce(Remote.BinaryOperator.minBy(comparing(extractor))));
        }

    /**
     * Return the sum of the specified function.
     *
     * @param extractor  the function to sum;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getAge}
     *
     * @return a {@link CompletableFuture} that will resolve to the sum of the specified function
     */
    public CompletableFuture<Long> sum(Remote.ToIntFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the sum of the specified function
     */
    public CompletableFuture<Long> sum(Filter<?> filter, Remote.ToIntFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the sum of the specified function
     */
    public CompletableFuture<Long> sum(Remote.ToLongFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the sum of the specified function
     */
    public CompletableFuture<Long> sum(Filter<?> filter, Remote.ToLongFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the sum of the specified function
     */
    public CompletableFuture<Double> sum(Remote.ToDoubleFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the sum of the specified function
     */
    public CompletableFuture<Double> sum(Filter<?> filter, Remote.ToDoubleFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the sum of the specified function
     */
    public CompletableFuture<BigDecimal> sum(Remote.ToBigDecimalFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the sum of the specified function
     */
    public CompletableFuture<BigDecimal> sum(Filter<?> filter, Remote.ToBigDecimalFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the average of the specified function
     */
    public CompletableFuture<Double> average(Remote.ToIntFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the average of the specified function
     */
    public CompletableFuture<Double> average(Filter<?> filter, Remote.ToIntFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the average of the specified function
     */
    public CompletableFuture<Double> average(Remote.ToLongFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the average of the specified function
     */
    public CompletableFuture<Double> average(Filter<?> filter, Remote.ToLongFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the average of the specified function
     */
    public CompletableFuture<Double> average(Remote.ToDoubleFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the average of the specified function
     */
    public CompletableFuture<Double> average(Filter<?> filter, Remote.ToDoubleFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the average of the specified function
     */
    public CompletableFuture<BigDecimal> average(Remote.ToBigDecimalFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to the average of the specified function
     */
    public CompletableFuture<BigDecimal> average(Filter<?> filter, Remote.ToBigDecimalFunction<? super T> extractor)
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
     * @return a {@link CompletableFuture} that will resolve to a set of distinct values for the specified extractor
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R> CompletableFuture<Collection<? extends R>> distinct(ValueExtractor<? super T, ? extends R> extractor)
        {
        InvocableMap.StreamingAggregator aggregator = Aggregators.distinctValues(extractor);

        return getMapInternal().aggregate(aggregator);
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
     * @return a {@link CompletableFuture} that will resolve to a set of distinct values for the specified extractor
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R> CompletableFuture<Collection<? extends R>> distinct(Filter<?> filter,
                ValueExtractor<? super T, ? extends R> extractor)
        {
        InvocableMap.StreamingAggregator aggregator = Aggregators.distinctValues(extractor);

        return getMapInternal().aggregate(filter, aggregator);
        }

    /**
     * Return the grouping of entities by the specified extractor.
     *
     * @param extractor  the extractor to get a grouping value from;
     *                   typically a method reference on the entity class,
     *                   such as {@code Person::getGender}
     * @param <K>        the type of extracted grouping keys
     *
     * @return a {@link CompletableFuture} that will resolve to a grouping of entities by the specified extractor;
     *         the keys in the returned map will be distinct values extracted by the specified {@code extractor},
     *         and the values will be sets of entities that match each extracted key
     */
    public <K> CompletableFuture<Map<K, Set<T>>> groupBy(ValueExtractor<? super T, ? extends K> extractor)
        {
        return CompletableFuture.supplyAsync(() -> getNamedMap().stream()
                .map(Map.Entry::getValue)
                .collect(groupingBy(extractor, RemoteCollectors.toSet())));
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
     * @return a {@link CompletableFuture} that will resolve to a grouping of entities by the specified extractor;
     *         the keys in the returned map will be distinct values extracted by the specified {@code extractor},
     *         and the values will be sets of entities that match each extracted key
     */
    public <K> CompletableFuture<Map<K, SortedSet<T>>> groupBy(ValueExtractor<? super T, ? extends K> extractor,
                Remote.Comparator<? super T> orderBy)
        {
        return CompletableFuture.supplyAsync(() -> getNamedMap().stream()
                .map(Map.Entry::getValue)
                .collect(groupingBy(extractor, RemoteCollectors.toSortedSet(orderBy))));
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
     * @return a {@link CompletableFuture} that will resolve to a grouping of entities by the specified extractor;
     *         the keys in the returned map will be distinct values extracted by the specified {@code extractor},
     *         and the values will be sets of entities that match each extracted key
     */
    public <K> CompletableFuture<Map<K, Set<T>>> groupBy(Filter<?> filter,
                ValueExtractor<? super T, ? extends K> extractor)
        {
        return CompletableFuture.supplyAsync(() -> getNamedMap().stream(filter)
                .map(Map.Entry::getValue)
                .collect(groupingBy(extractor, RemoteCollectors.toSet())));
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
     * @return a {@link CompletableFuture} that will resolve to a grouping of entities by the specified extractor;
     *         the keys in the returned map will be distinct values extracted by the specified {@code extractor},
     *         and the values will be sets of entities that match each extracted key
     */
    public <K> CompletableFuture<Map<K, SortedSet<T>>> groupBy(Filter<?> filter,
                ValueExtractor<? super T, ? extends K> extractor, Remote.Comparator<? super T> orderBy)
        {
        return CompletableFuture.supplyAsync(() -> getNamedMap().stream(filter)
                .map(Map.Entry::getValue)
                .collect(groupingBy(extractor, RemoteCollectors.toSortedSet(orderBy))));
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
     * @return a {@link CompletableFuture} that will resolve to a grouping of entities by the specified extractor;
     *         the keys in the returned map will be distinct values extracted by the specified {@code extractor},
     *         and the values will be results of the specified {@code collector} for each group
     *
     * @see RemoteCollectors
     */
    public <K, A, R> CompletableFuture<Map<K, R>> groupBy(ValueExtractor<? super T, ? extends K> extractor,
                RemoteCollector<? super T, A, R> collector)
        {
        return CompletableFuture.supplyAsync(() -> getNamedMap().stream()
                .map(Map.Entry::getValue)
                .collect(groupingBy(extractor, collector)));
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
     * @return a {@link CompletableFuture} that will resolve to a grouping of entities by the specified extractor;
     *         the keys in the returned map will be distinct values extracted by the specified {@code extractor},
     *         and the values will be results of the specified {@code collector} for each group
     *
     * @see RemoteCollectors
     */
    public <K, A, R> CompletableFuture<Map<K, R>> groupBy(Filter<?> filter,
                ValueExtractor<? super T, ? extends K> extractor, RemoteCollector<? super T, A, R> collector)
        {
        return CompletableFuture.supplyAsync(() -> getNamedMap().stream(filter)
                .map(Map.Entry::getValue)
                .collect(groupingBy(extractor, collector)));
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
     * @return a {@link CompletableFuture} that will resolve to a grouping of entities by the specified extractor;
     *         the keys in the returned map will be distinct values extracted by the specified {@code extractor},
     *         and the values will be results of the specified {@code collector} for each group
     *
     * @see RemoteCollectors
     */
    public <K, A, R, M extends Map<K, R>> CompletableFuture<M> groupBy(ValueExtractor<? super T, ? extends K> extractor,
                Remote.Supplier<M> mapFactory, RemoteCollector<? super T, A, R> collector)
        {
        return CompletableFuture.supplyAsync(() -> getNamedMap().stream()
                .map(Map.Entry::getValue)
                .collect(groupingBy(extractor, mapFactory, collector)));
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
     * @return a {@link CompletableFuture} that will resolve to a grouping of entities by the specified extractor;
     *         the keys in the returned map will be distinct values extracted by the specified {@code extractor},
     *         and the values will be results of the specified {@code collector} for each group
     *
     * @see RemoteCollectors
     */
    public <K, A, R, M extends Map<K, R>> CompletableFuture<M> groupBy(Filter<?> filter,
                ValueExtractor<? super T, ? extends K> extractor, Remote.Supplier<M> mapFactory,
                RemoteCollector<? super T, A, R> collector)
        {
        return CompletableFuture.supplyAsync(() -> getNamedMap().stream(filter)
                .map(Map.Entry::getValue)
                .collect(groupingBy(extractor, mapFactory, collector)));
        }

    /**
     * Return the top N highest values for the specified extractor.
     *
     * @param extractor  the extractor to get the values to compare with
     * @param cResults   the number of highest values to return
     * @param <R>        the type of returned values
     *
     * @return a {@link CompletableFuture} that will resolve to the top N highest values for the specified extractor
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R extends Comparable<? super R>> CompletableFuture<List<R>> top(
                ValueExtractor<? super T, ? extends R> extractor,
                int cResults)
        {
        InvocableMap.StreamingAggregator aggregator = Aggregators.topN(extractor, cResults);

        return getMapInternal()
                .aggregate(aggregator)
                .thenApply(rs -> Arrays.stream((Object[]) rs).collect(Collectors.toList()));
        }

    /**
     * Return the top N highest values for the specified extractor.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the extractor to get the values to compare with
     * @param cResults   the number of highest values to return
     * @param <R>        the type of returned values
     *
     * @return a {@link CompletableFuture} that will resolve to the top N highest values for the specified extractor
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R extends Comparable<? super R>> CompletableFuture<List<R>> top(Filter<?> filter,
                ValueExtractor<? super T, ? extends R> extractor, int cResults)
        {
        InvocableMap.StreamingAggregator aggregator = Aggregators.topN(extractor, cResults);

        return getMapInternal()
                .aggregate(filter, aggregator)
                .thenApply(rs -> Arrays.stream((Object[]) rs).collect(Collectors.toList()));
        }

    /**
     * Return the top N highest values for the specified extractor.
     *
     * @param extractor   the extractor to get the values to compare with
     * @param comparator  the comparator to use when comparing extracted values
     * @param cResults    the number of highest values to return
     * @param <R>         the type of returned values
     *
     * @return a {@link CompletableFuture} that will resolve to the top N highest values for the specified extractor
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R> CompletableFuture<List<R>> top(ValueExtractor<? super T, ? extends R> extractor,
                Remote.Comparator<? super R> comparator, int cResults)
        {
        InvocableMap.StreamingAggregator aggregator = Aggregators.topN(extractor, comparator, cResults);

        return getMapInternal()
                .aggregate(aggregator)
                .thenApply(rs -> Arrays.stream((Object[]) rs).collect(Collectors.toList()));
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
     * @return a {@link CompletableFuture} that will resolve to the top N highest values for the specified extractor
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R> CompletableFuture<List<R>> top(Filter<?> filter, ValueExtractor<? super T, ? extends R> extractor,
                Remote.Comparator<? super R> comparator, int cResults)
        {
        InvocableMap.StreamingAggregator aggregator = Aggregators.topN(extractor, comparator, cResults);

        return getMapInternal()
                .aggregate(filter, aggregator)
                .thenApply(rs -> Arrays.stream((Object[]) rs).collect(Collectors.toList()));
        }

    /**
     * Return the top N entities with the highest values for the specified extractor.
     *
     * @param extractor  the extractor to get the values to compare with
     * @param cResults   the number of highest values to return
     * @param <R>        the type of values used for comparison
     *
     * @return a {@link CompletableFuture} that will resolve to the top N entities with the highest values
     *         for the specified extractor
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R extends Comparable<? super R>> CompletableFuture<List<T>> topBy(
                ValueExtractor<? super T, ? extends R> extractor, int cResults)
        {
        InvocableMap.StreamingAggregator aggregator =
                Aggregators.topN(ValueExtractor.identity(), new ExtractorComparator<>(extractor), cResults);

        return getMapInternal()
                .aggregate(aggregator)
                .thenApply(rs -> Arrays.stream((Object[]) rs).collect(Collectors.toList()));
        }

    /**
     * Return the top N entities with the highest values for the specified extractor.
     *
     * @param filter     the entity selection criteria
     * @param extractor  the extractor to get the values to compare with
     * @param cResults   the number of highest values to return
     * @param <R>        the type of values used for comparison
     *
     * @return a {@link CompletableFuture} that will resolve to the top N entities with the highest values
     *        for the specified extractor
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R extends Comparable<? super R>> CompletableFuture<List<T>> topBy(Filter<?> filter,
                ValueExtractor<? super T, ? extends R> extractor, int cResults)
        {
        InvocableMap.StreamingAggregator aggregator =
                Aggregators.topN(ValueExtractor.identity(), new ExtractorComparator<>(extractor), cResults);

        return getMapInternal()
                .aggregate(filter, aggregator)
                .thenApply(rs -> Arrays.stream((Object[]) rs).collect(Collectors.toList()));
        }

    /**
     * Return the top N entities with the highest values for the specified extractor.
     *
     * @param comparator  the comparator to use when comparing extracted values
     * @param cResults    the number of highest values to return
     *
     * @return a {@link CompletableFuture} that will resolve to the top N entities with the highest value
     *        for the specified extractor
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public CompletableFuture<List<T>> topBy(Remote.Comparator<? super T> comparator, int cResults)
        {
        InvocableMap.StreamingAggregator aggregator = Aggregators.topN(ValueExtractor.identity(), comparator, cResults);

        return getMapInternal()
                .aggregate(aggregator)
                .thenApply(rs -> Arrays.stream((Object[]) rs).collect(Collectors.toList()));
        }

    /**
     * Return the top N entities with the highest values for the specified extractor.
     *
     * @param filter      the entity selection criteria
     * @param comparator  the comparator to use when comparing extracted values
     * @param cResults    the number of highest values to return
     *
     * @return a {@link CompletableFuture} that will resolve to the top N entities with the highest values
     *         for the specified extractor
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public CompletableFuture<List<T>> topBy(Filter<?> filter, Remote.Comparator<? super T> comparator, int cResults)
        {
        InvocableMap.StreamingAggregator aggregator = Aggregators.topN(ValueExtractor.identity(), comparator, cResults);

        return getMapInternal()
                .aggregate(filter, aggregator)
                .thenApply(rs -> Arrays.stream((Object[]) rs).collect(Collectors.toList()));
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Ensures that this repository is initialized when accessed for the first
     * time, and calls {@link #getMap()}.
     *
     * @return the {@link AsyncNamedMap} returned by the {@link #getMap()} method
     */
    private AsyncNamedMap<ID, T> getMapInternal()
        {
        ensureInitialized();
        return getMap();
        }
    }
