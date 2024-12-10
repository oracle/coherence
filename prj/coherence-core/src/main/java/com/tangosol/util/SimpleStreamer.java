/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;


import java.util.Collection;
import java.util.Iterator;

import java.util.stream.Stream;


/**
 * Simple implementation of a {@link Streamer}.
 *
 * @author as  2015.05.07
 * @since 12.2.1
 */
public class SimpleStreamer<T>
        implements Streamer<T>
    {
    /**
     * Construct Streamer based on {@link Iterable}.
     *
     * @param iterable  an {@code Iterable} to create {@code Streamer} from
     */
    public SimpleStreamer(Iterable<T> iterable)
        {
        this(iterable.iterator());
        }

    /**
     * Construct Streamer based on {@link Iterator}.
     *
     * @param iterator  an {@code Iterator} to create {@code Streamer} from
     */
    public SimpleStreamer(Iterator<T> iterator)
        {
        this(iterator, -1, 0);
        }

    /**
     * Construct Streamer based on {@link Stream}.
     *
     * @param stream  a {@code Stream} to create {@code Streamer} from
     */
    public SimpleStreamer(Stream<T> stream)
        {
        this(stream.iterator(), -1, 0);
        }

    /**
     * Construct Streamer based on {@link Collection}.
     *
     * @param coll  a {@code Collection} to create {@code Streamer} from
     */
    public SimpleStreamer(Collection<T> coll)
        {
        this(coll.iterator(), coll.size(), SIZED);
        }

    /**
     * Construct Streamer instance.
     *
     * @param iterator          an {@code Iterator} to create {@code Streamer} from
     * @param cSize             the number of elements this {@code Streamer} will
     *                          iterate over
     * @param nCharacteristics  the bit mask representing this {@code Streamer}'s
     *                          characteristics
     */
    protected SimpleStreamer(Iterator<T> iterator, long cSize, int nCharacteristics)
        {
        m_iterator         = iterator;
        m_cSize            = cSize;
        m_nCharacteristics = nCharacteristics;
        }

    // ---- Streamer interface ----------------------------------------------

    @Override
    public boolean hasNext()
      {
      return m_iterator.hasNext();
      }

    @Override
    public T next()
      {
      return m_iterator.next();
      }

    @Override
    public long size()
        {
        return m_cSize;
        }

    @Override
    public int characteristics()
        {
        return m_nCharacteristics;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The underlying iterator.
     */
    private Iterator<T> m_iterator;

    /**
     * The number of elements this {@code Streamer} will iterate over.
     */
    private long m_cSize;

    /**
     * The bit mask representing this {@code Streamer}'s characteristics
     */
    private int m_nCharacteristics;
    }
