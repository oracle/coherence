/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;


import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * Enables traversal of elements in the underlying data set using either
 * {@link Iterator}, {@link Spliterator} or a {@link Stream}, while allowing
 * implementors to provide additional metadata, such as the size and other
 * characteristics of the underlying data set.
 * <p>
 * Similarly to the {@link Spliterator}, the {@code Streamer} allows only a
 * single traversal of the underlying data set. Moreover, a {@link #spliterator()
 * Spliterator} or a {@link #stream() Stream} retrieved from a partially
 * consumed {@code Streamer} will continue the traversal of the underlying data.
 * <p>
 * Note, that the {@link #remove removal} of the underlying elements is not supported.
 *
 * @author as  2015.05.06
 * @since 12.2.1
 */
public interface Streamer<T>
      extends Iterator<T>
    {
    /**
     * Return the exact size of the data set this {@code Streamer} will iterate over,
     * or negative integer if the exact size is unknown.
     *
     * @return the exact size of the data set this {@code Streamer} will iterate over,
     *         or negative integer if the exact size is unknown
     */
    public long size();

    /**
     * A bit mask representing the set of characteristics of this {@code Streamer}.
     *
     * @return a bit mask representing the set of characteristics of this {@code Streamer}
     */
    public int characteristics();

    /**
     * Return a {@link Spliterator} over the elements described by this {@code Streamer}.
     *
     * @return a {@link Spliterator} over the elements described by this
     *         {@code Streamer}
     */
    public default Spliterator<T> spliterator()
      {
      return Spliterators.spliterator(this, size(),
          isSized() ? Spliterator.SIZED : 0);
      }

    /**
     * Return a {@link Stream} based on this {@code Streamer}.
     *
     * @return a {@link Stream} based on this {@code Streamer}
     */
    public default Stream<T> stream()
      {
      return StreamSupport.stream(spliterator(), false);
      }

    /**
     * A convenience accessor to check if this streamer is {@link #SIZED}.
     *
     * @return <code>true</code> if this streamer is {@link #SIZED}, false otherwise
     */
    public default boolean isSized()
        {
        return (characteristics() & SIZED) != 0 && size() >= 0;
        }

    /**
     * A convenience accessor to check if this streamer is {@link #ALL_INCLUSIVE}.
     *
     * @return <code>true</code> if this streamer is {@link #ALL_INCLUSIVE}, false otherwise
     */
    public default boolean isAllInclusive()
        {
        return (characteristics() & ALL_INCLUSIVE) != 0;
        }

    /**
     * This operation is not supported.
     */
    @Override
    public default void remove()
      {
      throw new UnsupportedOperationException();
      }

    // ----- constants ------------------------------------------------------

    /**
     * Characteristic value signifying that the value returned from
     * {@code size()} prior to traversal represents a finite size that,
     * in the absence of structural source modification, represents an exact
     * count of the number of elements that would be encountered by a complete
     * traversal.
     */
    public static final int SIZED         = 0x00000001;

    /**
     * Characteristic value signifying that this Streamer includes all the
     * values from the underlying data set.
     */
    public static final int ALL_INCLUSIVE = 0x00000002;
    }
