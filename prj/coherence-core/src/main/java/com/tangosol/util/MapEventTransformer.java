/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;


import java.io.Serializable;
import java.util.Objects;


/**
 * MapEventTransformer interface is used to allow an event consumer to change
 * the content of a {@link MapEvent} destined for the corresponding
 * {@link MapListener}.
 * <p>
 * In general, the {@link #transform transform} method is called after the
 * original MapEvent is evaluated by a Filter (such as
 * {@link com.tangosol.util.filter.MapEventFilter}). The values contained by the
 * returned  MapEvent object will be the ones given (sent) to the corresponding
 * listener. Returning null will prevent the emission of the event altogether.
 * <p>
 * <b>Note:</b> Currently, the MapEventTransformer interface is supported only
 * by partitioned caches.
 *
 * @author gg/jh  2008.05.01
 * @since Coherence 3.4
 */
@FunctionalInterface
public interface MapEventTransformer<K, V, U>
        extends Serializable
    {
    /**
     * Transform the specified MapEvent. The values contained by the returned
     * MapEvent object will be the ones given (sent) to the corresponding
     * listener.
     *
     * @param event  the original MapEvent object
     *
     * @return modified MapEvent object or null to discard the event
     */
    public MapEvent<K, U> transform(MapEvent<K, V> event);

    // ----- helper methods -------------------------------------------------

     /**
      * Returns a composed {@code MapEventTransformer} that performs, in sequence,
      * this transformation followed by the {@code after} transformation.
      * <p>
      * If performing either transformation throws an exception, it is relayed
      * to the caller of the composed operation.  If performing this
      * transformation throws an exception, the {@code after} transformation
      * will not be performed.
      *
      * @param <T>    the resulting transformed type T of the after transformer
      * @param after  the transformation to perform after this transformation
      *
      * @return a composed {@code MapEventTransformer} that performs in sequence
      *         this transformation followed by the {@code after} transformation
      */
    public default <T> MapEventTransformer<K, V, T> andThen(MapEventTransformer<K, U, T> after)
        {
        Objects.requireNonNull(after);
        return (evt) -> after.transform(this.transform(evt));
        }
    }
