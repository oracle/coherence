/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.tangosol.net.BackingMapContext;

import com.tangosol.util.Converter;
import com.tangosol.util.ConverterCollections;
import com.tangosol.util.Filter;
import com.tangosol.util.FilterEnumerator;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * ConversionHelper provides access to common {@link Converter}s used internally
 * by PartitionedCache.
 *
 * @author hr  2014.02.24
 * @since Coherence 12.1.3
 */
public abstract class ConversionHelper
    {
    // ----- static helpers -------------------------------------------------

    /**
     * Returns a {@link Converter} that uses the provided Converters to view a
     * Collection. The value passed to the returned Converter must be a
     * Collection.
     * <p>
     * For example to convert a {@code Map<String, Collection<Integer>>} to
     * a {@code Map<String, Collection<String>>} this method could be used as
     * follows:
     * <pre><code>
     *     Map<String, Collection<String>> map = ConverterCollections.getMap(mapRaw,
     *             convKeyUp, convKeyDown,
     *             getCollectionConverter((Integer n) -> String.valueOf(n),
     *                                    (String s)  -> Integer.parseInt(s)),
     *             convValueDown);
     * </code></pre>
     *
     * @param convUp    the Converter to view the underlying Collection
     *                  through
     * @param convDown  the Converter to pass items down to the underlying
     *                  Collection through
     *
     * @return a Converter that uses the provided Converts to view a Collection
     */
    public static Converter getCollectionConverter(final Converter convUp, final Converter convDown)
        {
        return new Converter()
            {
            @Override
            public Object convert(Object value)
                {
                return ConverterCollections.getCollection((Collection) value,
                        convUp, convDown);
                }
            };
        }

    /**
     * Return a view of the provided {@link Collection} exposing only those elements
     * that {@link Filter#evaluate(Object)} evaluate} to true by the provided
     * {@link Filter}.
     *
     * @param col     the source collection
     * @param filter  the Filter to apply to each element in the source Collection
     *
     * @return a view of the source Collection exposing only those elements
     *         permitted by the {@link Filter}
     */
    public static Collection getFilterCollection(Collection col, Filter filter)
        {
        return new FilterCollection(col, filter);
        }

    /**
     * Return a {@link Converter} that converts a Map to a Collection representing
     * the values of the Map, a.k.a. {@link Map#values()}.
     *
     * @return a Converter that converts a Map to a Collection representing
     *         the values of the Map
     */
    public static Converter getMapValuesConverter()
        {
        return CONV_MAPVALUES;
        }

    /**
     * Return a {@link Converter} that converts a {@link BackingMapContext} to
     * a string cache name.
     *
     * @return a Converter that converts a BackingMapContext to a string cache
     *         name
     */
    public static Converter<BackingMapContext, String> getCacheNameConverter()
        {
        return CONV_CACHE_NAME;
        }

    /**
     * Return a {@link Converter} that converts a Map to a Collection representing
     * the values of the Map, a.k.a. {@link Map#values()}.
     * <p>
     * The Collection returned uses the provided converters to convert each
     * element in the collection on access or update.
     *
     * @param convUp    the Converter to view the underlying Collection
     *                  through
     * @param convDown  the Converter to pass items down to the underlying
     *                  Collection through
     *
     * @return a Converter that converts a Map to a Collection representing
     *         the values of the Map
     */
    public static Converter getMapValuesConverter(Converter convUp, Converter convDown)
        {
        return new MapValuesConverter(convUp, convDown);
        }

    // ----- inner class: MapValuesConverter --------------------------------

    /**
     * A {@link Converter} that converts a Map to a Collection representing
     * the values of the Map, a.k.a. {@link Map#values()}.
     * <p>
     * The Collection returned uses the provided converters to convert each
     * element in the collection on access or update.
     */
    protected static class MapValuesConverter
            implements Converter
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a MapValuesConverter that converts a Map to a Collection
         * of values.
         */
        public MapValuesConverter()
            {
            this(null, null);
            }

        /**
         * Construct a MapValuesConverter that converts a Map to a Collection
         * of values using the {@link Converter}s to convert each element.
         *
         * @param convUp    converts each element on access
         * @param convDown  converts each element on update
         */
        public MapValuesConverter(Converter convUp, Converter convDown)
            {
            f_convUp   = convUp;
            f_convDown = convDown;
            }

        // ----- Converter interface ----------------------------------------

        @Override
        public Object convert(Object value)
            {
            Collection colVals = ((Map) value).values();
            if (f_convUp != null || f_convDown != null)
                {
                colVals = ConverterCollections.getCollection(colVals, f_convUp, f_convDown);
                }
            return colVals;
            }

        // ----- data members -----------------------------------------------

        /**
         * Converts the element on access from its raw form to its exposed form.
         */
        protected final Converter f_convUp;

        /**
         * Converts the element on update from its exposed form to its raw form.
         */
        protected final Converter f_convDown;
        }

    // ----- inner class: FilterCollection ----------------------------------

    /**
     * A FilterCollection is an unmodifiable {@link Collection} that given a
     * 'source' Collection either returns the entire source Collection or a
     * subset. The elements contained within the source Collection and exposed
     * by the FilterCollection is determined by the provided {@link Filter}.
     *
     * @param <E> the type of elements held by this Collection
     */
    protected static class FilterCollection<E>
            extends AbstractCollection<E>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a FilterCollection with the provided Collection and
         * {@link Filter}.
         *
         * @param col     the source Collection
         * @param filter  the Filter to apply to each element in the source
         *                Collection
         */
        protected FilterCollection(Collection<E> col, Filter filter)
            {
            f_col    = col;
            f_filter = filter;
            }

        // ----- AbstractCollection methods ---------------------------------

        @Override
        public Iterator<E> iterator()
            {
            return new FilterEnumerator(f_col.iterator(), f_filter);
            }

        @Override
        public int size()
            {
            if (m_cSize < 0)
                {
                int cSize = 0;
                for (Iterator<E> iter = iterator(); iter.hasNext(); iter.next())
                    {
                    ++cSize;
                    }
                m_cSize = cSize;
                }
            return m_cSize;
            }

        @Override
        public boolean isEmpty()
            {
            for (Iterator iter = iterator(); iter.hasNext(); )
                {
                return false;
                }
            return true;
            }

        // ----- data members -----------------------------------------------

        /**
         * The source Collection.
         */
        protected final Collection f_col;

        /**
         * The Filter to apply to each element in the source Collection.
         */
        protected final Filter f_filter;

        /**
         * A cached size of the Filter applied to the source Collection.
         */
        protected int m_cSize = -1;
        }

    // ----- constants ------------------------------------------------------

    /**
     * A MapValuesConverter with no converters.
     */
    protected static final Converter CONV_MAPVALUES  = new MapValuesConverter();

    /**
     * A Converter that converts a BackingMapContext to a cache name.
     */
    public static final Converter<BackingMapContext, String> CONV_CACHE_NAME = BackingMapContext::getCacheName;
    }
