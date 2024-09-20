/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

import java.util.Collection;

/**
 * ObservableCollection interface represents an object with a model being 
 * a Collection that allows for pluggable notifications for occurring changes.
 *
 * @param <V>  the type of the Collection values
 */
public interface ObservableCollection<V>
        extends Collection<V>
    {
    /**
     * Add a standard collection listener that will receive all events (inserts,
     * updates, deletes) that occur against the collection, with the old-value
     * and new-value included. This has the same result as the following call:
     * <pre>
     *   addListener(listener, (Filter) null, false);
     * </pre>
     *
     * @param listener the {@link CollectionEvent} listener to add
     */
    void addListener(CollectionListener<? super V> listener);

    /**
     * Remove a standard collection listener that previously signed up for all
     * events. This has the same result as the following call:
     * <pre>
     *   removeListener(listener, (Filter) null);
     * </pre>
     *
     * @param listener the listener to remove
     */
    void removeListener(CollectionListener<? super V> listener);

    /**
     * Add a collection listener that receives events based on a filter evaluation.
     * <p>
     * The listeners will receive CollectionEvent objects, but if fLite is passed as
     * true, they <i>might</i> not contain the OldValue and NewValue
     * properties.
     * <p>
     * To unregister the Listener, use the
     * {@link #removeListener(CollectionListener, Filter)} method.
     *
     * @param listener  the {@link CollectionEvent} listener to add
     * @param filter    a filter that will be passed CollectionEvent objects to select
     *                  from; a CollectionEvent will be delivered to the listener only
     *                  if the filter evaluates to true for that CollectionEvent (see
     *                  {@link com.tangosol.util.filter.MapEventFilter});
     *                  null is equivalent to a filter that always returns true
     * @param fLite     true to indicate that the {@link CollectionEvent} objects do
     *                  not have to include the OldValue and NewValue
     *                  property values in order to allow optimizations
     */
    void addListener(CollectionListener<? super V> listener, Filter<V> filter, boolean fLite);

    /**
     * Remove a collection listener that previously signed up for events based on a
     * filter evaluation.
     *
     * @param listener  the listener to remove
     * @param filter    the filter that was passed into the corresponding
     *                   addCollectionListener() call
     */
    void removeListener(CollectionListener<? super V> listener, Filter<V> filter);
    }