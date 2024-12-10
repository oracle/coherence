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

import com.tangosol.util.ClassHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;

import com.tangosol.util.extractor.DeserializationAccelerator;

import com.tangosol.util.filter.MapEventFilter;

import com.tangosol.util.function.Remote;

import java.util.Comparator;
import java.util.Objects;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Base functionality for all Coherence
 * <a href="https://martinfowler.com/eaaCatalog/repository.html">repository</a>
 * implementations.
 *
 * @param <ID>  the type of entity's identifier
 * @param <T>   the type of entity stored in this repository
 * @param <M>   the underlying map type
 *
 * @author Ryan Lubke    2021.04.08
 * @author Aleks Seovic
 * @since 21.06
 */
public abstract class AbstractRepositoryBase<ID, T, M>
    {
    // tag::abstract[]
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

    /**
     * Return the map that is used as the underlying entity store.
     *
     * @return the map that is used as the underlying entity store
     */
    protected abstract M getMap();
    // end::abstract[]

    /**
     * Return the underlying {@link NamedMap}.  This is used internally
     * to support listeners and indices.
     *
     * @return the underlying {@link NamedMap}
     */
    abstract NamedMap<ID, T> getNamedMap();

    // ----- helpers --------------------------------------------------------

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
        NamedMap<ID, T>    namedMap   = getNamedMap();
        Class<? extends T> entityType = getEntityType();

        //noinspection CheckStyle
        if (getClass().isAnnotationPresent(Accelerated.class) ||
            entityType.isAnnotationPresent(Accelerated.class))
            {
            Logger.info("Configuring deserialization accelerator for " + getClass().getName());
            namedMap.addIndex(new DeserializationAccelerator());
            }

        Stream.of(entityType.getMethods())
                .filter(m -> m.isAnnotationPresent(Indexed.class))
                .forEach(m ->
                         {
                         try
                             {
                             Indexed idx = m.getAnnotation(Indexed.class);
                             boolean fOrdered = idx.ordered();
                             Comparator<?> comparator =
                                     Comparator.class.equals(idx.comparator())
                                     ? null
                                     : (Comparator<?>) ClassHelper.newInstance(idx.comparator(), null);

                             String sIndexMsg = "Creating index %s::%s (ordered=%b, comparator=%s)";
                             Logger.info(() -> String.format(sIndexMsg, entityType.getSimpleName(),
                                                             m.getName(), fOrdered, comparator));

                             namedMap.addIndex(ValueExtractor.forMethod(m), fOrdered, comparator);
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
     * @param updater the updater function to use
     * @param value   the value to update each entity with, which will be passed
     *                as an argument to the updater function
     * @param factory the entity factory to use to create new entity instance
     * @param <ID>    the type of entity's identifier
     * @param <T>     the type of entity stored in this repository
     * @param <U>     the type of value to update
     *
     * @return an entry processor that should be used to perform the update
     */
    static <ID, T, U> InvocableMap.EntryProcessor<ID, T, Void> updaterProcessor(
            ValueUpdater<? super T, ? super U> updater,
            U value,
            EntityFactory<? super ID, ? extends T> factory)
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
     * @param updater the updater function to use
     * @param factory the entity factory to use to create new entity instance
     * @param <ID>    the type of entity's identifier
     * @param <T>     the type of entity stored in this repository
     * @param <R>     the type of return value of the updater function
     *
     * @return an entry processor that should be used to perform the update
     */
    static <ID, T, R> InvocableMap.EntryProcessor<ID, T, R> updateFunctionProcessor(
            Remote.Function<? super T, ? extends R> updater,
            EntityFactory<? super ID, ? extends T> factory)
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
     * @param updater the updater function to use
     * @param value   the value to update each entity with, which will be passed
     *                as an argument to the updater function
     * @param factory the entity factory to use to create new entity instance
     * @param <ID>    the type of entity's identifier
     * @param <T>     the type of entity stored in this repository
     * @param <U>     the type of value to update
     * @param <R>     the type of return value of the updater function
     *
     * @return an entry processor that should be used to perform the update
     */
    static <ID, T, U, R> InvocableMap.EntryProcessor<ID, T, R> updateBiFunctionProcessor(
            Remote.BiFunction<? super T, ? super U, ? extends R> updater,
            U value,
            EntityFactory<? super ID, ? extends T> factory)
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

    // ----- listener support -----------------------------------------------

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
        getNamedMap().addMapListener(instantiateMapListener(listener));
        }

    /**
     * Unregister a listener that observes all repository events.
     *
     * @param listener  the event listener to unregister
     */
    public void removeListener(Listener<? super T> listener)
        {
        getNamedMap().removeMapListener(instantiateMapListener(listener));
        }

    /**
     * Register a listener that will observe all events for a specific entity.
     *
     * @param id        the identifier of the entity to observe
     * @param listener  the event listener to register
     */
    public void addListener(ID id, Listener<? super T> listener)
        {
        getNamedMap().addMapListener(instantiateMapListener(listener), id, false);
        }

    /**
     * Unregister a listener that observes all events for a specific entity.
     *
     * @param id        the identifier of the entity to observe
     * @param listener  the event listener to unregister
     */
    public void removeListener(ID id, Listener<? super T> listener)
        {
        getNamedMap().removeMapListener(instantiateMapListener(listener), id);
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

        getNamedMap().addMapListener(instantiateMapListener(listener), filter, false);
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

        getNamedMap().removeMapListener(instantiateMapListener(listener), filter);
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
